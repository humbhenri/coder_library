(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :as ui]
        [coder-library.snippets :only [load-snippets new-snippet save-snippets]]
        [coder-library.prefs :as prefs]
        [clojure.java.io :only [resource]])
  (:import [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton
            JOptionPane JFrame SwingUtilities DefaultListModel
            JMenuBar JMenuItem JMenu JDialog JToolBar JFileChooser JScrollPane
            KeyStroke]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener MouseAdapter KeyEvent InputEvent]
           [org.fife.ui.rtextarea RTextScrollPane]))

(def application {:snippets (atom [])
                  :selected (atom 0)
                  :editable (atom false)
                  :message (atom "")})

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

;;; Snippets model stuff
(defn load-data []
  (let [snippets (:snippets application)]
    (reset! snippets (load-snippets (get-db-path)))))

(defn shutdown []
  (save-snippets (get-db-path) @(:snippets application)))

(defn search-snippets
  "Return all the snippets that contains the terms inside body ou header."
  [terms]
  (filter #(or (-> (:header %) (.contains terms))
               (-> (:body %) (.contains terms))) @(:snippets application)))

;;; GUI related stuff
(defn display-msg
  "Change status bar message"
  [msg]
  (reset! (:message application) msg))

(defn select-snippet
  "Action to be executed when a item is selected on the snippets list"
  [listSelectionEvent]
  (let [model (.getSource listSelectionEvent)
        item (.getSelectedIndex model)
        snippets (:snippets application)
        editable (:editable application)]
    (when-not (.getValueIsAdjusting listSelectionEvent)
      (reset! (:selected application) item)
      (reset! editable true))))

(defn insert-snippet
  [lang body header]
  (let [snippets (:snippets application)]
    (swap! snippets conj {:language lang :body body :header header})
    (display-msg "Snippet inserted.")))


(defn save-snippet [text]
  (let [snippets (:snippets application)
        msg (:message application)
        selected (:selected application)]
    (swap! snippets assoc-in [@selected :body] text)
    (reset! msg "Saved.")))


(defn make-new-snippet-dialog
  [frame]
  (let [dialog (JDialog. frame "Insert New Snippet" true)
        code-area (ui/syntax-area 60 20)
        lang (ui/combo (ui/get-supported-languages) (fn [jcombobox] (ui/set-syntax code-area (.getSelectedItem jcombobox))))
        header (ui/txt 40 "")
        hide #(do (.setText code-area "")
                  (.setText header "")
                  (.hide dialog))
        save (ui/button "Save" #(do
                                   (insert-snippet (.getSelectedItem lang) (.getText code-area) (.getText header))
                                   (hide)))
        cancel (ui/button "Cancel" hide)
        component (ui/migpanel "fillx")]
    (doto component
      (.add (ui/label "Syntax") "split, left, width 100!")
      (.add lang "growx, wrap")
      (.add (ui/label "Description") "split, left, width 100!")
      (.add header "growx, wrap")
      (.add (RTextScrollPane. code-area) "span, grow, pushy")
      (.add save "split, right, width 100!")
      (.add cancel "width 100!"))
    (doto dialog
      (.setLocationRelativeTo nil)
      (.setVisible false)
      (.setContentPane component)
      (.setSize 640 480))))


(defn make-new-options-dialog [frame]
  (let [dialog (JDialog. frame "Options" true)
        hide #(.hide dialog)
        db-path-input (ui/txt 30 (get-db-path))
        file-chooser (doto (JFileChooser. )
                       (.setFileSelectionMode JFileChooser/DIRECTORIES_ONLY)
                       (.setAcceptAllFileFilterUsed false))
        save-btn (ui/button "Save" #(do
                                      (set-db-path (str (.getText db-path-input)))
                                      (display-msg (str "Library path changed: " (get-db-path)))
                                      (hide)))
        cancel-btn (ui/button "Cancel" hide)
        ask-db-path #(when (= (.showOpenDialog file-chooser dialog) JFileChooser/APPROVE_OPTION)
                       (SwingUtilities/invokeLater (fn [] (.setText db-path-input
                                                                    (-> (.getSelectedFile file-chooser)
                                                                        (.getAbsolutePath)
                                                                        (str file-sep db-file-name)))
                                                     (display-msg (.getAbsolutePath (.getSelectedFile file-chooser))))))]
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


(defn make-search-dialog [frame]
  (let [dialog (JDialog. frame "Search Dialog" false)
        search (ui/txt 30 "")
        model (DefaultListModel.)
        result-number (ui/label "")
        get-index #(first (for [ [i snippet] (map-indexed vector @(:snippets application))
                                 :when (= % (:header snippet)) ] i))
        result (ui/jlist model (fn [e]
                                 (let [index (get-index (.getSelectedValue (.getSource e)))]
                                   (reset! (:selected application) index))))
        hide #(do (.setText search "")
                  (.clear model)
                  (.hide dialog))
        update-result #(do (.clear model)
                         (doseq [e (search-snippets %)]
                             (.addElement model (:header e)))
                         (.setText result-number (str (.getSize model) " results")))
        ok (ui/button "OK" #(update-result (.getText search)))
        cancel (ui/button "Cancel" hide)
        component (ui/migpanel "fillx")]
    (.addActionListener search (proxy [ActionListener] []
                                 (actionPerformed [_] (SwingUtilities/invokeLater
                                                       (update-result (.getText search))))))
    (doto component
      (.add (ui/label "Search") "growx, left")
      (.add search "growx, wrap")
      (.add (JScrollPane. result) "span, grow, pushy")
      (.add result-number "growx, left")
      (.add ok "split, right, width 100!")
      (.add cancel "width 100!"))
    (doto dialog
      (.setLocationRelativeTo nil)
      (.setVisible false)
      (.setContentPane component)
      (.setSize 640 280))))


(defn make-window []
  (let [frame (JFrame. "Coder Libray")
        snippets-list-model (DefaultListModel.)
        snippets-list (jlist snippets-list-model select-snippet)
        code-area (ui/syntax-area 60 20)
        new-snippet-dialog (make-new-snippet-dialog frame)
        menubar (JMenuBar.)
        toolbar (JToolBar.)
        save-btn (ui/button "Save" (fn [] (save-snippet (.getText code-area))))
        save-menu (ui/menu-item "Save" (fn [e] (save-snippet (.getText code-area))
                                         (KeyStroke/getKeyStroke KeyEvent/VK_S InputEvent/CTRL_MASK)))
        content-pane (ui/migpanel "fillx")
        status (ui/label "")
        options-dialog (make-new-options-dialog frame)
        search-dialog (make-search-dialog frame)
        app-icon (.getImage (java.awt.Toolkit/getDefaultToolkit) (resource "icon.png"))
        syntax-combo (ui/combo (ui/get-supported-languages)
                               (fn [jcombobox] (ui/set-syntax code-area
                                                             (.getSelectedItem jcombobox))))
        snippet-name (ui/txt 30 "")]
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
                    (when (>= index 0)
                      (let [snippet (nth @(:snippets application) index)
                            title (or (:header snippet) "")
                            syntax (.indexOf (ui/get-supported-languages) (:language snippet))]
                        (.setText code-area (:body snippet))
                        (ui/set-syntax code-area (:language snippet))
                        (.setTitle frame (str "Coder Library - " title))
                        (.setSelectedIndex snippets-list index)
                        (.ensureIndexIsVisible snippets-list index)
                        (.setSelectedIndex syntax-combo (if (> syntax 0) syntax 0))
                        (.setText snippet-name title)))
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
                 (SwingUtilities/invokeLater #(.setText status newmsg))))
    (.add menubar
          (doto (JMenu. "File")
            (.add (ui/menu-item "New" #(.setVisible new-snippet-dialog true) (KeyStroke/getKeyStroke KeyEvent/VK_N InputEvent/CTRL_MASK)))
            (.add save-menu)))
    (.add menubar
          (doto (JMenu. "Edit")
            (.add (ui/menu-item "Search"
                                #(.setVisible search-dialog true)
                                (KeyStroke/getKeyStroke KeyEvent/VK_L InputEvent/CTRL_MASK)))))
    (.add menubar
          (doto (JMenu. "Tools")
            (.add (ui/menu-item "Options" #(.setVisible options-dialog true)))))
    (.setFloatable toolbar false)
    (.add toolbar
          (doto (ui/button "New" #(doto new-snippet-dialog
                                    (.setLocationRelativeTo nil)
                                    (.setVisible true)))
            (.setIcon (icon "new.gif" "New"))))
    (.add toolbar
          (doto save-btn (.setIcon (icon "save.gif" "Save"))))
    (doto content-pane
      (.add toolbar "span, grow, wrap")
      (.add (ui/splitter
             (JScrollPane. snippets-list)
             (ui/stack (RTextScrollPane. code-area)
                       (shelf snippet-name syntax-combo))) "span, grow")
      (.add status "span, grow"))
    (doto frame
      (.setIconImage app-icon)
      (.setJMenuBar menubar)
      (.setContentPane content-pane)
      (.setSize 800 600)
      (.setLocationRelativeTo nil)
      (.setVisible true))))


(defn -main
  [& args]
  (-> (Runtime/getRuntime)
      (.addShutdownHook
       (Thread. shutdown)))
  (.setDefaultCloseOperation (make-window) JFrame/EXIT_ON_CLOSE)
  (load-data)
  nil)

;;; icons http://www.oracle.com/technetwork/java/tbg-general-141722.html
