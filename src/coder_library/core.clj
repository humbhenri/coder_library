(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :as ui]
        [coder-library.snippets :as model]
        [coder-library.prefs :as prefs]
        [clojure.java.io :only [resource]])
  (:import [javax.swing JFrame DefaultListModel UIManager]))

;;; App logic
(def *snippets-list* (atom []))

(def edit? true)                        ; true if is editing, false if
                                        ; is a new snippet

(declare get-db-path)

(defn load-all-snippets []
  (reset! *snippets-list* (map :header (model/get-all-snippets (get-db-path)))))

(defn search-snippets [term]
  (reset! *snippets-list* (map :header (model/search term))))

(defmacro when-snippets-change [& body]
  `(add-watch
    *snippets-list* ""
    (fn [~'_ ~'_ ~'_ ~'snippets]
      ~@body)))


;;; User Preferences
(def file-sep (System/getProperty "file.separator"))

(def db-file-name "coder.db")

(def default-db-path (str (System/getProperty "user.home")
                         file-sep
                         db-file-name))

(defn set-db-path [path]
  (prefs/put-pref "DB-PATH" path))

(defn get-db-path []
  (prefs/get-pref "DB-PATH" default-db-path))


;;; Build GUI
(defn build-snippets-list []
  (let [model (DefaultListModel.)
        s-list (ui/jlist model)]
    (when-snippets-change (ui/do-swing
                           (.clear model)
                           (doseq [snippet snippets]
                             (.addElement model snippet))))
    s-list))

(defn window-content []
  (let [search-widget (ui/txt 30 "")
        syntax-area   (ui/syntax-area 60 20)
        new           (ui/button (ui/icon "new.gif" "New"))
        save          (ui/button (ui/icon "save.gif" "Save"))
        delete        (ui/button (ui/icon "del.gif" "Delete"))
        preferences   (ui/button (ui/icon "preferences.gif" "Preferences"))
        search        (ui/button (ui/icon "search.gif" "Search"))
        description   (ui/txt 30 "")
        languages     (ui/combo (ui/get-supported-languages) #())
        snippets-list (build-snippets-list)
        split         (ui/vertical-splitter (ui/scrollpane snippets-list)
                                            (ui/rscrollpane syntax-area))]
    (.setDividerLocation split 200)
    (.setEnabled save false)
    (.setEnabled delete false)
    (ui/selection-listener snippets-list (let [descr (.getSelectedValue snippets-list)
                                               [snippet & more] (model/search-by :header descr)]
                                           (ui/set-syntax syntax-area (:language snippet))

                                           (.setText syntax-area (:body snippet))
                                           (.setText description descr)
                                           (.setSelectedItem languages (:language snippet))
                                           (def edit? true)))
    (doseq [c [search search-widget]]
      (ui/action-listener c (search-snippets (.getText search-widget))))
    (ui/action-listener new (do (search-snippets "") ;clear
                                (def edit? false)
                                (.setEnabled save true)))
    (ui/action-listener save (if edit?
                               (-> (model/search-by :header (.getSelectedValue snippets-list))
                                   (assoc :header (.getText description)
                                          :body (.getText syntax-area)
                                          :language (.getSelectedItem languages)
                                          :modification (Date.))
                                   (assoc ))
                               (do (model/new-snippet (.getSelectedItem languages)
                                                      (.getText syntax-area)
                                                      (.getText description))
                                   (search-snippets "")
                                   (.setEnabled save false))))
    (doto (ui/migpanel "fillx")
      (.add search        "split, width 30")
      (.add search-widget "span, growx, wrap")
      (.add split         "span, grow, pushy")
      (.add new           "split, left, width 30!")
      (.add preferences   "split, left, width 30!")
      (.add (ui/label     "Description:") "split, left")
      (.add description   "span, growx")
      (.add languages     "split, right")
      (.add save          "split, right, width 30!")
      (.add delete        "split, right, width 30!"))))


;;; Main Window
(defn make-window []
  (let [frame (JFrame. "Coder Libray")
        app-icon (.getImage (java.awt.Toolkit/getDefaultToolkit) (resource "icon.png"))]
    (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
    (doto frame
      (.setIconImage app-icon)
      (.setContentPane (window-content))
      (.setSize 480 640)
      (.setLocationRelativeTo nil)
      (.setVisible true))))


(defn -main [& args]
  (.setDefaultCloseOperation (make-window) JFrame/EXIT_ON_CLOSE)
  (load-all-snippets)
  (-> (Runtime/getRuntime) (.addShutdownHook (Thread. #(model/save-snippets (get-db-path)))))
  nil)
