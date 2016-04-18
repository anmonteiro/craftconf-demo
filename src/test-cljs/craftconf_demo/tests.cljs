(ns craftconf-demo.tests
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [craftconf-demo.core :as demo]))

(def sample-data
  {:speaker/list [{:db/id 17592186045420
                   :speaker/name "Bob Smith"
                   :speaker/age 35
                   :speaker/talk {:db/id 17592186045418
                                  :talk/title "The joys of Foo"
                                  :talk/duration 45
                                  :talk/favorites 0}}
                  {:db/id 17592186045421
                   :speaker/name "Mary Brown"
                   :speaker/age 30
                   :speaker/talk {:db/id 17592186045419
                                  :talk/title "Building X in 30 minutes"
                                  :talk/duration 30
                                  :talk/favorites 1}}]
   :favorites/list [{:db/id 17592186045421
                     :speaker/name "Mary Brown"
                     :speaker/age 30
                     :speaker/talk {:db/id 17592186045419
                                    :talk/title "Building X in 30 minutes"
                                    :talk/duration 30
                                    :talk/favorites 1}}]})

(def gen-tx-fav-talk
  (gen/tuple
    (gen/vector
      (gen/fmap seq
        (gen/tuple
          (gen/elements '[favorite/inc!]))))
    (gen/fmap (fn [id] [:speaker/by-id id])
      (gen/elements [17592186045420 17592186045421]))))

(defn in-favs-list? [favs [_ id]]
  (boolean (some #{id} (map :db/id favs))))

(defn prop-adds-to-favorites []
  (prop/for-all* [gen-tx-fav-talk]
    (fn [[tx ref]]
      (let [parser (om/parser {:read demo/read :mutate demo/mutate})
            state  (atom (om/tree->db demo/RootView sample-data true))]
                                        ;(println "hi")
        (parser {:state state
                 :ref   ref} tx)
        (let [ui (parser {:state state} (om/get-query demo/RootView))]
          (if-not (empty? tx)
            (in-favs-list? (:favorites/list ui) ref)
            true))))))
