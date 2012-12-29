(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :only [label button txt shelf stack splitter grid alert jlist syntax-area]]
        [coder-library.snippets :only [load-snippets new-snippet]])
  (:import [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton
            JOptionPane JFrame SwingUtilities DefaultListModel]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]
))


(def application {:snippets (atom [])})


(defn update-snippet-body [listSelectionEvent]
  (let [model (.getSource listSelectionEvent)
        item (.getFirstIndex listSelectionEvent)]
    (println " blah")
    (when (.isSelectedIndex model item)
      (prn item))))


(defn make-window []
  (let [snippets-list (jlist [] update-snippet-body)]
    (add-watch (:snippets application) nil
               (fn [_ _ _ newsnippets]
                 (SwingUtilities/invokeLater
                  (fn []
                    (let [model (:model snippets-list)]
                      (.clear model)
                      (doseq [snippet newsnippets]
                        (.addElement model (:header snippet)))
                      (.revalidate (:widget snippets-list)))))))
    (doto (JFrame. "Coder Libray")
      (.setContentPane (splitter (:widget snippets-list)
                                 (stack (syntax-area 60 20))))
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
