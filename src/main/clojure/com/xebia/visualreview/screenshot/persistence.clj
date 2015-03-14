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

(ns com.xebia.visualreview.screenshot.persistence
  (:require [slingshot.slingshot :as ex]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre]
            [com.xebia.visualreview.persistence.util :as putil])
  (:import (java.sql SQLException)))

(defn save-screenshot!
  "Stores a reference with data of a new screenshot. Returns the new screenshot id."
  [conn run-id screenshot-name size properties meta image-id]
  (try
    (timbre/log :debug (str "saving screenshot with image-id " image-id))
    (putil/insert-single! conn :screenshot {:run-id          run-id
                                      :screenshot-name screenshot-name
                                      :image-id        image-id
                                      :size            size
                                      :meta            (json/generate-string meta)
                                      :properties      (json/generate-string properties)})
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
  (putil/query conn ["SELECT screenshot.* FROM screenshot WHERE screenshot.run_id = ?" run-id]
         :row-fn (putil/parse-json-fields :meta :properties)
         :result-set-fn vec))