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

(ns com.xebia.visualreview.service.analysis
  (:require [com.xebia.visualreview.util :refer :all]
            [com.xebia.visualreview.service.persistence.util :as putil]
            [com.xebia.visualreview.service.service-util :as sutil]))

;; Analysis
(defn create-analysis!
  "Returns the generated analysis id"
  [conn baseline-node run-id]
  (putil/insert-single! conn :analysis {:baseline-node baseline-node :run-id run-id}))

(defn get-analysis
  [conn run-id]
  (putil/query-single conn
    ["SELECT analysis.*, project.id project_id, project.name project_name, suite.id suite_id, suite.name suite_name
     FROM analysis
     JOIN run ON run.id = analysis.run_id
     JOIN suite ON suite.id = run.suite_id
     JOIN project ON project.id = suite.project_id
     WHERE run_id = ?" run-id]
    :row-fn sutil/format-dates))

(defn get-full-analysis [conn run-id]
  (let [analysis (get-analysis conn run-id)
        diffs (putil/query conn
                ["SELECT diff.*,
                  sbefore.size before_size,
                  sbefore.meta before_meta,
                  sbefore.properties before_properties,
                  sbefore.screenshot_name before_name,
                  sbefore.image_id before_image_id,
                  safter.size after_size,
                  safter.meta after_meta,
                  safter.properties after_properties,
                  safter.screenshot_name after_name,
                  safter.image_id after_image_id FROM analysis
                  JOIN diff ON diff.analysis_id = analysis.id
                  JOIN screenshot safter ON safter.id = diff.after
                  LEFT JOIN screenshot sbefore ON sbefore.id = diff.before
                  WHERE analysis.run_id = ?" run-id]
                :row-fn (comp (putil/parse-json-fields :before-meta :before-properties :after-meta :after-properties) sutil/format-dates)
                :result-set-fn vec)]
    {:analysis analysis :diffs diffs}))

;; Diffs
(defn save-diff!
  "Stores a new diff. Returns the new diff's id."
  [conn image-id before after percentage analysis-id]
  (putil/insert-single! conn :diff {:before      before
                                    :after       after
                                    :percentage  percentage
                                    :status      "pending"
                                    :analysis-id analysis-id
                                    :image-id    image-id}))

(defn get-diff [conn run-id diff-id]
  (putil/query-single conn
    ["SELECT diff.* FROM diff
      JOIN analysis ON analysis.id = diff.analysis_id
      JOIN run ON run.id = analysis.run_id
      WHERE run.id = ? AND diff.id = ?" run-id diff-id]))

(defn update-diff-status! [conn diff-id status]
  (putil/update! conn :diff {:status status} ["id = ?" diff-id]))


(defn get-analysis-compound-status
  "Returns 'accepted' when all screenshots in the analysis have the status 'accepted'.
   Returns 'rejected' when one of the screenshots in the analysis has the status 'rejected'
   Returns 'pending' when none of the screenshots in the analysis has the status 'rejected' and at least one
   has 'pending'"
  [conn run-id]
  (let [diffs (:diffs (get-full-analysis conn run-id))
        nr-of-rejected (count (filter (fn [diff] (= (:status diff) "rejected")) diffs))
        nr-of-pending (count (filter (fn [diff] (= (:status diff) "pending")) diffs))]
    (if (< (count diffs) 1)
      "empty"
      (if (> nr-of-rejected 0)
        "rejected"
        (if (> nr-of-pending 0)
          "pending"
          "accepted"
          )))))
