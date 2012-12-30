(ns coder-library.snippets
  (:import [java.util Date]))

(defn new-snippet [& args]
  (let [[lang body header] args]
    {:language lang
     :body body
     :header header
     :modification (Date.)}))

;;; TODO
(defn load-snippets []
  (take 10 (repeat (new-snippet "language" "body" "header"))))