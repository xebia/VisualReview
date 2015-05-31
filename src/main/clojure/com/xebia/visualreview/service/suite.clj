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

(ns com.xebia.visualreview.service.suite
  (:require [com.xebia.visualreview.service.persistence.util :as putil]
            [com.xebia.visualreview.service.project :as project]
            [com.xebia.visualreview.service.baseline :as baseline]
            [com.xebia.visualreview.service.run :as run]))

(defn get-suite-by-name
  ([conn project-name suite-name]
   (get-suite-by-name conn project-name suite-name identity))
  ([conn project-name suite-name row-fn]
   (putil/query-single conn
                       ["SELECT suite.* FROM suite JOIN project ON project_id = project.id WHERE project.name = ? AND suite.name = ?" project-name suite-name]
                       :row-fn row-fn)))

(defn get-suite-by-id
  ([conn suite-id]
   (get-suite-by-id conn suite-id identity))
  ([conn suite-id row-fn]
   (putil/query-single conn
                       ["SELECT suite.* FROM suite JOIN project ON suite.project_id = project.id WHERE suite.id = ?" suite-id]
                       :row-fn row-fn)))

(defn get-full-suite
  "Returns the suite with its list of runs"
  [conn project-id suite-id]
  (when-let [suite (get-suite-by-id conn suite-id #(dissoc % :project-id))]
    (assoc suite :runs (run/get-runs conn project-id suite-id)
                 :project (project/get-project-by-id conn project-id))))

(def ^:private get-suites-sql "SELECT suite.id, suite.name FROM suite JOIN project ON project.id = suite.project_id WHERE project.id = ?")
(defn get-suites
  "Returns the list of suites"
  [conn project-id]
  (putil/query conn [get-suites-sql project-id] :result-set-fn vec))

(defn get-suites-by-project-id
  "Returns all suites of a project"
  [conn project-id]
  {:pre (number? project-id)}
  (when-let [pname (project/get-project-by-id conn project-id :name)]
    {:id     project-id
     :name   pname
     :suites (get-suites conn project-id)}))

(defn create-suite-for-project!
  "Creates a new suite with an empty baseline for the given project. Returns the created suite's id."
  [conn project-name suite-name]
  (let [project-id (project/get-project-by-name conn project-name :id)
        new-suite-id (putil/insert-single! conn :suite {:project-id project-id :name suite-name})]
    (baseline/create-baseline-tree! conn new-suite-id)
    new-suite-id))
