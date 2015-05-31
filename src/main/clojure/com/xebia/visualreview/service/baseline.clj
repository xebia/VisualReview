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

(ns com.xebia.visualreview.service.baseline
  (:require [com.xebia.visualreview.service.persistence.util :as putil]
            [slingshot.slingshot :as ex]
            [clojure.java.jdbc :as j]
            [cheshire.core :as json])
  (:import (java.sql SQLException)))

;; Baseline
(defn get-baseline-screenshot [conn suite-id branch-name screenshot-name properties]
  (putil/query-single conn
                      ["SELECT screenshot.* FROM baseline_tree tr
     JOIN baseline_branch br ON br.baseline_tree = tr.id
     JOIN bl_node_screenshot bl_ss ON bl_ss.baseline_node = br.head
     JOIN screenshot ON screenshot.id = bl_ss.screenshot_id
     WHERE tr.suite_id = ? AND br.name = ? AND screenshot.screenshot_name = ?
     AND screenshot.properties = ?" suite-id branch-name screenshot-name (json/generate-string properties)]))

(defn get-baseline-screenshot-by-diff-id [conn diff-id]
  (putil/query-single conn
                      ["SELECT screenshot.* FROM diff
     JOIN screenshot ON screenshot.id = diff.before
     WHERE diff.id = ?" diff-id]))

(defn get-baseline-head
  ([conn suite-id] (get-baseline-head conn suite-id "master"))
  ([conn suite-id branch-name]
   (putil/query-single conn ["SELECT br.head FROM suite
   JOIN baseline_tree t ON suite.id = t.suite_id
   JOIN baseline_branch br ON t.baseline_root = br.head
   WHERE suite_id = ? AND br.name = ?" suite-id branch-name]
                       :row-fn :head)))

(defn get-baseline-node
  [conn node-id]
  (putil/query-single conn ["SELECT * FROM baseline_node WHERE id = ?" node-id]))

(defn create-bl-node-screenshot!
  [conn node-id screenshot-id]
  (putil/insert-single! conn :bl-node-screenshot {:baseline-node node-id :screenshot-id screenshot-id}))

(defn delete-bl-node-screenshot!
  [conn node-id screenshot-id]
  (putil/delete! conn :bl-node-screenshot ["baseline_node = ? AND screenshot_id = ?" node-id screenshot-id]))

(defn get-bl-node-screenshot
  [conn node-id screenshot-id]
  (putil/query-single conn ["SELECT * FROM bl_node_screenshot WHERE baseline_node = ? AND screenshot_id = ?" node-id screenshot-id]))

(defn set-baseline! [conn diff-id screenshot-id new-screenshot-id]
  (first
    (putil/update! conn :bl-node-screenshot {:screenshot-id new-screenshot-id}
                   ["screenshot_id = ? AND baseline_node =
              (SELECT analysis.baseline_node FROM diff
              JOIN analysis ON analysis.id = diff.analysis_id
              WHERE diff.id = ?)" screenshot-id diff-id])))


(defn get-baseline-tree
  [conn suite-id]
  (putil/query-single conn ["SELECT * FROM baseline_tree WHERE suite_id = ?" suite-id]))

(defn create-baseline-tree!
  "Creates a new baseline tree for the given suite.
  The baseline will be empty (no screenshots). Returns the generated root-id"
  [conn suite-id]
  (let [root-id (putil/insert-single! conn :baseline-node {})
        tree-id (putil/insert-single! conn :baseline-tree {:suite-id suite-id :baseline-root root-id})]
    (putil/insert-single! conn :baseline-branch {:baseline-tree tree-id
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
  (putil/query-single conn
                      [(node-root-sql node-id "SELECT bl.* FROM T JOIN baseline_tree bl ON bl.baseline_root = T.id WHERE parent IS ?") nil]))

(defn- copy-baseline-refs [conn from-id to-id]
  (j/db-do-prepared conn
                    "INSERT INTO BL_NODE_SCREENSHOT SELECT ?, SCREENSHOT_ID FROM BL_NODE_SCREENSHOT WHERE baseline_node = ?"
                    [to-id from-id]))

(defn- create-baseline-child! [conn tree-id branch-name]
  {:pre [(number? tree-id) (string? branch-name)]}
  (when-let [parent-id (putil/query-single conn ["SELECT * FROM baseline_branch WHERE baseline_tree = ? AND name = ?" tree-id branch-name]
                                           :row-fn :head)]
    (let [child-id (putil/insert-single! conn :baseline-node {:parent parent-id})]
      (copy-baseline-refs conn parent-id child-id)
      (putil/update! conn :baseline-branch {:head child-id} ["head = ?" parent-id]))))

(defn create-baseline-branch! [conn parent-id branch-name]
  {:pre [(number? parent-id) (string? branch-name)]}
  (try
    (let [tree-id (:id (get-tree-for-node conn parent-id))
          child-id (putil/insert-single! conn :baseline-node {:parent parent-id})]
      (copy-baseline-refs conn parent-id child-id)
      (putil/insert-single! conn :baseline-branch {:name          branch-name
                                                   :baseline-tree tree-id
                                                   :head          child-id
                                                   :branch-root   child-id}))
    (catch SQLException e
      (when (putil/unique-constraint-violation? e)
        (ex/throw+ {:type    :sql-exception
                    :subtype ::unique-constraint-violation
                    :message (.getMessage e)})))))

(defn get-baseline-branch [conn suite-id branch-name]
  {:pre [(number? suite-id) (string? branch-name)]}
  (putil/query-single conn
                      ["SELECT br.* FROM baseline_branch br
     JOIN baseline_tree tr ON tr.id = br.baseline_tree
     WHERE tr.suite_id = ? AND br.name = ?" suite-id branch-name]))
