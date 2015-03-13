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
  (:import [java.io File FileNotFoundException]
           [java.nio.file NotDirectoryException Files AccessDeniedException LinkOption]
           [java.nio.file.attribute FileAttribute]
           [javax.imageio ImageIO]
           [java.awt.image BufferedImage])
  (:require [clojure.java.io :as io]))

(def screenshots-dir "screenshots")

(defn init-screenshots-dir! [dir]
  (let [dir (or dir "screenshots")
        file (io/file dir)
        path (.toPath file)
        ex-msg (fn [s] (str "The screenshot directory \"" dir "\" " s))]
    (cond
      (not (.exists file)) (throw (IllegalArgumentException. ^String (ex-msg "does not exist.")))
      (not (.isDirectory file)) (throw (NotDirectoryException. dir))
      (not (Files/isReadable path)) (throw (AccessDeniedException. dir nil (ex-msg "is not readable.")))
      (not (Files/isWritable path)) (throw (AccessDeniedException. dir nil (ex-msg "is not writable.")))
      :else (alter-var-root #'screenshots-dir (fn [_] dir)))))

(defn create-project-directory! [project-id]
  {:pre [project-id]}
  (.mkdir ^File (io/file screenshots-dir (str project-id))))

(defn create-run-directory!
  "Creates the path <project-id>/<suite-id>/<run-id> relative to the screenshots-dir.
  Will automatically create any missing parent directory."
  [project-id suite-id run-id]
  {:pre [project-id suite-id run-id]}
  (let [[project-dir suite-dir run-dir] (mapv str [project-id suite-id run-id])
        diffs-file (.toPath (io/file screenshots-dir project-dir suite-dir "diffs"))
        run-file (.toPath (io/file screenshots-dir project-dir suite-dir run-dir))]
    (when (Files/notExists run-file (make-array LinkOption 0))
      (Files/createDirectories run-file (make-array FileAttribute 0)))
    (when (Files/notExists diffs-file (make-array LinkOption 0))
      (Files/createDirectories diffs-file (make-array FileAttribute 0)))))

(defn store-screenshot! [project-id suite-id run-id screenshot-id file]
  {:pre [project-id suite-id run-id screenshot-id file]}
  (let [[project-dir suite-dir run-dir screenshot-fname] (map str [project-id suite-id run-id screenshot-id])
        dest (io/file screenshots-dir project-dir suite-dir run-dir (str screenshot-fname ".png"))]
    (io/copy file dest)))

(defn store-diff! [project-id suite-id diff-id ^BufferedImage diff-image]
  {:pre [project-id suite-id diff-id] :post [true?]}
  (let [[project-dir suite-dir] (map str [project-id suite-id])
        dest (io/file screenshots-dir project-dir suite-dir "diffs" (str diff-id ".png"))]
    (ImageIO/write diff-image "png" dest)))

(defn get-file [file-path]
  {:pre [file-path]}
  (let [file (io/file screenshots-dir file-path)]
    (if (.exists file)
      file
      (throw (FileNotFoundException. (format "File at %s does not exist." (str (.getPath file))))))))

(defn store-image!
  "Stores the given file to the given path"
  [file directory id]
  {:pre [file directory id]}
  (let [dest (io/file screenshots-dir directory (str id ".png"))]
    (do
      (.mkdirs (.getParentFile dest))
      (io/copy file dest))))