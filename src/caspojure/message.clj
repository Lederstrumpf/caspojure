(ns caspojure.message
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(s/def ::vote #{0 1})
(s/def ::estimate #{0 1})
(s/def ::genesis-message (s/tuple ::vote ::estimate nil?))
(s/def ::message (s/or :message (s/tuple ::vote ::estimate (s/every ::message :min-count 1 :gen-max 4))
                       :genesis ::genesis-message))

(s/valid? ::message [1 0
                     [[1 0
                       [[0 0 nil]
                        [1 0 [[0 0 nil]]]]]
                      [0 0 nil]]
                     ])

(s/valid? ::message [1 0 nil])

(gen/generate (s/gen ::message))
