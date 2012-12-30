(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :only [label button txt shelf stack splitter
                                    grid alert jlist syntax-area menu-item icon migpanel]]
        [coder-library.snippets :only [load-snippets new-snippet]])
  (:import [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton
            JOptionPane JFrame SwingUtilities DefaultListModel
            JMenuBar JMenuItem JMenu JDialog JToolBar]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]
           [org.fife.ui.rtextarea RTextScrollPane]))


(def application {:snippets (atom [])
                  :code-area-content (atom "")
                  :editable (atom false)
                  :message (atom "")})


(defn update-snippet-body [listSelectionEvent]
  (let [model (.getSource listSelectionEvent)
        item (.getSelectedIndex model)
        snippets (:snippets application)
        editable (:editable application)]
    (when-not (.getValueIsAdjusting listSelectionEvent)
      (reset! (:code-area-content application)
              (:body (nth @snippets item)))
      (reset! editable false))))


(defn make-new-snippet-dialog [frame]
  (let [dialog (JDialog. frame "Insert New Snippet" true)
        save-btn (button "Save" #(alert "Saved"))
        cancel-btn (button "Cancel" #(alert "Cancel"))
        code-area (syntax-area 60 20)]
    (doto dialog
      (.setVisible false)
      (.setContentPane
       (stack (shelf (label "Language:") (label "..."))
              (label "Snippet")
              (RTextScrollPane. code-area)
              (shelf save-btn cancel-btn)))
      (.setSize 320 240))))


(defn insert-new-snippet []
  (do
    (reset! (:editable application) true)
    (reset! (:code-area-content application) "")))


(defn edit-snippet []
  (reset! (:editable application) true))


(defn save-snippet [text]
  (let [snippets (:snippets application)
        edit (:editable application)
        msg (:message application)]
    (swap! snippets conj (new-snippet "java" text "test"))
    (reset! edit false)
    (reset! msg "Saved.")))


(defn make-window []
  (let [frame (JFrame. "Coder Libray")
        snippets-list-model (DefaultListModel.)
        snippets-list (jlist snippets-list-model update-snippet-body)
        code-area (syntax-area 60 20)
        new-snippet-dialog (make-new-snippet-dialog frame)
        menubar (JMenuBar.)
        toolbar (JToolBar.)
        save-btn (button "Save" (fn [] (save-snippet (.getText code-area))))
        save-menu (menu-item "Save" (fn [e] (save-snippet (.getText code-area))))
        content-pane (migpanel "fillx")
        status (label "")]
    (add-watch (:snippets application) nil
               (fn [_ _ _ newsnippets]
                 (SwingUtilities/invokeLater
                  (fn []
                    (.clear snippets-list-model)
                    (doseq [snippet newsnippets]
                      (.addElement snippets-list-model (:header snippet)))
                    (.revalidate snippets-list)))))
    (.setEditable code-area false)
    (add-watch (:code-area-content application) nil
               (fn [_ _ _ newcontent]
                 (SwingUtilities/invokeLater
                  (fn []
                    (.setText code-area newcontent)
                    (.revalidate code-area)))))
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
    (.setEnabled save-menu false)
    (.add menubar
          (doto (JMenu. "File")
            (.add (menu-item "New" (fn [e] (insert-new-snippet))))
            (.add save-menu)))
    (.setFloatable toolbar false)
    (.add toolbar
          (doto (button "New" (fn [] (do
                                       (insert-new-snippet))))
            (.setIcon (icon "new.gif" "New"))))
    (.add toolbar
          (doto save-btn (.setIcon (icon "save.gif" "Save"))
                (.setEnabled false)))
    (doto content-pane
      (.add toolbar "span, grow")
      (.add (splitter snippets-list (stack (RTextScrollPane. code-area))) "span, grow")
      (.add status "span, grow"))
    (doto frame
      (.setJMenuBar menubar)
      ;; (.setContentPane (stack
      ;;                   toolbar
      ;;                   (splitter snippets-list
      ;;                             (stack
      ;;                              (RTextScrollPane. code-area)))))
      (.setContentPane content-pane)
      (.setSize 640 480)
      (.setVisible true))))


(defn load-data []
  (let [snippets (:snippets application)]
    (reset! snippets (load-snippets))))


(defn -main
  [& args]
  (make-window)
  (load-data)
  nil)

;;; icons http://www.oracle.com/technetwork/java/tbg-general-141722.html