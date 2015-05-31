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

(ns com.xebia.visualreview.service.run
  (:require [com.xebia.visualreview.service.persistence.util :as putil]
            [com.xebia.visualreview.service.analysis :as analysis]
            [com.xebia.visualreview.service.service-util :as sutil]
            [com.xebia.visualreview.service.baseline :as baseline])
  (:import (java.sql Timestamp)
           (java.util Date)))

;; Runs
(defn create-run!
  "Creates a run for the given project and suite names.
  If the suite does not yet exist it will be created along with a new baseline.
  Creating a run also creates an analysis for the run, this may change in the future.
  Returns the created run id."
  [conn suite-id]
  (let [baseline (baseline/get-baseline-head conn suite-id "master")
        new-run-id (putil/insert-single! conn :run {:suite-id    suite-id
                                                    :start-time  (Timestamp. (.getTime (Date.)))
                                                    :branch-name "master"
                                                    :status      "running"})
        _ (analysis/create-analysis! conn baseline new-run-id)]
    new-run-id))

(defn get-run
  "Returns the data for the given run-id"
  [conn run-id]
  (putil/query-single conn
                      ["SELECT run.*, project.id project_id FROM run
          JOIN suite ON run.suite_id = suite.id JOIN project ON suite.project_id = project.id
          WHERE project.id = suite.project_id AND suite.id = run.suite_id AND run.id = ?" run-id]
                      :row-fn sutil/format-dates))

(def ^:private get-suite-runs-sql
  "SELECT run.* FROM run JOIN suite ON suite.id = run.suite_id
  JOIN project ON suite.project_id = project.id
  WHERE suite.id = ? AND project.id = ?
  ORDER BY run.start_time DESC")
(defn get-runs
  "Returns the list of runs for the given suite"
  [conn project-id suite-id]
  (putil/query conn [get-suite-runs-sql suite-id project-id]
               :row-fn (comp #(assoc % :project-id project-id) sutil/format-dates)
               :result-set-fn vec))

(defn delete-run!
  "Deletes a run and all attached analyses and screenshot metadata.
  Images metadata and binary files are left intact.
  Returns true when deletion was succesful."
  [conn run-id]
  {:pre (number? run-id)}
  (sutil/attempt
    (do (putil/delete! conn :run ["id = ?" run-id])
        true)
    "Could not delete run: %s"
    ::delete-by-id-failed))