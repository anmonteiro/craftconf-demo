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

(defmethod read :remote/data
  [{:keys [query state target]} k params]
  (println "get st k" )
  {:value (get @state k)
   :remote true})

(defmulti mutate om/dispatch)

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
    (println "hi" query)
    (om/set-query! c {:params {:user-query (str->query query)}})))

(defui QueryEditor
  static om/IQueryParams
  (params [this]
    {:user-query [:speaker/name :speaker/age
                  {:speaker/talk [:talk/title :talk/duration]}]})
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
      (println "props" props)
      (dom/div #js {:width "600px"}
        (dom/p nil)
        (dom/div #js {:id "query-editor"})
        (dom/button #js {:style #js {:fontSize "20px"
                                     :marginLeft "20px"}
                         :onClick (partial handle-run-query-click! this)} "Run Query")
        (dom/hr nil)
        (devcard "Run query result" "" (:remote/data props) {:heading false})))))

(def init-state
  {:query-result {}})

(def reconciler
  (om/reconciler {:state init-state
                  :parser (om/parser {:read read
                                      :mutate mutate})
                  :send (utils/transit-post "/api")}))

(dev/add-css-if-necessary!)
(om/add-root! reconciler QueryEditor (gdom/getElement "app"))

