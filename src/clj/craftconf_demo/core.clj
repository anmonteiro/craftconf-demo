(ns craftconf-demo.core
  (:require [com.stuartsierra.component :as component]
            [craftconf-demo.datomic :as datomic]
            [craftconf-demo.server :as server]))

(defn dev-system [config-options]
  (let [{:keys [db-uri web-port]} config-options]
    (component/system-map
      :db (datomic/new-database db-uri)
      :webserver
      (component/using
        (server/dev-server web-port)
        {:datomic-connection :db}))))

(def servlet-system (atom nil))

(def dev-config
  {:db-uri   "datomic:mem://localhost:4334/craftconf-demo"
   :web-port 8081})

(defn dev-start []
  (let [sys  (dev-system dev-config)
        sys' (component/start sys)]
    (reset! servlet-system sys')
    sys'))

(comment
  (require '[craftconf-demo.core :as cc])
  (cc/dev-start)
  (:connection (:db @cc/servlet-system))


  )
