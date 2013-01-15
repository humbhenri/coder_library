(ns coder-library.snippets
  (:import [java.util Date])
  (:use [clojure.java.io :as io]
        [clojure.string :only [upper-case]]))


(def snippets [])


(defn new-snippet [& args]
  (let [[lang body header] args]
    (def snippets (->> (conj snippets {:language lang
                                       :body body
                                       :header header
                                       :modification (Date.)})
                       (sort-by :modification)
                       (reverse)
                       (vec)))))


(defn get-all-snippets [db-path]
  (try
    (def snippets (->> (load-file db-path)
                       (sort-by :modification)
                       (reverse)
                       (vec)))
    snippets
    (catch java.io.FileNotFoundException e
      (def snippets (vector))
      snippets)))


(defn search-by [key val]
  (for [snippet snippets :when (= (key snippet) val)] snippet))


(defn save-snippets [db-path]
  (spit db-path snippets))


(defn search [term]
  (let [terms (into [] (.split term "\\s+"))]
    (distinct (for [s snippets t terms
                    :when (or (-> (upper-case (:header s)) (.contains (upper-case t)))
                              (-> (upper-case (:body s)) (.contains (upper-case t))))]
                s))))
