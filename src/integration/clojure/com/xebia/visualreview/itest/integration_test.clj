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

(ns com.xebia.visualreview.itest.integration-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.itest-util :refer [start-server stop-server]]))


(def project-name-1 "A Test Project")
(def project-name-2 "Another Project")
(def suite-name "Test suite")

(use-fixtures :each mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture mock/test-server-fixture)

(deftest projects

  (testing "Initial state"
    (is (= (:body (api/get-projects)) []) "Initially there are no projects"))

  (testing "Creating projects"

    (testing "Request validation"
      (let [response (api/put-project! {})]
        (is (re-find #"'name' is missing" (:body response)) "The request parameters are validated")
        (is (= 422 (:status response)) "Unprocessable entity is returned for invalid input")))

    (testing "Two valid projects"
      (let [response1 (api/put-project! {:name project-name-1})
            response2 (api/put-project! {:name project-name-2})]
        (is (= (:status response1) 201) "Create project 1")
        (is (= (:status response2) 201) "Create project 2")
        (is (= {:id 1 :name project-name-1 :suites []} (:body response1)))
        (is (= (:body (api/get-projects)) [{:id 1 :name project-name-1} {:id 2 :name project-name-2}]))))

    (testing "Single project endpoint"
      (is (= {:id 1 :name project-name-1 :suites []} (:body (api/get-project 1)))))

    (testing "Project names are unique"
      (let [response (api/put-project! {:name project-name-1})]
        (is (= 409 (:status response)) "Conflict status code")
        (is (re-find #"already exists" (:body response)) "Return error message")))

    (testing "Project name can not be empty"
      (let [response (api/put-project! {:name ""})]
        (is (= 422 (:status response)) "Unprocessable entity status code")
        (is (re-find #"can not be empty" (:body response)) "Return error message")))))