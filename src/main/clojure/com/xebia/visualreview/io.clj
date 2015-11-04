;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Copyright 2015 Xebia B.V.
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.xebia.visualreview.io
  (:import [java.io FileNotFoundException File IOException]
           [java.nio.file NotDirectoryException Files AccessDeniedException])
  (:require [clojure.java.io :as io]))

(def screenshots-dir "screenshots")

(defn- ex-msg [file s] (str "The screenshot directory \"" file "\" " s))
(defn- assert-accessible-dir! [^File file]
  (let [file-exists? (or (.exists file) (.mkdirs file))
        path (.toPath file)]
    (cond
      (not (Files/isReadable path))
      (throw (AccessDeniedException. (str path) nil (ex-msg path "is not readable.")))

      (not (Files/isWritable path))
      (throw (AccessDeniedException. (str path) nil (ex-msg path "is not writable.")))

      (not (Files/isExecutable path))
      (throw (AccessDeniedException. (str path) nil (ex-msg path "is not accessible.")))

      (not (.isDirectory file)) (throw (NotDirectoryException. (str file)))

      :else file-exists?)))

(defn init-screenshots-dir!
  "Will initialize the directory for local screenshots storage and try
   to create it if it doesn't exist. Will throw when dir can not be
   accessed for reading and writing"
  [dir]
  (let [dir (or dir "screenshots")
        file (io/file dir)]
    (if (assert-accessible-dir! file)
      (alter-var-root #'screenshots-dir (fn [_] dir))
      (throw (IOException. ^String (ex-msg file "could not be created."))))))

(defn get-file [file-path]
  {:pre [file-path]}
  (let [file (io/file screenshots-dir file-path)]
    (if (.exists file)
      file
      (throw (FileNotFoundException. (format "File at %s does not exist." (str (.getPath file))))))))

(defn store-png-image!
  "Stores the given file to the given path"
  [file directory id]
  {:pre [file directory id]}
  (let [dest (io/file screenshots-dir directory (str id ".png"))]
    (do
      (.mkdirs (.getParentFile dest))
      (io/copy file dest))))
