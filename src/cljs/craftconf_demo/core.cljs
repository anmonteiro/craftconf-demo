(ns craftconf-demo.core
  (:require [craftconf-demo.utils :as utils]
            [devcards.core :as dc :include-macros true]
            [devcards.system :as dev]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs.reader :as r]
            [devcards.util.edn-renderer :refer [html-edn]]
            [goog.dom :as gdom]
            [goog.object :as gobj]
            [cljs.pprint :as pp :refer [pprint]]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addons.matchbrackets]
            [cljsjs.codemirror.addons.closebrackets]))

(enable-console-print!)

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :remote/data
  [{:keys [query state target]} k params]
  ;; For demo purposes, always throw away what we have in the app state before
  ;; making the remote call
  (when-not (nil? target)
    (swap! state dissoc k))
  {:value (get @state k)
   :remote true})

(defmethod read :speaker/list
  [{:keys [query state target]} k params]
  (let [st @state]
    (if (contains? st k)
      {:value (om/db->tree query (get st k) st)}
      {:remote true})))

(defmethod read :favorites/list
  [{:keys [query state target]} k params]
  (let [st @state]
    (if (contains? st k)
      {:value (om/db->tree query (get st k) st)}
      {:remote true})))

(defn favorite-talk [state speaker-id]
  (let [talk-ident (get-in state (conj speaker-id :speaker/talk))]
    (-> state
      (update-in (conj talk-ident :talk/favorites) inc)
      #_(update-in [:favorites/list]
        #(cond-> %
           (not (some #{speaker-id} %)) (conj speaker-id))))))

(defmethod mutate 'favorite/inc!
  [{:keys [state ref] :as env} _ _]
  ;; OPTIMISTIC UPDATE
  {:action
   (fn []
     (swap! state favorite-talk ref))})

(defn str->query [query-str]
  (try
    (r/read-string query-str)
    (catch js/Error e "Invalid Query")))

(def cm-opts
  #js {:lineNumbers       true
       :matchBrackets     true
       :autoCloseBrackets true
       :indentWithTabs    false
       :mode              #js {:name "clojure"}})

(defn pprint-src
  "Pretty print src for CodeMirror editor.
  Could be included in textarea->cm"
  [s]
  (-> s
      pprint
      with-out-str))

(defn make-card [card-body]
  (if (satisfies? dc/IDevcardOptions card-body)
    card-body
    (reify dc/IDevcardOptions
      (-devcard-options [this opts]
        (assoc opts :main-obj card-body)))))

(defn devcard [name doc main-obj & [opts]]
  (let [card (cond-> {:name name
                      :documentation doc
                      :main-obj (make-card main-obj)}
               (not (nil? opts)) (merge {:options opts}))]
    (dc/card-base card)))

(defn textarea->cm
  "Decorate a textarea with a CodeMirror editor given an id and code as string."
  [id code]
  (let [ta (gdom/getElement id)]
    (js/CodeMirror
      #(.replaceChild (.-parentNode ta) % ta)
      (doto cm-opts
        (gobj/set "value" (str code "\n"))))))

(defn handle-run-query-click! [c]
  (let [cm (om/get-state c :cm)
        query (.getValue cm)]
    (om/set-query! c {:params {:user-query (str->query query)}})))

(defui QueryEditor
  static om/IQueryParams
  (params [this]
    {:user-query [:speaker/name :speaker/age :speaker/talk]})
  static om/IQuery
  (query [this]
    '[{:remote/data ?user-query}])
  Object
  (componentDidMount [this]
    (let [query (:user-query (om/get-params this))
          src (pprint-src query)
          cm (textarea->cm "query-editor" src)]
      (om/update-state! this assoc :cm cm)))
  (render [this]
    (let [props (om/props this)
          local (om/get-state this)]
      (dom/div nil ;#js {:style #js {:display "table"}}
        (dom/div #js {:style #js {:display "inline-block"
                                  :verticalAlign "top"
                                  :width "50%"
                                  ;:paddingRight "20px"
                                  }}
          (dom/p nil)
          (dom/div #js {:id "query-editor"})
          (dom/button #js {:style #js {:fontSize "20px"
                                       :marginLeft "20px"}
                           :onClick (partial handle-run-query-click! this)} "Run Query"))
        (dom/div #js {:style #js {:display "inline-block"
                                  :verticalAlign "top"
                                  :width "50%"
                                  ;:borderLeft "black 2px solid"
                                  ;:paddingLeft "20px"
                                  }}
          (devcard "Run query result" "" (:remote/data props) {:heading false}))))))

(def reconciler
  (om/reconciler {:state {}             ;init-state
                  :parser (om/parser {:read read
                                      :mutate mutate})
                  :send (utils/transit-post "/api")}))

;; =============================================================================

(defui Talk
  static om/Ident
  (ident [this {:keys [db/id]}]
    [:talk/by-id id])
  static om/IQuery
  (query [this]
    [:db/id :talk/title :talk/duration :talk/favorites])
  Object
  (render [this]
    (let [{:keys [talk/title talk/duration talk/favorites]} (om/props this)
          fav-inc (om/get-computed this :handle-fav-click)]
      (dom/div nil
        (dom/p nil title)
        (dom/p nil (str "Duration: " duration " minutes."))
        (dom/p nil (str "Favorites: " favorites))
        (dom/button #js {:onClick fav-inc} "Favorite!")))))

(def talk-view (om/factory Talk))

(defui Speaker
  static om/Ident
  (ident [this {:keys [db/id]}]
    [:speaker/by-id id])
  static om/IQuery
  (query [this]
    [:db/id :speaker/name :speaker/age {:speaker/talk (om/get-query Talk)}])
  Object
  (render [this]
    (let [{:keys [speaker/name speaker/age speaker/talk]} (om/props this)]
      (dom/li nil
        (dom/p nil (str name ", " age " years old."))
        (talk-view (om/computed talk {:handle-fav-click #(om/transact! this '[(favorite/inc!) :favorites/list])}))))))

(def speaker (om/factory Speaker {:keyfn :db/id}))

(defui ListView
  Object
  (render [this]
    (dom/div nil
      (dom/ul nil
        (map speaker (om/props this))))))

(def list-view (om/factory ListView))

(defui RootView
  static om/IQuery
  (query [this]
    [{:speaker/list (om/get-query Speaker)}
     {:favorites/list (om/get-query Speaker)}])
  Object
  (render [this]
    (let [{speakers :speaker/list favorites :favorites/list} (om/props this)]
      (dom/div #js {:style #js {:display "table"
                                :marginLeft "50px"
                                :fontFamily "Helvetica"}}
        (dom/div #js {:style #js {:width "300px"
                                  :display "table-cell"
                                  :borderRight "black 1px solid"}}
          (dom/h2 nil "Speakers")
          (list-view speakers))
        (dom/div #js {:style #js {:width "350px"
                                  :display "table-cell"
                                  :paddingLeft "30px"}}
          (dom/h2 nil "Favorites")
          (list-view favorites))))))

(defn setup-app! []
  (let [target (gdom/getElement "app")
        location (.. js/window -location -pathname)]
    (case location
      ("/" "/index.html")
      (do (dev/add-css-if-necessary!)
          (om/add-root! reconciler QueryEditor target))

      "/app.html"
      (om/add-root! reconciler RootView target))))

(setup-app!)
