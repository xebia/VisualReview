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

(ns com.xebia.visualreview.service.project
  (:require [com.xebia.visualreview.service.service-util :as sutil]
            [com.xebia.visualreview.service.persistence.util :as putil]
            [com.xebia.visualreview.service.project.persistence :as ppersistence]))

(defn get-project-by-name
  "Retrieves a project by project name. Returns nil when this project does not exist."
  ([conn project-name]
   (get-project-by-name conn project-name identity))
  ([conn project-name row-fn]
   {:pre (string? project-name)}
   (sutil/attempt
    (ppersistence/query-for-project conn "name" project-name row-fn)
    "Could not retrieve project: %s", ::retrieve-by-name-failed)))

(defn get-project-by-id
  "Retrieves a project by project id. Returns nil when this project does not exist."
  ([conn project-id]
   (get-project-by-id conn project-id identity))
  ([conn project-id row-fn]
   {:pre (number? project-id)}
   (sutil/attempt
     (ppersistence/query-for-project conn "id" project-id row-fn)
     "Could not retrieve project by id: %s"
     ::retrieve-by-id-failed)))

;; Projects
(defn get-projects
  "Retrieve the list of projects"
  [conn]
  (sutil/attempt
    (putil/query conn ["SELECT * FROM project"] :result-set-fn vec)
    "Could not retrieve a list of projects: %s"
    ::get-projects-failed))

(defn create-project!
  "Creates a new project. Returns the new project id."
  [conn project-name]
  {:pre (string? name)}
  (sutil/attempt
    (putil/insert-single! conn :project {:name project-name})
    "Could not create project: %s"
    ::create-project-failed))

(defn delete-project!
  "Deletes a project and all attached suites, runs, analyses and screenshot metadata.
  Images metadata and binary files are left intact.
  Returns true when deletion was succesful. "
  [conn project-id]
  {:pre (number? project-id)}
  (sutil/attempt
    (do (putil/delete! conn :project ["id = ?" project-id])
        true)
    "Could not delete project: %s"
    ::delete-by-id-failed))