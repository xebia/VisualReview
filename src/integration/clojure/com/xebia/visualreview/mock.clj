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

(ns com.xebia.visualreview.mock
  (:require [clojure.java.jdbc :as j]
            [taoensso.timbre :as timbre]
            [com.xebia.visualreview.io :as io]
            [com.xebia.visualreview.persistence.database :as db]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.itest-util :as util])
  (:import [java.io File]
           [java.nio.file Files Paths SimpleFileVisitor FileVisitResult Path LinkOption]
           [java.nio.file.attribute BasicFileAttributes]))

(def ^:dynamic *conn* {:classname      "org.h2.Driver"
                       :subprotocol    "h2"
                       :subname        "file:./target/temp/vrtest.db;TRACE_LEVEL_FILE=4"
                       :user           ""
                       :init-pool-size 1
                       :max-pool-size  1})

(defn- path-exists? [^Path path]
  (Files/exists path (into-array LinkOption nil)))

(defn delete-recursively!
  "Deletes all files and subdirectories recursively. Will not follow or delete symlinks."
  [filename]
  (let [path (Paths/get filename (into-array String nil))]
    (when (path-exists? path)
      (let [file-visitor (proxy [SimpleFileVisitor] []
                           (preVisitDirectory [_ ^BasicFileAttributes attrs]
                             (if (.isSymbolicLink attrs)
                               FileVisitResult/SKIP_SUBTREE
                               FileVisitResult/CONTINUE))
                           (visitFile [file ^BasicFileAttributes attrs]
                             (when-not (.isSymbolicLink attrs)
                               (Files/delete file))
                             FileVisitResult/CONTINUE)
                           (postVisitDirectory [dir _]
                             (Files/delete dir)
                             FileVisitResult/CONTINUE))]
        (Files/walkFileTree path file-visitor)))))

(def test-screenshot-dir "target/temp/screenshots")

(defn setup-db []
  (timbre/log :info "Setting up test database")
  (j/with-db-connection [conn *conn*]
    (j/execute! conn ["DROP ALL OBJECTS"])
    (db/run-init-script conn)))

(defn test-server-fixture [f]
  (util/start-server)
  (f)
  (util/stop-server))

(defn setup-screenshot-dir-fixture [f]
  (println "Rebinding screenshot dir to" test-screenshot-dir)
  (with-redefs [io/screenshots-dir test-screenshot-dir]
    (delete-recursively! io/screenshots-dir)
    (.mkdirs ^File (clojure.java.io/file io/screenshots-dir))
    (f)))

(defn rebind-db-spec-fixture [f]
  (println "Rebinding db spec to" (:subname *conn*))
  (with-redefs [db/conn *conn*]
    (f)))

(defn setup-db-fixture [f]
  (println "Setting up mock db")
  (setup-db)
  (f))

(defn logging-fixture [f]
  (timbre/with-logging-config {:fmt-output-fn :message} (f)))

(defn upload-tapir [run-id meta props]
  (api/upload-screenshot! run-id {:file "tapir.png" :meta meta :properties props :screenshotName "Tapir"}))
(defn upload-tapir-hat [run-id meta props]
  (api/upload-screenshot! run-id {:file "tapir_hat.png" :meta meta :properties props :screenshotName "Tapir"}))
(defn upload-chess-image-1 [run-id meta props]
  (api/upload-screenshot! run-id {:file "chess1.png" :meta meta :properties props :screenshotName "Kasparov vs Topalov - 1999"}))
(defn upload-chess-image-2 [run-id meta props]
  (api/upload-screenshot! run-id {:file "chess2.png" :meta meta :properties props :screenshotName "Kasparov vs Topalov - 1999"}))

