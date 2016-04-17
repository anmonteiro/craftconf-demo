(ns craftconf-demo.parser
  (:require [datomic.api :as d]
            [om.next.server :as om]))

(defmulti readf om/dispatch)

(defn fetch-speakers [conn query]
  (d/q '[:find [(pull ?eid query) ...]
         :in $ query
         :where
         [?eid :speaker/name]]
    (d/db conn)
    query))

(defn fetch-favorites [conn query]
  (d/q '[:find [(pull ?spkr query) ...]
         :in $ query
         :where
         [?spkr :speaker/talk ?talk]
         [?talk :talk/favorites ?fav]
         [(pos? ?fav)]]
    (d/db conn)
    query))

(defmethod readf :remote/data
  [{:keys [conn query]} k params]
  (let [result-set (when-not (empty? query)
                     (fetch-speakers conn query))]
    {:value result-set}))

(defmethod readf :speaker/list
  [{:keys [conn query]} k params]
  (let [result-set (when-not (empty? query)
                     (fetch-speakers conn query))]
    {:value result-set}))

(defmethod readf :favorites/list
  [{:keys [conn query]} k params]
  (let [result-set (when-not (empty? query)
                     (fetch-favorites conn query))]
    {:value result-set}))

(defmulti mutatef om/dispatch)
