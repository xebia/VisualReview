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

(ns com.xebia.visualreview.itest.suite-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.service.project :as project]
            [com.xebia.visualreview.service.suite :as suite]
            [com.xebia.visualreview.service.run :as run]))

(use-fixtures :each mock/logging-fixture mock/rebind-db-spec-fixture mock/setup-db-fixture)

(deftest project-service-store
  (testing "stores and retrieves a suite"
    (let [project-id (project/create-project! mock/*conn* "some project")
          suite-id (suite/create-suite-for-project! mock/*conn* "some project" "some suite")
          suite-by-id (suite/get-suite-by-id mock/*conn* suite-id)
          suite-by-name (suite/get-suite-by-name mock/*conn* "some project" "some suite")
          expected-suite {:name "some suite" :project-id project-id :id suite-id}]
      (is (= suite-id 1))
      (is (= suite-by-id expected-suite))
      (is (= suite-by-name expected-suite)))))


(deftest suite-service-list
  (testing "retrieves a list of suites"
    (let [project-id (project/create-project! mock/*conn* "some project")
          suite-id-1 (suite/create-suite-for-project! mock/*conn* "some project" "some suite 1")
          suite-id-2 (suite/create-suite-for-project! mock/*conn* "some project" "some suite 2")
          suite-id-3 (suite/create-suite-for-project! mock/*conn* "some project" "some suite 3")
          suites (suite/get-suites mock/*conn* project-id)]
      (is (= (count suites) 3))
      (is (= ((nth suites 0) {:id suite-id-1 :name "some suite 1"})))
      (is (= (nth suites 1) {:id suite-id-2 :name "some suite 2"}))
      (is (= (nth suites 2) {:id suite-id-3 :name "some suite 3"})))))

(deftest suite-service-full-suite
  (testing "retrieves the full data of a suite, including run data"
    (let  [project-id (project/create-project! mock/*conn* "some project")
           project (project/get-project-by-id mock/*conn* project-id)
           suite-id (suite/create-suite-for-project! mock/*conn* "some project" "some suite")
           run-1 (run/get-run mock/*conn* (run/create-run! mock/*conn* suite-id))
           run-2 (run/get-run mock/*conn* (run/create-run! mock/*conn* suite-id))
           full-suite (suite/get-full-suite mock/*conn* project-id suite-id)]
      (is (= full-suite {:name "some suite" :project project :id suite-id :runs [run-2 run-1]})))))


(deftest suite-service-delete
  (testing "deletes suites and attached runs"
    (let [project-id (project/create-project! mock/*conn* "project name")
          created-project (project/get-project-by-id mock/*conn* project-id)
          suite-id-1 (suite/create-suite-for-project! mock/*conn* "project name" "suite name")
          suite-1 (suite/get-suite-by-id mock/*conn* suite-id-1)
          run-1 (run/get-run mock/*conn* (run/create-run! mock/*conn* suite-id-1))
          suite-id-2 (suite/create-suite-for-project! mock/*conn* "project name" "suite name 2")
          suite-2 (suite/get-suite-by-id mock/*conn* suite-id-2)
          run-2 (run/get-run mock/*conn* (run/create-run! mock/*conn* suite-id-2))
          deleted-suite (suite/delete-suite! mock/*conn* suite-id-1)

          suite-1-after-deletion (suite/get-suite-by-id mock/*conn* suite-id-1)
          suite-2-after-deletion (suite/get-suite-by-id mock/*conn* suite-id-2)
          run-1-after-deletion (run/get-run mock/*conn* (:id run-1))
          run-2-after-deletion (run/get-run mock/*conn* (:id run-2))]
      (is (= deleted-suite true))
      ; sanity checks
      (is (not (nil? created-project)))
      (is (not (nil? suite-1)))
      (is (not (nil? suite-2)))

      (is (nil? suite-1-after-deletion))
      (is (not (nil? suite-2-after-deletion)))
      (is (nil? run-1-after-deletion))
      (is (not (nil? run-2-after-deletion))))))           ; tests if cascade deletes don't delete too much
