;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Copyright 2015 Xebia B.V.
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;  http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.xebia.visualreview.service.screenshot
  (:require [com.xebia.visualreview.service.image :as image]
            [com.xebia.visualreview.service.screenshot.persistence :as sp]
            [com.xebia.visualreview.service.service-util :as sutil]
            [slingshot.slingshot :as ex]
            [com.xebia.visualreview.service.run :as run])
  (:import [java.io File]))

(defn get-screenshot-by-id
  [conn screenshot-id]
  (sutil/attempt
    (sp/get-screenshot-by-id conn screenshot-id)
    (str "Could not retrieve screenshot with id " screenshot-id ": %s")
    ::screenshot-cannot-retrieve-from-db))

(defn get-screenshots-by-run-id
  [conn run-id]
  (sutil/attempt (sp/get-screenshots conn run-id) "Could not retrieve screenshots: %s", ::screenshot-cannot-retrieve-by-run-from-db))

(defn insert-screenshot!
  "Stores a screenshot in both database and file system. Returns the screenshot's ID.
  Throws a service-exception when the screenshot could not be stored."
  [conn run-id screenshot-name properties meta ^File file]
  (sutil/assume (not= (run/get-run conn run-id) nil) (str "Could not store screenshot, run id " run-id " does not exist.") ::screenshot-cannot-store-in-db-runid-does-not-exist)
  (let [file-size (.length file)
        image-id (image/insert-image! conn file) ]
    (ex/try+
      (sp/save-screenshot! conn run-id screenshot-name file-size properties meta image-id)
      (catch [:type :sql-exception :subtype ::sp/unique-constraint-violation] {}
        (sutil/throw-service-exception "Could not store screenshot in database: screenshot with name and properties already exists" ::screenshot-cannot-store-in-db-already-exists))
      (catch Object o
        (sutil/rethrow-as-service-exception o "Could not store screenshot in database: %s" ::screenshot-cannot-store-in-db)))))

(defn get-unused-screenshot-ids [conn]
  "Returns a vector of screenshot IDs that are not referenced by any run"
  (sutil/attempt (sp/get-unused-screenshot-ids conn) "Could not determine unused screenshots: %s", ::screenshot-cannot-retrieve-unused-from-db))

(defn delete-screenshots! [conn screenshot-ids]
  "Deletes screenshots with the given IDs."
  (sutil/attempt (sp/delete-screenshots! conn screenshot-ids) "Could not delete screenshots: %s", ::screenshot-cannot-delete-from-db))

(defn delete-unused-screenshots! [conn]
  (delete-screenshots! conn (get-unused-screenshot-ids conn)))