(ns tutorial.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str]))

(import '(javax.swing JFileChooser)
  '(javax.swing.filechooser FileNameExtensionFilter))

(defn now [] (quot (System/currentTimeMillis) 1000))

(defn data-file [file] 
  (io/file (spit file ""))
)

(defn create-files [fileStartRendering fileGetRendering fileOutPut fileCountStartRendering fileDoubleRendering]
  (data-file fileStartRendering)
  (data-file fileGetRendering)
  (data-file fileOutPut)
  (data-file fileCountStartRendering)
  (data-file fileDoubleRendering)
)

(defn delete-files [fileStartRendering fileGetRendering fileCountStartRendering fileDoubleRendering]
  (io/delete-file fileStartRendering)
  (io/delete-file fileGetRendering)
  (io/delete-file fileCountStartRendering)
  (io/delete-file fileDoubleRendering)
)

(defn write-file-rendering [line file]
  (with-open [w (io/writer  file :append true)]
    (.write w (str line "\n")))
)

(defn find-substr [target needle]
  (.contains target needle)
)

(defn read-file-as-text [file]
  (slurp file)
)

(defn matching-file [file match]
  (find-substr (read-file-as-text file) match)
)

(defn count-start-rendering [file]
  (with-open [rdr (io/reader file)]
    (count (line-seq rdr)))
)

(defn write-uid-session [line fileOutPut]
  (def splitString (str/split line #";"))
  (def uId (get splitString 1))
  (def aString (str "\t \t<uid>" uId "</uid>"))
  (write-file-rendering aString fileOutPut)
  (def timeStamp "\t \t<!-- One or more timestamps of the startRendering -->")
  (write-file-rendering timeStamp fileOutPut)  
)

(defn write-doc-session [line fileOutPut]
  (def newRendering "\t<rendering>")
  (write-file-rendering newRendering fileOutPut)
  (def documentId "\t \t<!-- Document id -->")
  (write-file-rendering documentId fileOutPut)
  (def splitString (str/split line #";"))
  (def docId (str/split (get splitString 1) #"-"))
  (def aString (str "\t \t<document>" (get docId 0) "</document>"))
  (write-file-rendering aString fileOutPut)
  (def page (str "\t \t<page>" (get docId 1) "</page>"))
  (write-file-rendering page fileOutPut)
)

(defn write-close-session [fileOutPut fileCountStartRendering fileDoubleRendering]
  (def summary "\t<!-- Summary --> \n \t<summary>")
  (write-file-rendering summary fileOutPut)
  (def totalNumber (str "\t \t<!-- Total number of renderings --> \n \t \t<count>" (count-start-rendering fileCountStartRendering) "</count>"))
  (write-file-rendering totalNumber fileOutPut)
  (def duplicates (str "\t \t<!-- Number of double renderings (multiple starts with same UID) --> \n \t \t<duplicates>" (count-start-rendering fileDoubleRendering) "</duplicates>"))
  (write-file-rendering duplicates fileOutPut)
  (def noGet (str "\t \t<!-- Number of startRenderings without get --> \n \t \t<unnecessary>" 0 "</unnecessary>"))
  (write-file-rendering noGet fileOutPut)
  (def closeSummary "\t</summary>")
  (write-file-rendering closeSummary fileOutPut)
  (def strStop "</report>")
  (write-file-rendering strStop fileOutPut)
)

(defn write-export-file [fileStartRendering fileGetRendering fileOutPut fileCountStartRendering fileDoubleRendering]
  (def strStart "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n<report>")
  (write-file-rendering strStart fileOutPut)
  (with-open [rdr (io/reader fileStartRendering)]
    (doseq [line (line-seq rdr)]
      (if (str/includes? line "uid")
        (do 
          (write-uid-session line fileOutPut)
          (if (matching-file fileCountStartRendering uId)
            (write-file-rendering uId fileDoubleRendering)
          )
          (write-file-rendering uId fileCountStartRendering)
          (with-open [reader (io/reader fileStartRendering)]
            (doseq [line2 (line-seq reader)]
              (if (and (str/includes? line2 "uid") (str/includes? line2 uId))
                (do
                  (def splitString2 (str/split line2 #";"))
                  (def bString (str "\t \t<start>" (get splitString2 0) "</start>"))
                  (write-file-rendering bString fileOutPut)
                  )
            )))
          (def getTimeStamp "\t \t<!-- One or more timestamps of getRendering -->")
          (write-file-rendering getTimeStamp fileOutPut)
          (with-open [getReader (io/reader fileGetRendering)]
            (doseq [line3 (line-seq getReader)]
              (if (str/includes? line3 uId)
                (do
                  (def splitString3 (str/split line3 #";"))
                  (def cString (str "\t \t<get>" (get splitString3 0) "</get>"))
                  (write-file-rendering cString fileOutPut)
                )
              )))
          (def newRendering "\t</rendering>")
          (write-file-rendering newRendering fileOutPut)
        )
        (do
          (write-doc-session line fileOutPut)))))
          (write-close-session fileOutPut fileCountStartRendering fileDoubleRendering)
          (delete-files fileStartRendering fileGetRendering fileCountStartRendering fileDoubleRendering)
          (println (str "Your output file is ready with the name: " fileOutPut))
)

(defn read-write-file [file fileStartRendering fileGetRendering fileOutPut fileCountStartRendering fileDoubleRendering]
  (with-open [rdr (io/reader file)]
    (doseq [line (line-seq rdr)]
      (if (and (str/includes? line "startRendering") (str/includes? line "ServiceProvider"))
          (do 
            (if (str/includes? line "returned")
              (do  
                (def splitString1 (str/split line #" "))
                (def aString (str (get splitString1 0) " " (get splitString1 1) ";" (get splitString1 9) ";uid"))
                (write-file-rendering aString fileStartRendering))
              
              (do 
                (def splitString2 (str/split line #" "))
                (def bString (str (get splitString2 0) " " (get splitString2 1) ";" (str/join (re-seq #"[\p{L}\p{N}\-]" (str/replace (str (get splitString2 11) " " (get splitString2 12)) #", " "-")))))
                (write-file-rendering bString fileStartRendering)))
          )
          (if (and (str/includes? line "getRendering") (str/includes? line "ServerSession"))
            (do 
              (def splitString3 (str/split line #" "))
              (def cString (str (get splitString3 0) " " (get splitString3 1) ";" (str/join (re-seq #"[\p{L}\p{N}\-]" (str/replace (get splitString3 9) #"arguments=" "")))))
              (write-file-rendering cString fileGetRendering))))))
  (write-export-file fileStartRendering fileGetRendering fileOutPut fileCountStartRendering fileDoubleRendering)
)

(defn select-file []
  (let [ extFilter (FileNameExtensionFilter. "Log File" (into-array  ["log"]))
    filechooser (JFileChooser. (System/getProperty "user.home"))
    dummy (.setFileFilter filechooser extFilter)
    retval (.showOpenDialog filechooser nil) ]
    (if (= retval JFileChooser/APPROVE_OPTION)
      (do 
        (def fileStartRendering (str "startRendering" (now) ".txt"))
        (def fileGetRendering (str "getRendering" (now) ".txt"))
        (def fileOutPut (str "output" (now) ".xml"))
        (def fileCountStartRendering (str "countStartRendering" (now) ".txt"))
        (def fileDoubleRendering (str "doubleRendering" (now) ".txt"))
        (create-files fileStartRendering fileGetRendering fileOutPut fileCountStartRendering fileDoubleRendering)
        (read-write-file (.getSelectedFile filechooser) fileStartRendering fileGetRendering fileOutPut fileCountStartRendering fileDoubleRendering))
    "Select a Log File to run the program."))
)

(defn -main [& args]
  ; Hello, I'm Silvestre Silva and I tried to make this test as simple as possible for a static structure log file, because this is my first time learning and developing anything in the clojure.
  (select-file)
)