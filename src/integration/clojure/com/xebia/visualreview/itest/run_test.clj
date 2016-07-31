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

(ns com.xebia.visualreview.itest.run-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.service.project :as project]
            [com.xebia.visualreview.service.suite :as suite]
            [com.xebia.visualreview.service.run :as run]))

(use-fixtures :each mock/logging-fixture mock/rebind-db-spec-fixture mock/setup-db-fixture)

(deftest run-service-store
  (testing "stores and retrieves a run"
    (let [project-id (project/create-project! mock/*conn* "some project")
          suite-id (suite/create-suite-for-project! mock/*conn* "some project" "some suite")
          run-id (run/create-run! mock/*conn* suite-id "master")
          run (run/get-run mock/*conn* run-id)]
      (is (= (:id run) run-id))
      (is (= (:project-id run) project-id))
      (is (= (:suite-id run) suite-id))
      (is (= (:status run) "running"))
      (is (= (:end-time run) nil))
      (is (not (nil? (:start-time run)))))))

(deftest run-service-list
  (testing "retrieves a list of runs"
    (let [project-id (project/create-project! mock/*conn* "some project")
          suite-id (suite/create-suite-for-project! mock/*conn* "some project" "some suite")
          run-1 (run/get-run mock/*conn* (run/create-run! mock/*conn* suite-id "master"))
          run-2 (run/get-run mock/*conn* (run/create-run! mock/*conn* suite-id "master"))
          run-3 (run/get-run mock/*conn* (run/create-run! mock/*conn* suite-id "master"))
          runs (run/get-runs mock/*conn* project-id suite-id)]
      (is (= (count runs) 3))
      (is (= (nth runs 0) run-3))
      (is (= (nth runs 1) run-2))
      (is (= (nth runs 2) run-1)))))

(deftest run-service-delete
  (testing "deletes runs"
    (let [project-id (project/create-project! mock/*conn* "project name")
          created-project (project/get-project-by-id mock/*conn* project-id)
          suite-id (suite/create-suite-for-project! mock/*conn* "project name" "suite name")
          suite (suite/get-suite-by-id mock/*conn* suite-id)
          run-id-1 (run/create-run! mock/*conn* suite-id "master")
          run-1 (run/get-run mock/*conn* run-id-1)
          run-id-2(run/create-run! mock/*conn* suite-id "master")
          run-2 (run/get-run mock/*conn* run-id-2)
          deleted-run (run/delete-run! mock/*conn* run-id-1)

          run-1-after-deletion (run/get-run mock/*conn* run-id-1)
          run-2-after-deletion (run/get-run mock/*conn* run-id-2)
          suite-after-deletion (suite/get-suite-by-id mock/*conn* suite-id)]
      (is (= deleted-run true))
      ; sanity checks
      (is (not (nil? created-project)))
      (is (not (nil? suite)))
      (is (not (nil? run-1)))
      (is (not (nil? run-2)))

      (is (nil? run-1-after-deletion))
      (is (not (nil? run-2-after-deletion)))
      (is (not (nil? suite-after-deletion))))))           ; tests if cascade deletes don't delete too much

