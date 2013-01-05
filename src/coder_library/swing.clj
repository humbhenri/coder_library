(ns coder-library.swing
  (:gen-class)
  (:import [javax.swing Box BoxLayout JTextField JPanel JSplitPane JLabel JButton
            JOptionPane DefaultListModel JList ListSelectionModel JScrollPane
            SwingUtilities JMenu JMenuItem ImageIcon JComboBox]
           [javax.swing.event ListSelectionListener]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]
           [org.fife.ui.rsyntaxtextarea RSyntaxTextArea SyntaxConstants]
           [net.miginfocom.swing MigLayout])
  (:use [clojure.java.io :only [resource]]))

;;; Joy of Clojure
(defn shelf [& components]
  (let [shelf (JPanel.)]
    (.setLayout shelf (FlowLayout.))
    (doseq [c components] (.add shelf c))
    shelf))

(defn stack [& components]
  (let [stack (Box. BoxLayout/PAGE_AXIS)]
    (doseq [c components]
      (.setAlignmentX c Component/CENTER_ALIGNMENT)
      (.add stack c))
    stack))

(defn splitter [top bottom]
  (doto (JSplitPane.)
    (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
    (.setLeftComponent top)
    (.setRightComponent bottom)))

(defn button [text f]
  (doto (JButton. text)
    (.addActionListener
     (proxy [ActionListener] []
       (actionPerformed [_] (f))))))

(defn txt [cols t]
  (doto (JTextField.)
    (.setColumns cols)
    (.setText t)))

(defn label [txt] (JLabel. txt))

(defn alert
  ([msg] (alert nil msg))
  ([frame msg]
     (javax.swing.JOptionPane/showMessageDialog frame msg)))

(defn grid [x y f]
  (let [g (doto (JPanel.)
            (.setLayout (GridLayout. x y)))]
    (dotimes [i x]
      (dotimes [j y]
        (.add g (f))))
    g))


(defn migpanel [& constraints]
  (let [[layout col row] constraints]
    (JPanel. (MigLayout. layout col row))))


(defn jlist [model selection-cb]
  (let [jlist (JList. model)]
    (doto jlist
      (.setSelectionMode ListSelectionModel/SINGLE_SELECTION)
      (.setLayoutOrientation JList/VERTICAL)
      (.setVisibleRowCount -1))
    (.addListSelectionListener jlist (proxy [ListSelectionListener] []
                                    (valueChanged [e]
                                      (selection-cb e))))
    (JScrollPane. jlist)))


(defn syntax-area [rows cols]
  (doto (RSyntaxTextArea. rows cols)
    (.setAntiAliasingEnabled true)
    (.setSyntaxEditingStyle SyntaxConstants/SYNTAX_STYLE_JAVA)))

(defn get-supported-languages []
  (for [field (.getDeclaredFields SyntaxConstants)]
    (.getName field)))

(defn set-syntax [^RSyntaxTextArea syntax-area syntax]
  (let [get-constant #(-> (.replaceAll % "SYNTAX_STYLE_" "text/") (.toLowerCase))]
    (.setSyntaxEditingStyle syntax-area (get-constant syntax))))

(defn menu-item [label action]
  (doto (JMenuItem. label)
    (.addActionListener (proxy [ActionListener] []
                          (actionPerformed [_]
                            (action))))))


(defn icon [path alt-text]
  (ImageIcon. (resource path) alt-text))


(defn combo [items selection-cb]
  (doto (JComboBox. (.toArray items))
    (.addActionListener (proxy [ActionListener] []
                          (actionPerformed [e] (selection-cb (.getSource e)))))))
