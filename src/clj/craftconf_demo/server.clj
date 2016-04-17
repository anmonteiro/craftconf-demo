(ns craftconf-demo.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [redirect resource-response]]
            [craftconf-demo.middleware :refer [wrap-transit-response wrap-transit-params]]
            [bidi.bidi :as bidi]
            [om.next.server :as om]
            [craftconf-demo.parser :as parser]))

(def app-routes
  ["" {"/" :index
       "/api"
       {:get  {[""] :api}
        :post {[""] :api}}}])

(def server-parser
  (om/parser {:read parser/readf
              :mutate parser/mutatef}))

(defn index [req]
  (assoc (resource-response (str "index.html") {:root "public"})
    :headers {"Content-Type" "text/html"}))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

;; TODO: move this to utils
(defn- pull-tempids [data]
  (let [rewritten-mutations
        (->> (filter (comp symbol? first) data)
          (map (fn [[k v]]
                 (let [tempids (-> v :result :tempids)
                       v' (-> v
                            (update-in [:result] dissoc :tempids)
                            (assoc :tempids tempids))]
                   [k v'])))
          (reduce merge {}))]
    (merge data rewritten-mutations)))

(defn api [req]
  (let [data (server-parser {:conn (:datomic-connection req)}
               (:transit-params req))
        data (pull-tempids data)]
    (generate-response data)))

(defn handler [req]
  (let [match (bidi/match-route app-routes (:uri req)
                :request-method (:request-method req))]
    (case (:handler match)
      :index (index req)
      :api   (api req)
      req)))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :datomic-connection conn))))

(defn make-handler [conn]
  (-> handler
    (wrap-connection conn)
    wrap-transit-params
    wrap-transit-response
    (wrap-resource "public")))

(defrecord WebServer [port handler container datomic-connection]
  component/Lifecycle
  (start [component]
    (let [conn (:connection datomic-connection)]
      (if container
        (let [req-handler (handler conn)
              container (run-jetty req-handler
                          {:port port :join? false})]
          (assoc component :container container))
        ;; if no container
        (assoc component :handler (handler conn)))))
  (stop [component]
    (.stop container)))

(defn dev-server [web-port]
  (WebServer. web-port make-handler true nil))
