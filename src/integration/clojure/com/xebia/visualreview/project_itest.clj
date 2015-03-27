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

(ns com.xebia.visualreview.project-itest
  (:require [com.xebia.visualreview.mock :as mock]
            [taoensso.timbre :as timbre]
            [com.xebia.visualreview.project :as project]
            [com.xebia.visualreview.persistence :as p]
            [midje.sweet :refer :all]))

(timbre/set-level! :error)

(background
  (before :facts (mock/setup-db))
  (around :facts (mock/rebind-db-spec ?form)))

(facts "Project service"
  (fact "stores and retrieves a project by name"
      (let [project-id (project/create-project! mock/*conn* "some name")
            project-by-name (project/get-project-by-name mock/*conn* "some name")
            project-by-id (project/get-project-by-id mock/*conn* project-id)
            expected-project {:id 1 :name "some name"}]
        project-id => 1
        project-by-name => expected-project
        project-by-id => expected-project))

   (fact "retrieves a list of projects"
         (do
           (project/create-project! mock/*conn* "some name")
           (project/create-project! mock/*conn* "some other name")
           (project/create-project! mock/*conn* "yet another name"))
           (let [projects (project/get-projects mock/*conn*)]
             (count projects) => 3
             (nth projects 0) => {:id 1 :name "some name"}
             (nth projects 1) => {:id 2 :name "some other name"}
             (nth projects 2) => {:id 3 :name "yet another name"}))

  (fact "deletes projects and attached suites"
        (let [project-id-1 (project/create-project! mock/*conn* "project name")
              project-id-2 (project/create-project! mock/*conn* "project name 2")
              created-project (project/get-project-by-id mock/*conn* project-id-1)
              suite-id-1 (p/create-suite-for-project! mock/*conn* "project name" "suite name")
              suite-id-2 (p/create-suite-for-project! mock/*conn* "project name 2" "suite name")
              suite (p/get-suite-by-id mock/*conn* project-id-1 suite-id-1)
              deleted-project (project/delete-project! mock/*conn* project-id-1)
              project-1-after-deletion (project/get-project-by-id mock/*conn* project-id-1)
              project-2-after-deletion (project/get-project-by-id mock/*conn* project-id-2)
              suite-1-after-deletion (p/get-suite-by-id mock/*conn* project-id-1 suite-id-1)
              suite-2-after-deletion (p/get-suite-by-id mock/*conn* project-id-2 suite-id-2)]
          deleted-project => true
          (nil? created-project) => false
          (nil? suite) => false
          (nil? project-1-after-deletion) => true
          (nil? project-2-after-deletion) => false
          (nil? suite-1-after-deletion) => true
          (nil? suite-2-after-deletion) => false))); tests if cascade deletes don't delete too much

