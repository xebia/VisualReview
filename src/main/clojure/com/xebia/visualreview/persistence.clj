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

(ns com.xebia.visualreview.persistence
  (:require [clojure.java.jdbc :as j]
            [com.xebia.visualreview.util :refer :all]
            [slingshot.slingshot :as ex])
  (:import [java.sql Timestamp]
           [java.util Date]
           [java.sql SQLException]))

(defn- unique-constraint-violation? [^SQLException e]
  (= (.getSQLState e) "23505"))

(defn- ent-fn [^String s] (.replace s \- \_))
(defn- ident-fn [^String s] (.replace (.toLowerCase s) \_ \-))

(def ^:private h2-generated-key (keyword "scope_identity()"))
(defn- extract-generated-id
  "Workaround for incompatibilities of the clojure.java.jdbc update! and insert! methods between JDBC drivers.
   For example: the H2 driver returns a :scope_identity() key, while PostgreSQL's driver returns the actual table column names as keys.
   This function returns the generated id as a number.
   Important note: this function assumes jdbc-returned-keys only returns 1 key (which is true for our application at time of writing)."
  [{h2-id h2-generated-key id :id}]
  (or h2-id id))

(defn- insert-single! [conn table row-map & opts]
  (extract-generated-id (first (apply j/insert! conn table row-map :entities ent-fn opts))))
(defn- update! [conn table set-map where-clause & opts]
  (apply j/update! conn table set-map where-clause :entities ent-fn opts))
(defn- query [conn sql-and-params & opts]
  (apply j/query conn sql-and-params :identifiers ident-fn opts))
(defn- query-single [& args]
  (first (apply query args)))

(defn- format-dates [run-row]
  (-> run-row
      (format-date :start-time)
      (format-date :end-time)
      (format-date :creation-time)))

(defn get-suite-by-name
  ([conn project-name suite-name]
    (get-suite-by-name conn project-name suite-name identity))
  ([conn project-name suite-name row-fn]
    (query-single conn
      ["SELECT suite.* FROM suite JOIN project ON project_id = project.id WHERE project.name = ? AND suite.name = ?" project-name suite-name]
      :row-fn row-fn)))

(defn get-suite-by-id
  ([conn project-id suite-id]
    (get-suite-by-id conn project-id suite-id identity))
  ([conn project-id suite-id row-fn]
    (query-single conn
      ["SELECT suite.* FROM suite JOIN project ON suite.project_id = project.id WHERE suite.id = ? AND project.id = ?" suite-id project-id]
      :row-fn row-fn)))

(defn get-project-by-name
  ([conn project-name]
    (get-project-by-name conn project-name identity))
  ([conn project-name row-fn]
    (query-single conn ["SELECT * FROM project WHERE name = ?" project-name] :row-fn row-fn)))

(defn get-project-by-id
  ([conn project-id]
    (get-project-by-id conn project-id identity))
  ([conn project-id row-fn]
    (query-single conn ["SELECT project.* FROM project WHERE id = ?" project-id] :row-fn row-fn)))

(defn create-suite-for-project!
  "Creates a new suite with an empty baseline for the given project. Returns the created suite's id."
  [conn project-name suite-name]
  (let [project-id (get-project-by-name conn project-name :id)
        new-suite-id (insert-single! conn :suite {:project-id project-id :name suite-name})]
    (insert-single! conn :baseline {:suite-id new-suite-id})
    new-suite-id))

;; Baseline
(defn get-baseline-screenshot [conn suite-id screenshot-name {:keys [os browser resolution]}]
  (query-single conn
    ["SELECT screenshot.* FROM screenshot
                 JOIN baseline_screenshot ON screenshot.id = baseline_screenshot.screenshot_id
                 JOIN baseline ON baseline_screenshot.baseline_id = baseline.id
                 JOIN suite ON baseline.suite_id = suite.id
                 WHERE suite.id = ? AND screenshot.screenshot_name = ?
                 AND screenshot.os = ? AND screenshot.resolution = ?
                 AND screenshot.browser = ?" suite-id screenshot-name os resolution browser]))

(defn get-baseline [conn suite-id]
  (query-single conn ["SELECT * FROM baseline WHERE suite_id = ?" suite-id]))

(defn create-baseline-screenshot!
  "Adds the given screenshot-id to the given baseline."
  [conn baseline-id screenshot-id]
  (insert-single! conn :baseline-screenshot {:baseline-id   baseline-id
                                             :screenshot-id screenshot-id}))

(defn set-baseline! [conn diff-id screenshot-id new-screenshot-id]
  (first
    (update! conn :baseline-screenshot {:screenshot-id new-screenshot-id}
             ["screenshot_id = ? AND baseline_id =
              (SELECT analysis.baseline_id FROM diff
              JOIN analysis ON analysis.id = diff.analysis_id
              WHERE diff.id = ?)" screenshot-id diff-id])))

;; Analysis
(defn- create-analysis! [conn baseline-id run-id]
  "Returns the generated analysis id"
  (insert-single! conn :analysis {:baseline-id baseline-id :run-id run-id}))

(defn get-analysis
  [conn run-id]
  (query-single conn
    ["SELECT analysis.*, project.id project_id, project.name project_name, suite.id suite_id, suite.name suite_name
     FROM analysis
     JOIN run ON run.id = analysis.run_id
     JOIN suite ON suite.id = run.suite_id
     JOIN project ON project.id = suite.project_id
     WHERE run_id = ?" run-id]
    :row-fn format-dates))

(defn get-full-analysis [conn run-id]
  (let [analysis (get-analysis conn run-id)
        diffs (query conn
                ["SELECT diff.*, diff_image.path,
                 sbefore.size before_size,
                 sbefore.resolution before_resolution,
                 sbefore.os before_os,
                 sbefore.browser before_browser,
                 sbefore.version before_version,
                 sbefore.screenshot_name before_name,
                 sbefore.path before_path,
                 safter.size after_size,
                 safter.resolution after_resolution,
                 safter.browser after_browser,
                 safter.version after_version,
                 safter.os after_os,
                 safter.screenshot_name after_name,
                 safter.path after_path FROM analysis
                 JOIN diff ON diff.analysis_id = analysis.id
                 JOIN diff_image ON diff.diff_image = diff_image.id
                 JOIN screenshot safter ON safter.id = diff.after
                 JOIN screenshot sbefore ON sbefore.id = diff.before
                 WHERE analysis.run_id = ?" run-id]
                :row-fn format-dates
                :result-set-fn vec)]
    {:analysis analysis :diffs diffs}))

;; Screenshots
(defn save-screenshot!
  "Stores metadata of a new screenshot. Returns the new screenshot id."
  [conn run-id screenshot-name path meta]
  (try
    (insert-single! conn :screenshot (merge {:run-id          run-id
                                             :screenshot-name screenshot-name
                                             :path            path}
                                            meta))
    (catch SQLException e
      (when (unique-constraint-violation? e)
        (ex/throw+ {:type    :sql-exception
                    :subtype ::unique-constraint-violation
                    :message (.getMessage e)})))))

(defn get-screenshot-by-id [conn screenshot-id]
  (query-single conn ["SELECT * FROM screenshot WHERE id = ?" screenshot-id] :result-set-fn vec))

(defn get-screenshots [conn run-id]
  (query conn ["SELECT * FROM screenshot WHERE run_id = ?" run-id] :result-set-fn vec))

;; Runs
(defn create-run!
  "Creates a run for the given project and suite names.
  If the suite does not yet exist it will be created along with a new baseline.
  Creating a run also creates an analysis for the run, this may change in the future.
  Returns the created run id."
  [conn {:keys [project-name suite-name]}]
  (let [suite-id (or (get-suite-by-name conn project-name suite-name :id)
                     (create-suite-for-project! conn project-name suite-name))
        baseline (get-baseline conn suite-id)
        new-run-id (insert-single! conn :run {:suite-id   suite-id
                                              :start-time (Timestamp. (.getTime (Date.)))
                                              :status     "running"})
        _ (create-analysis! conn (:id baseline) new-run-id)]
    new-run-id))

(defn get-run
  "Returns the data for the given run-id"
  [conn run-id]
  (query-single conn
    ["SELECT run.*, project.id project_id FROM run
          JOIN suite ON run.suite_id = suite.id JOIN project ON suite.project_id = project.id
          WHERE project.id = suite.project_id AND suite.id = run.suite_id AND run.id = ?" run-id]
    :row-fn format-dates))

(def ^:private get-suite-runs-sql
  "SELECT run.* FROM run JOIN suite ON suite.id = run.suite_id
  JOIN project ON suite.project_id = project.id
  WHERE suite.id = ? AND project.id = ?
  ORDER BY run.start_time DESC")
(defn get-runs
  "Returns the list of runs for the given suite"
  [conn project-id suite-id]
  (query conn [get-suite-runs-sql suite-id project-id]
         :row-fn (comp #(assoc % :project-id project-id) format-dates)
         :result-set-fn vec))

;; Suites
(defn get-suite
  "Returns the suite with its list of runs"
  [conn project-id suite-id]
  (when-let [suite (get-suite-by-id conn project-id suite-id #(dissoc % :project-id))]
    (assoc suite :runs (get-runs conn project-id suite-id)
                 :project (get-project-by-id conn project-id))))

(def ^:private get-suites-sql "SELECT suite.id, suite.name FROM suite JOIN project ON project.id = suite.project_id WHERE project.id = ?")
(defn get-suites
  "Returns the list of suites"
  [conn project-id]
  (query conn [get-suites-sql project-id] :result-set-fn vec))

;; Projects
(defn get-projects
  "Retrieve the list of projects"
  [conn]
  (query conn ["SELECT * FROM project"] :result-set-fn vec))

(defn get-project
  "Returns the project with list of suites"
  [conn project-id]
  (when-let [pname (get-project-by-id conn project-id :name)]
    {:id     project-id
     :name   pname
     :suites (get-suites conn project-id)}))

(defn create-project!
  "Creates a new project. Returns the new project id."
  [conn project-name]
  (insert-single! conn :project {:name project-name}))

;; Diff
(defn store-diff-image! [conn path]
  (insert-single! conn :diff-image {:path path}))

(defn save-diff!
  "Stores a new diff. Returns the new diff's id."
  [conn path before after percentage analysis-id]
  (let [diff-image-id (store-diff-image! conn path)]
    (insert-single! conn :diff {:before      before
                                :after       after
                                :percentage  percentage
                                :status      "pending"
                                :analysis-id analysis-id
                                :diff-image  diff-image-id})))

(defn get-diff [conn run-id diff-id]
  (query-single conn
    ["SELECT diff.* FROM diff
      JOIN analysis ON analysis.id = diff.analysis_id
      JOIN run ON run.id = analysis.run_id
      WHERE run.id = ? AND diff.id = ?" run-id diff-id]))

(defn update-diff-status! [conn diff-id status]
  (update! conn :diff {:status status} ["id = ?" diff-id]))
