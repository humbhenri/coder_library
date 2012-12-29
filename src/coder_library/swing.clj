(ns coder-library.swing
  (:gen-class)
  (:import [javax.swing Box BoxLayout JTextField JPanel JSplitPane JLabel JButton
            JOptionPane DefaultListModel JList ListSelectionModel JScrollPane
            SwingUtilities]
           [javax.swing.event ListSelectionListener]
           [java.awt BorderLayout Component GridLayout FlowLayout]
           [java.awt.event ActionListener]
           [org.fife.ui.rsyntaxtextarea RSyntaxTextArea SyntaxConstants]
           [org.fife.ui.rtextarea RTextScrollPane]))

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

(defn jlist [items selection-cb]
  (let [model (DefaultListModel.)
        jlist (JList. model)]
    (doto jlist
      (.setSelectionMode ListSelectionModel/SINGLE_INTERVAL_SELECTION)                (.setLayoutOrientation JList/VERTICAL)
      (.setVisibleRowCount -1))
    (.addListSelectionListener (.getSelectionModel jlist) (proxy [ListSelectionListener] []
                                    (valueChanged [e]
                                      (SwingUtilities/invokeLater (selection-cb e))
                                      (.revalidate jlist))))

    (doseq [item items]
      (.addElement model item))
    {:widget (JScrollPane. jlist)
     :model model
     :jlist jlist}))


(defn syntax-area [rows cols]
  (RTextScrollPane.
   (doto (RSyntaxTextArea. rows cols)
     (.setAntiAliasingEnabled true)
     (.setSyntaxEditingStyle SyntaxConstants/SYNTAX_STYLE_JAVA))))