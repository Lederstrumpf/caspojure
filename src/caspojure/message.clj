(ns caspojure.message
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(def relayer-count 10)
(s/def ::relayer-id (s/int-in 0 relayer-count))
(s/def ::vote boolean?)
(s/def ::vote-message (s/tuple ::relayer-id ::vote))
(s/def ::estimate boolean?)
(s/def ::genesis-message (s/tuple ::relayer-id ::estimate nil?))
(s/def ::relay-message (s/or :message (s/tuple ::relayer-id ::estimate (s/every (s/or :relay ::relay-message :vote ::vote-message) :min-count 1 :gen-max 4))
                       :genesis ::genesis-message))

(gen/generate (s/gen ::vote-message))
(gen/generate (s/gen ::relay-message))

(s/valid? ::vote-message [2 false])

(s/explain ::relay-message [1 false
                     [[9 true
                       [[2 false nil]
                        [1 true [[0 true nil]]]]]
                      [0 true nil]]
                     ])

(gen/generate (s/gen ::vote-message))
(gen/generate (s/gen ::relay-message))
