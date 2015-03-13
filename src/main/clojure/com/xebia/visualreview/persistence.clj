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
            [slingshot.slingshot :as ex]
            [cheshire.core :as json]
            [com.xebia.visualreview.persistence.database :as db])
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

(defn- parse-json-fields [& ks]
  (fn [screenshot-row]
    (reduce #(update-in %1 [%2] json/parse-string true) screenshot-row ks)))

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
(declare create-baseline-tree!)
(defn create-suite-for-project!
  "Creates a new suite with an empty baseline for the given project. Returns the created suite's id."
  [conn project-name suite-name]
  (let [project-id (get-project-by-name conn project-name :id)
        new-suite-id (insert-single! conn :suite {:project-id project-id :name suite-name})]
    (create-baseline-tree! conn new-suite-id)
    new-suite-id))

;; Baseline
(defn get-baseline-screenshot [conn suite-id branch-name screenshot-name properties]
  (query-single conn
    ["SELECT screenshot.* FROM baseline_tree tr
     JOIN baseline_branch br ON br.baseline_tree = tr.id
     JOIN bl_node_screenshot bl_ss ON bl_ss.baseline_node = br.head
     JOIN screenshot ON screenshot.id = bl_ss.screenshot_id
     WHERE tr.suite_id = ? AND br.name = ? AND screenshot.screenshot_name = ?
     AND screenshot.properties = ?" suite-id branch-name screenshot-name (json/generate-string properties)]))

(defn get-baseline-head
  ([conn suite-id] (get-baseline-head conn suite-id "master"))
  ([conn suite-id branch-name]
   (query-single conn ["SELECT br.head FROM suite
   JOIN baseline_tree t ON suite.id = t.suite_id
   JOIN baseline_branch br ON t.baseline_root = br.head
   WHERE suite_id = ? AND br.name = ?" suite-id branch-name]
     :row-fn :head)))

(defn create-baseline-screenshot!
  "Adds the given screenshot-id to the given baseline."
  [conn baseline-node screenshot-id]
  (insert-single! conn :bl-node-screenshot {:baseline-node baseline-node
                                            :screenshot-id screenshot-id}))

(defn create-bl-node-screenshots!
  [conn node-id screenshot-id]
  (insert-single! conn :bl-node-screenshot {:baseline-node node-id :screenshot-id screenshot-id}))

(defn set-baseline! [conn diff-id screenshot-id new-screenshot-id]
  (first
    (update! conn :bl-node-screenshot {:screenshot-id new-screenshot-id}
             ["screenshot_id = ? AND baseline_node =
              (SELECT analysis.baseline_node FROM diff
              JOIN analysis ON analysis.id = diff.analysis_id
              WHERE diff.id = ?)" screenshot-id diff-id])))

;; Analysis
(defn- create-analysis! [conn baseline-node run-id]
  "Returns the generated analysis id"
  (insert-single! conn :analysis {:baseline-node baseline-node :run-id run-id}))

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
                     sbefore.meta before_meta,
                     sbefore.properties before_properties,
                     sbefore.screenshot_name before_name,
                     sbefore.path before_path,
                     safter.size after_size,
                     safter.meta after_meta,
                     safter.properties after_properties,
                     safter.screenshot_name after_name,
                     safter.path after_path FROM analysis
                     JOIN diff ON diff.analysis_id = analysis.id
                     JOIN diff_image ON diff.diff_image = diff_image.id
                     JOIN screenshot safter ON safter.id = diff.after
                     JOIN screenshot sbefore ON sbefore.id = diff.before
                     WHERE analysis.run_id = ?" run-id]
                     :row-fn (comp (parse-json-fields :before-meta :before-properties :after-meta :after-properties) format-dates)
                     :result-set-fn vec)]
    {:analysis analysis :diffs diffs}))

;; Screenshots
(defn save-screenshot!
  "Stores a reference with data of a new screenshot. Returns the new screenshot id."
  [conn run-id screenshot-name path size properties meta]
  (try
    (insert-single! conn :screenshot {:run-id          run-id
                                      :screenshot-name screenshot-name
                                      :path            path
                                      :size            size
                                      :meta            (json/generate-string meta)
                                      :properties      (json/generate-string properties)})
    (catch SQLException e
      (when (unique-constraint-violation? e)
        (ex/throw+ {:type    :sql-exception
                    :subtype ::unique-constraint-violation
                    :message (.getMessage e)})))))

(defn get-screenshot-by-id [conn screenshot-id]
  (query-single conn ["SELECT * FROM screenshot WHERE id = ?" screenshot-id]
    :row-fn (parse-json-fields :meta :properties)
    :result-set-fn vec))

(defn get-screenshots [conn run-id]
  (query conn ["SELECT * FROM screenshot WHERE run_id = ?" run-id]
         :row-fn (parse-json-fields :meta :properties)
         :result-set-fn vec))

;; Runs
(defn create-run!
  "Creates a run for the given project and suite names.
  If the suite does not yet exist it will be created along with a new baseline.
  Creating a run also creates an analysis for the run, this may change in the future.
  Returns the created run id."
  [conn {:keys [project-name suite-name branch-name] :or {branch-name "master"}}]
  (let [suite-id (or (get-suite-by-name conn project-name suite-name :id)
                     (create-suite-for-project! conn project-name suite-name))
        baseline (get-baseline-head conn suite-id branch-name)
        new-run-id (insert-single! conn :run {:suite-id   suite-id
                                              :start-time (Timestamp. (.getTime (Date.)))
                                              :status     "running"})
        _ (create-analysis! conn baseline new-run-id)]
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
(defn get-full-suite
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

(defn get-baseline-tree
  [conn suite-id]
  (query-single conn ["SELECT * FROM baseline_tree WHERE suite_id = ?" suite-id]))

(defn create-baseline-tree!
  "Creates a new baseline tree for the given suite.
  The baseline will be empty (no screenshots). Returns the generated root-id"
  [conn suite-id]
  (let [root-id (insert-single! conn :baseline-node {})
        tree-id (insert-single! conn :baseline-tree {:suite-id suite-id :baseline-root root-id})]
    (insert-single! conn :baseline-branch {:baseline-tree tree-id
                                           :name          "master"
                                           :head          root-id
                                           :branch-root   root-id})
    root-id))

(defn- node-root-sql
  "Returns the sql for getting the root node of a tree given the node_id.
  This query is vendor specific and only works for H2"
  [node-id clauses]
  {:pre [(number? node-id)]}
  (str "WITH RECURSIVE T(id, parent) AS "
       "(SELECT id, parent FROM baseline_node WHERE id = " node-id
       " UNION ALL "
       "SELECT b.id, b.parent FROM T JOIN baseline_node b ON b.id = T.parent) "
       clauses))

(defn- get-tree-for-node [conn node-id]
  (query-single conn
    [(node-root-sql node-id "SELECT bl.* FROM T JOIN baseline_tree bl ON bl.baseline_root = T.id WHERE parent IS ?") nil]))

(defn- copy-baseline-refs [conn from-id to-id]
  (j/db-do-prepared conn
    "INSERT INTO BL_NODE_SCREENSHOT SELECT ?, SCREENSHOT_ID FROM BL_NODE_SCREENSHOT WHERE baseline_node = ?"
    [to-id from-id]))

(defn- create-baseline-child! [conn tree-id branch-name]
  {:pre [(number? tree-id) (string? branch-name)]}
  (when-let [parent-id (query-single conn ["SELECT * FROM baseline_branch WHERE baseline_tree = ? AND name = ?" tree-id branch-name]
                         :row-fn :head)]
    (let [child-id (insert-single! conn :baseline-node {:parent parent-id})]
      (copy-baseline-refs conn parent-id child-id)
      (update! conn :baseline-branch {:head child-id} ["head = ?" parent-id]))))

(defn create-baseline-branch! [conn parent-id branch-name]
  {:pre [(number? parent-id)]}
  (j/with-db-transaction [conn conn]
    (let [tree-id (:id (get-tree-for-node conn parent-id))
          child-id (insert-single! conn :baseline-node {:parent parent-id})]
      (copy-baseline-refs conn parent-id child-id)
      (insert-single! conn :baseline-branch {:name          branch-name
                                             :baseline-tree tree-id
                                             :head          child-id
                                             :branch-root   child-id}))))

(comment
  (get-baseline-tree db/conn 1)
  (do
    (create-project! db/conn "Test project")
    (create-suite-for-project! db/conn "Test project" "My suite")
    (create-run! db/conn {:project-name "Test project" :suite-name "My suite"})
    (dotimes [i 4]
      (let [ss-id (save-screenshot! db/conn 1 (str "Rocket-" i) "1/1/1" 2347 "{}" "{}")]
        (create-bl-node-screenshots! db/conn 1 ss-id)))
    )
  (create-baseline-child! db/conn 1 "master")
  (create-baseline-branch! db/conn 2 "storybranch")
  (create-baseline-child! db/conn 1 "storybranch")
  (query db/conn ["SELECT * FROM baseline_node"])
  (clojure.pprint/pprint (query db/conn ["SELECT * FROM bl_node_screenshot"]))
  (query db/conn ["SELECT * FROM baseline_tree"])
  (query db/conn ["SELECT * FROM baseline_branch"])

  (copy-baseline-refs db/conn 1 3)

  (do
    (j/execute! db/conn ["DROP ALL OBJECTS"])
    (db/run-init-script db/conn))
  )
