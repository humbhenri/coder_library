(ns coder-library.prefs
  (:import [java.util.prefs Preferences]))

;;; reference http://www.vogella.com/articles/JavaPreferences/article.html

(defn get-prefs []
  (.node (Preferences/userRoot) "Coder-Library"))


(defn put-pref [id ^java.lang.String value]
  (let [prefs (get-prefs)]
    (.put prefs id value)))


(defn get-pref [id default]
  (let [prefs (get-prefs)]
    (.get prefs id default)))
