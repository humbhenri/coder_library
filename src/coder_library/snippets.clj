(ns coder-library.snippets
  (:import [java.util Date])
  (:require [clojure.java.io :as io]))

(defn new-snippet [& args]
  (let [[lang body header] args]
    {:language lang
     :body body
     :header header
     :modification (Date.)}))

(defn load-snippets [db-path]
  (try
    (load-file db-path)
    (catch java.io.FileNotFoundException e
      (vector))))

(defn save-snippets [db-path snippets-list]
  (spit db-path snippets-list))