(ns caspojure.message
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(def relayer-count 10)
(s/def ::relayer-id (s/int-in 0 relayer-count))

(s/def ::vote boolean?)
(s/def ::vote-message (s/keys :req [::relayer-id ::vote]))

(s/def ::estimate (s/or :consensus boolean? :nil nil?))

(s/def ::empty-vector (s/and vector? empty?))
(s/def ::justification (s/every (s/or :relay ::relay-message :vote ::vote-message) :min-count 0 :gen-max 3))
(s/def ::relay-message (s/keys :req [::relayer-id ::estimate ::justification]))
(s/def ::genesis-message (s/spec (s/and
                                  (s/nonconforming ::relay-message)
                                  #(and (nil? (::estimate %)) (empty? (::justification %)))
                                  )

                                 :gen (fn [] (s/gen ::relay-message
                                                   {::estimate #(gen/return nil)
                                                    ::justification #(gen/return [])}))
                                 ))

(defn votes-in-justification
  [relay-or-vote]
   (cond
     (s/valid? ::vote-message relay-or-vote) [relay-or-vote]
     (s/valid? ::justification (::justification relay-or-vote)) (apply concat (map votes-in-justification (::justification relay-or-vote)))
     ))

(defn equivocations [relay-message]
  (->> relay-message
       votes-in-justification
       (group-by ::relayer-id)
       (reduce-kv (fn [m k v] (assoc m k (into #{} v))) {})
       (filter #(< 1 (count (val %))))))

(s/def ::equivocation-void-message (s/and (s/nonconforming ::relay-message) #(-> % equivocations empty?)))
(s/def ::equivocation-full-message (s/and (s/nonconforming ::relay-message) #(->> % (s/valid? ::equivocation-void-message) not)))

(defn estimate [relay-message]
  (let [votes (votes-in-justification relay-message)
        counted-votes (->> votes
                           (into #{})
                           (group-by ::vote)
                           (reduce-kv (fn [m k v] (assoc m k (count v))) {}))]
    (case (count counted-votes)
      ;; no votes
      0 nil
      ;; unilateral consensus
      1 (-> counted-votes first key)
      ;; majority
      2 (case (apply compare (map val counted-votes))
          -1 (-> counted-votes second key)
          0 nil
          1 (-> counted-votes first key)
          ))))

(s/def ::justified-message (s/and (s/nonconforming ::relay-message) #(= (::estimate %) (estimate %))))

(s/def ::valid-relay-message (s/and (s/nonconforming ::justified-message) ::equivocation-void-message))

(s/def ::valid-message (s/or
                        :vote ::vote-message
                        :relay ::valid-relay-message
                        ))
