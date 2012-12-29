(ns coder-library.core
  (:gen-class)
  (:use [coder-library.swing :only [label button txt shelf stack splitter grid alert jlist syntax-area]])
  (:import [javax.swing Box BoxLayout JTextField JPanel
            JSplitPane JLabel JButton
            JOptionPane JFrame]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]))

(defn make-window []
  (doto (JFrame. "Coder Libray")
    (.setContentPane (splitter (jlist ["Humberto" "Henrique" "Campos" "Pinheiro"])
                               (stack (syntax-area 60 20))))
    (.setSize 640 480)
    (.setVisible true)))


(defn -main
  [& args]
  (make-window))
