(ns coder-library.snippets
  (:import [java.util Date])
  (:use [clojure.java.io :as io]))


(def snippets [])


(defn new-snippet [& args]
  (let [[lang body header] args]
    (def snippets (conj snippets {:language lang
                                  :body body
                                  :header header
                                  :modification (Date.)}))))


(defn get-all-snippets [db-path]
  (try
    (def snippets (load-file db-path))
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
                    :when (or (.contains (:header s) t)
                              (.contains (:body s) t))]
                s))))
