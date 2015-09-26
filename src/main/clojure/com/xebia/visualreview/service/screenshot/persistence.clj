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

(ns com.xebia.visualreview.service.screenshot.persistence
  (:require [slingshot.slingshot :as ex]
            [cheshire.core :as json]
            [com.xebia.visualreview.service.persistence.util :as putil]
            [com.xebia.visualreview.logging :as log])
  (:import [java.sql SQLException]))

(defn save-screenshot!
  "Stores a reference with data of a new screenshot. Returns the new screenshot id."
  [conn run-id screenshot-name size properties meta image-id]
  (try
    (log/debug (str "saving screenshot with image-id " image-id))
    (let [
          screenshot-id (putil/insert-single! conn :screenshot {
                                            :screenshot-name screenshot-name
                                            :image-id        image-id
                                            :size            size
                                            :meta            (json/generate-string meta)
                                            :properties      (json/generate-string properties)})]
      (do (putil/insert-single! conn :run_screenshots { :screenshot-id   screenshot-id
                                                       :run-id          run-id})
          screenshot-id))
    (catch SQLException e
      (if (putil/unique-constraint-violation? e)
        (ex/throw+ {:type    :sql-exception
                    :subtype ::unique-constraint-violation
                    :message (.getMessage e)})
        (throw e)))))

(defn get-screenshot-by-id [conn screenshot-id]
  (putil/query-single conn ["SELECT screenshot.* FROM screenshot, image WHERE screenshot.id = ?" screenshot-id]
                      :row-fn (putil/parse-json-fields :meta :properties)
                      :result-set-fn vec))

(defn get-screenshots [conn run-id]
  (putil/query conn ["SELECT screenshot.* FROM screenshot, run_screenshots WHERE run_screenshots.run_id = ? AND screenshot.id = run_screenshots.screenshot_id" run-id]
               :row-fn (putil/parse-json-fields :meta :properties)
               :result-set-fn vec))

(defn delete-screenshots! [conn screenshot-ids]
  "Deletes screenshots with the given IDs"
  (putil/delete! conn :screenshot (into [(str "id IN (" (putil/sql-param-list (count screenshot-ids)) " )")] screenshot-ids)))

(defn get-unused-screenshot-ids [conn]
  "Returns a vector of screenshot- and image-IDs that are not referenced by any run"
  (putil/query conn ["SELECT id FROM screenshot where id NOT IN (select screenshot_id from run_screenshots)"]
               :row-fn :id
               :result-set-fn vec))
