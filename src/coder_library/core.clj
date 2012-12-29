(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :only [label button txt shelf stack splitter
                                    grid alert jlist syntax-area menu-item]]
        [coder-library.snippets :only [load-snippets new-snippet]])
  (:import [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton
            JOptionPane JFrame SwingUtilities DefaultListModel
            JMenuBar JMenuItem JMenu JDialog]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]
           [org.fife.ui.rtextarea RTextScrollPane]
))


(def application {:snippets (atom [])
                  :code-area-content (atom "")})


(defn update-snippet-body [listSelectionEvent]
  (let [model (.getSource listSelectionEvent)
        item (.getSelectedIndex model)
        snippets (:snippets application)]
    (when-not (.getValueIsAdjusting listSelectionEvent)
      (reset! (:code-area-content application)
              (:body (nth @snippets item))))))


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


(defn make-window []
  (let [frame (JFrame. "Coder Libray")
        snippets-list-model (DefaultListModel.)
        snippets-list (jlist snippets-list-model update-snippet-body)
        code-area (syntax-area 60 20)
        new-snippet-dialog (make-new-snippet-dialog frame)
        menubar (JMenuBar.)]
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
    (.add menubar
          (doto (JMenu. "File")
            (.add (menu-item "New Snippet"
                             (fn [e] (.setVisible new-snippet-dialog true))))))
    (doto frame
      (.setJMenuBar menubar)
      (.setContentPane (splitter snippets-list
                                 (stack (RTextScrollPane. code-area))))
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
