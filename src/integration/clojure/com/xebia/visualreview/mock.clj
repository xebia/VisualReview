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
            [com.xebia.visualreview.api-test :as api]))

(def ^:dynamic *conn* {:classname      "org.h2.Driver"
                       :subprotocol    "h2"
                       :subname        "file:./target/temp/vrtest.db"
                       :user           "`"
                       :init-pool-size 1
                       :max-pool-size  1})

(defn setup-db []
  (timbre/info "Setting up test database")
  (j/with-db-connection [conn *conn*]
    (j/execute! conn ["DROP ALL OBJECTS"])
    (com.xebia.visualreview.persistence.database/run-init-script conn)))

(defmacro rebind-screenshots-dir [& body]
  `(with-redefs [io/screenshots-dir "target/temp/screenshots"] ~@body))

(defmacro rebind-db-spec [& body]
  `(with-redefs [db/conn *conn*] ~@body))

(defn upload-tapir [run-id meta props]
  (api/upload-screenshot! run-id {:file "tapir.png" :meta meta :properties props :screenshotName "Tapir"}))
(defn upload-tapir-hat [run-id meta props]
  (api/upload-screenshot! run-id {:file "tapir_hat.png" :meta meta :properties props :screenshotName "Tapir"}))
(defn upload-chess-image-1 [run-id meta props]
  (api/upload-screenshot! run-id {:file "chess1.png" :meta meta :properties props :screenshotName "Kasparov vs Topalov - 1999"}))
(defn upload-chess-image-2 [run-id meta props]
  (api/upload-screenshot! run-id {:file "chess2.png" :meta meta :properties props :screenshotName "Kasparov vs Topalov - 1999"}))