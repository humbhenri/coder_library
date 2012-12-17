(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :only [label button txt shelf stack splitter grid alert jlist]])
  (:import [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton
            JOptionPane]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]))

(defn make-window []
  (doto (JFrame. "Coder Libray")
    (.setContentPane (stack (jlist ["Humberto" "Henrique" "Campos" "Pinheiro"])))
    (.pack)
    (.setVisible true)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
