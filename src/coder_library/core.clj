(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :as ui]
        [coder-library.snippets :only [load-snippets new-snippet save-snippets]]
        [coder-library.prefs :as prefs])
  (:import [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton
            JOptionPane JFrame SwingUtilities DefaultListModel
            JMenuBar JMenuItem JMenu JDialog JToolBar JFileChooser]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener MouseAdapter]
           [org.fife.ui.rtextarea RTextScrollPane]))


(def application {:snippets (atom [])
                  :selected (atom 0)
                  :editable (atom false)
                  :message (atom "")})

(defn display-msg [msg]
  (reset! (:message application) msg))

;;; Node to store the preferences (Preferences API)
(def preferences "Coder Library/preferences")


(defn set-db-path [path]
  (prefs/apply-prefs {preferences {:db-path path}}))

(defn get-db-path []
  (or (prefs/get-pref-values preferences :db-path)
      (str (System/getProperty "user.home") (System/getProperty "file.separator") "coder.db")))

(defn select-snippet [listSelectionEvent]
  (let [model (.getSource listSelectionEvent)
        item (.getSelectedIndex model)
        snippets (:snippets application)
        editable (:editable application)]
    (when-not (.getValueIsAdjusting listSelectionEvent)
      (reset! (:selected application) item)
      (reset! editable true))))


(defn insert-snippet [lang body header]
  (let [snippets (:snippets application)]
    (swap! snippets conj {:language lang :body body :header header})
    (display-msg "Snippet inserted.")))


(defn make-new-snippet-dialog [frame]
  (let [dialog (JDialog. frame "Insert New Snippet" true)
        code-area (ui/syntax-area 60 20)
        lang (ui/combo (ui/get-supported-languages) (fn [jcombobox] (ui/set-syntax code-area (.getSelectedItem jcombobox))))
        header (ui/txt 40 "")
        hide #(do (.setText code-area "")
                  (.setText header "")
                  (.hide dialog))
        save-btn (ui/button "Save" #(do
                                   (insert-snippet (.getSelectedItem lang) (.getText code-area) (.getText header))
                                   (hide)))
        cancel-btn (ui/button "Cancel" hide)]
    (doto dialog
      (.setLocationRelativeTo nil)
      (.setVisible false)
      (.setContentPane (stack (shelf (ui/label "Syntax") lang)
                              (shelf (ui/label "Description") header)
                              (RTextScrollPane. code-area)
                              (shelf save-btn cancel-btn)))
      (.setSize 640 480))))

(defn make-new-options-dialog [frame]
  (let [dialog (JDialog. frame "Options" true)
        hide #(.hide dialog)
        db-path-input (ui/txt 30 (get-db-path))
        file-chooser (doto (JFileChooser. )
                       (.setFileSelectionMode JFileChooser/DIRECTORIES_ONLY))
        save-btn (ui/button "Save" #(do
                                      (set-db-path (str (.getText db-path-input)))
                                      (display-msg (str "Library path changed: " (get-db-path)))
                                      (hide)))
        cancel-btn (ui/button "Cancel" hide)
        ask-db-path #(when (= (.showOpenDialog file-chooser dialog) JFileChooser/APPROVE_OPTION)
                       (SwingUtilities/invokeLater (fn [] (.setText db-path-input
                                                                    (-> (.getSelectedFile file-chooser)
                                                                        (.getAbsolutePath)
                                                                        (str (System/getProperty "file.separator") "coder.db"))))))]
    (.addMouseListener db-path-input (proxy [MouseAdapter] []
                                       (mouseClicked [mouseEvent]
                                         (ask-db-path))))
    (doto dialog
      (.setLocationRelativeTo nil)
      (.setVisible false)
      (.setContentPane (stack (shelf (ui/label "Directory to store the library:") db-path-input)
                              (shelf save-btn cancel-btn)))
      (.setSize 640 480)
      (.pack))))


(defn edit-snippet []
  (reset! (:editable application) true))


(defn save-snippet [text]
  (let [snippets (:snippets application)
        msg (:message application)
        selected (:selected application)]
    (swap! snippets assoc-in [@selected :body] text)
    (reset! msg "Saved.")))


(defn make-window []
  (let [frame (JFrame. "Coder Libray")
        snippets-list-model (DefaultListModel.)
        snippets-list (jlist snippets-list-model select-snippet)
        code-area (ui/syntax-area 60 20)
        new-snippet-dialog (make-new-snippet-dialog frame)
        menubar (JMenuBar.)
        toolbar (JToolBar.)
        save-btn (ui/button "Save" (fn [] (save-snippet (.getText code-area))))
        save-menu (ui/menu-item "Save" (fn [e] (save-snippet (.getText code-area))))
        content-pane (ui/migpanel "fillx")
        status (ui/label "")
        options-dialog (make-new-options-dialog frame)]
    (add-watch (:snippets application) nil
               (fn [_ _ _ newsnippets]
                 (SwingUtilities/invokeLater
                  (fn []
                    (.clear snippets-list-model)
                    (doseq [snippet newsnippets]
                      (.addElement snippets-list-model (:header snippet)))
                    (.revalidate snippets-list)))))
    (.setEditable code-area false)
    (add-watch (:selected application) nil
               (fn [_ _ _ index]
                 (SwingUtilities/invokeLater
                  (fn []
                    (let [snippet (nth @(:snippets application) index)
                          window-title (:header snippet)]
                      (.setText code-area (:body snippet))
                      (ui/set-syntax code-area (:language snippet))
                      (.setTitle frame (str "Coder Library - " window-title)))
                    (.revalidate frame)))))
    (add-watch (:editable application) nil
               (fn [_ _ _ editable]
                 (SwingUtilities/invokeLater
                  (fn []
                    (.setEditable code-area editable)
                    (.setEnabled save-btn editable)
                    (.setEnabled save-menu editable)))))
    (add-watch (:message application) nil
               (fn [_ _ _ newmsg]
                 (SwingUtilities/invokeLater
                  (fn [] (.setText status newmsg)))))
    (.add menubar
          (doto (JMenu. "File")
            (.add (ui/menu-item "New" #(.setVisible new-snippet-dialog true)))
            (.add save-menu)))
    (.add menubar
          (doto (JMenu. "Tools")
            (.add (ui/menu-item "Options" #(.setVisible options-dialog true)))))
    (.setFloatable toolbar false)
    (.add toolbar
          (doto (ui/button "New" (fn [] (doto new-snippet-dialog
                                          (.setLocationRelativeTo nil)
                                          (.setVisible true))))
            (.setIcon (icon "new.gif" "New"))))
    (.add toolbar
          (doto save-btn (.setIcon (icon "save.gif" "Save"))
                (.setEnabled false)))
    (doto content-pane
      (.add toolbar "span, grow")
      (.add (ui/splitter snippets-list (ui/stack (RTextScrollPane. code-area))) "span, grow")
      (.add status "span, grow"))
    (doto frame
      (.setJMenuBar menubar)
      (.setContentPane content-pane)
      (.setSize 800 600)
      (.setLocationRelativeTo nil)
      (.setVisible true))))


(defn load-data []
  (let [snippets (:snippets application)]
    (reset! snippets (load-snippets (get-db-path)))))

(defn shutdown []
  (save-snippets (get-db-path) @(:snippets application)))

(defn -main
  [& args]
  (-> (Runtime/getRuntime)
      (.addShutdownHook
       (Thread. shutdown)))
  (.setDefaultCloseOperation (make-window) JFrame/EXIT_ON_CLOSE)
  (load-data)
  nil)

;;; icons http://www.oracle.com/technetwork/java/tbg-general-141722.html