(ns caspojure.message
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(def relayer-count 10)
(s/def ::relayer-id (s/int-in 0 relayer-count))
(s/def ::vote boolean?)
(s/def ::vote-message (s/tuple ::relayer-id ::vote))
(s/def ::estimate (s/or :consensus boolean? :nil nil?))
(s/def ::justification (s/every (s/or :relay ::relay-message :vote ::vote-message) :min-count 1 :gen-max 4))
(s/def ::genesis-message (s/tuple ::relayer-id nil? nil?))
(s/def ::relay-message (s/or :message (s/tuple ::relayer-id ::estimate ::justification)
                             :genesis ::genesis-message))

(defn votes-in-justification
  [relay-or-vote]
   (cond
     (s/valid? ::vote-message relay-or-vote) [relay-or-vote]
     (s/valid? ::justification (last relay-or-vote)) (apply concat (map votes-in-justification (last relay-or-vote)))
     ))

(defn equivocations [relay-message]
  (->> relay-message
       votes-in-justification
       (group-by first)
       (reduce-kv (fn [m k v] (assoc m k (into #{} v))) {})
       (filter #(< 1 (count (val %))))))

(s/def ::equivocation-void-message (s/and ::relay-message #(-> % val equivocations empty?)))
(s/def ::equivocation-full-message (s/and ::relay-message #(->> % val (s/valid? ::equivocation-void-message) not)))

(defn estimate [relay-message]
  (let [votes (votes-in-justification relay-message)
        counted-votes (->> votes
                           (into #{})
                           (group-by second)
                           (reduce-kv (fn [m k v] (assoc m k (count v))) {}))]
    (case (count counted-votes)
      ;; no votes
      0 nil
      ;; unilateral consensus
      1 (ffirst counted-votes)
      ;; majority
      2 (case (apply compare (map second counted-votes))
          -1 (-> counted-votes second first)
          0 nil
          1 (-> counted-votes first first)
          ))))

(s/def ::justified-message (s/and (s/nonconforming ::relay-message) #(= (second %) (estimate %))))

(s/def ::valid-relay-message (s/and (s/nonconforming ::justified-message) ::equivocation-void-message))

(s/def ::valid-message (s/or
                        :vote ::vote-message
                        :relay ::valid-relay-message
                        ))
