(ns craftconf-demo.parser
  (:require [datomic.api :as d]
            [om.next.server :as om]))

(defmulti readf om/dispatch)

(defmethod readf :remote/data
  [{:keys [conn query]} k params]
  {:value (d/q '[:find [(pull ?eid query) ...]
                 :in $ query
                 :where
                 [?eid :speaker/name]]
            (d/db conn)
            query)})

(defmulti mutatef om/dispatch)
