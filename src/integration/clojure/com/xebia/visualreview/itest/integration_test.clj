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

(def screenshot-properties {:os         "LINUX"
                            :browser    "firefox"
                            :resolution "1024x786"
                            :version    "40.0"})



(use-fixtures :each mock/logging-fixture mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture mock/test-server-fixture)

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
        (is (re-find #"can not be empty" (:body response)) "Return error message"))))

  (testing "Deleting projects"
    (testing "Project can be deleted and returns true"
      (let [created-project (:body (api/put-project! {:name "my soon-to-be-deleted project"}))
            project-before-deletion (:body (api/get-project (:id created-project)))
            response-status (:status (api/delete-project! (:id project-before-deletion)))
            project-after-deletion-status (:status (api/get-project (:id project-before-deletion)))]
        (is (not (nil? project-before-deletion)))
        (is (= response-status 204))
        (is (= project-after-deletion-status 404)))))

  (testing "Deleting runs"
    (testing "Run can be deleted but leaves other runs and the baseline intact"
      ; test for regression of issue #49:
      ;   deletion of a run which contains images that were added to the baseline causes links to these baseline images
      ;   in other runs to be removed as well
      (let [run-1 (:body (api/post-run! project-name-1 suite-name))
            run-1-screenshot (:body (mock/upload-tapir (:id run-1) {} screenshot-properties))
            status-update (api/update-diff-status-of-screenshot (:id run-1) (:id run-1-screenshot) "accepted")
            run-2 (:body (api/post-run! project-name-1 suite-name))
            run-2-screenshot (:body (mock/upload-tapir (:id run-2) {} screenshot-properties))
            run-1-analysis-before-deletion (api/get-analysis (:id run-1))
            run-2-analysis-before-deletion (api/get-analysis (:id run-2))
            run-1-deletion (api/delete-run! (:id run-1))
            run-1-after-deletion (api/get-run (:id run-1))
            run-2-after-deletion (api/get-run (:id run-2))
            run-1-analysis-after-deletion (api/get-analysis (:id run-1))
            run-2-analysis-after-deletion (api/get-analysis (:id run-2))
            run-1-screenshot-after-deletion (api/get-image (:imageId run-1-screenshot))
            run-2-screenshot-after-deletion (api/get-image (:imageId run-2-screenshot))]
        ; sanity checks
        (is (= (not (nil? (:id run-1)))))
        (is (= (not (nil? (:id run-2)))))
        (is (= (not (nil? (:id run-1-analysis-before-deletion)))))
        (is (= (not (nil? (:id run-2-analysis-before-deletion)))))
        (is (= (:status status-update) 201))
        (is (= (not (nil? (:id run-1-screenshot)))))
        (is (= (not (nil? (:id run-2-screenshot)))))
        ; is run-1 properly deleted?
        (is (= (:status run-1-deletion) 204))
        (is (= (:status run-1-after-deletion) 404))
        (is (= (:status run-1-analysis-after-deletion) 404))
        ; is run-2 still available and is all the data for it still there?
        (is (:status run-2-after-deletion) 200)
        (is (= (:body run-2-after-deletion) run-2))
        (is (= (:body run-2-analysis-before-deletion) (:body run-2-analysis-after-deletion)))

        (is (= (:status run-1-screenshot-after-deletion) 200)) ; still there because of baseline
        (is (= (:status run-2-screenshot-after-deletion) 200)) ; still there because its run stil exists
        (is (= (not (nil? (:body run-1-screenshot-after-deletion)))))
        (is (= (not (nil? (:body run-2-screenshot-after-deletion)))))))))

