(ns caspojure.validator
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.alpha :as s]
            [manifold.stream :as stream]
            [caspojure.message :as message]))

(s/def ::validator (s/keys :req [::message/relayer-id ::view]))
(s/def ::view (s/coll-of ::message/relay-message :gen-max 5))

(def validators (reduce (fn [m [k v]] (assoc m k v)) {} (map
                                                        (fn [[id validator] stream] (vector id {::validator validator, ::stream stream}))
                                                        (map #(vector
                                                               %
                                                               (atom (gen/generate (s/gen ::validator
                                                                                          {::message/relayer-id (fn [] (gen/return %))}))))
                                                             (range message/relayer-count))
                                                        (repeatedly #(stream/stream)))))
