(ns tutorial.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str]))

(import '(javax.swing JFileChooser)
  '(javax.swing.filechooser FileNameExtensionFilter))

(defn select-file []
  (let [ extFilter (FileNameExtensionFilter. "Log File" (into-array  ["log"]))
    filechooser (JFileChooser. "C:/")
    dummy (.setFileFilter filechooser extFilter)
    retval (.showOpenDialog filechooser nil) ]
    (if (= retval JFileChooser/APPROVE_OPTION)
      (do 
        (read-write-file (.getSelectedFile filechooser) "startRendering.txt" "getRendering.txt"))
    "Select a Log File to run the program.")))

(defn read-write-file [file fileStartRendering fileGetRendering]
  ; (if (.exists (io/file (io/resource fileRendering)))
  ;   (io/delete-file (io/resource fileRendering))
  ;   (println "NÃO EXISTE"))
  ; (io/file (io/resource fileRendering) fileRendering)
  (with-open [rdr (io/reader file)]
    (doseq [line (line-seq rdr)]
      (if (and (str/includes? line "startRendering") (str/includes? line "ServiceProvider"))
          (write-file-rendering line fileStartRendering)
          (if (and (str/includes? line "getRendering") (str/includes? line "ServerSession"))
            (write-file-rendering line fileGetRendering)))))
  ; (write-export-file fileStartRendering fileGetRendering)
)

(defn write-file-rendering [line file]
  (with-open [w (io/writer  (io/resource file) :append true)]
    (.write w (str line "\n"))))

(defn write-export-file [fileStartRendering fileGetRendering]
  ; (let [alist (java.util.ArrayList.)]
  (with-open [rdr (io/reader fileStartRendering)]
    (doseq [line (line-seq rdr)]
      (if (str/includes? line "arguments")
        ; (def aString (str/split (str/trim line) #"arguments"))
        (do 
          (def splitString (str/split line #" "))
          (def aString (str (get splitString 0) " " (get splitString 1) ";" (get splitString 11) " " (get splitString 12)))
          ; (def bString (get (str/split aString #" WorkerThread") 0))
          (println aString)
        ))))
)

(defn count-start-rendering [file]
  (with-open [rdr (io/reader file)]
    (count (line-seq rdr))))

(defn -main []
  (count-start-rendering (io/resource "startRendering.txt"))
  (select-file)
  (write-export-file (io/resource "startRendering.txt") (io/resource"getRendering.txt"))
)