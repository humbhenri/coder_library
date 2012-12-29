(ns coder-library.snippets
  (:import [java.util Date]))

(defn new-snippet [& args]
  (let [[lang tags body header] args]
    {:language lang
     :tags tags
     :body body
     :header header
     :modification (Date.)}))

;;; TODO
(defn load-snippets []
  (take 10 (repeat (new-snippet "language" #{:tag1 :tag2} "body" "hader"))))