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

(ns com.xebia.visualreview.itest.upload-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.itest-util :refer [start-server stop-server]]))

(def project-name-1 "Test Project A")
(def project-name-2 "Test Project B")
(def suite-name "Test suite")
(def properties {:os         "LINUX"
                 :browser    "firefox"
                 :resolution "1024x786"
                 :version    "31.4.0"})

(def meta-info {:takenBy   "Daniel"
                :timeStamp "2015-02-19T16:34:12"})

(defn setup-projects []
  (api/put-project! {:name project-name-1})
  (api/put-project! {:name project-name-2}))

(use-fixtures :each mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture mock/test-server-fixture)

(deftest screenshots

  (setup-projects)

  (testing "There are two test projects"
    (let [projects (:body (api/get-projects))]
      (is (= [{:id 1 :name project-name-1} {:id 2 :name project-name-2}] projects))))

  (testing "There are no runs or screenshots yet"
    (is (= 404 (:status (api/get-runs project-name-1 suite-name))) "Not found status"))

  (let [run-id (-> (api/post-run! project-name-1 suite-name) :body :id)]
    (testing "We can upload screenshots"
      (let [response (mock/upload-tapir run-id meta-info properties)]
        (is (= 201 (:status response)))
        (are [k v] (= (-> response :body k) v)
          :id 1
          :imageId 1
          :runId run-id
          :screenshotName "Tapir"
          :size 38116
          :meta meta-info
          :properties properties)))

    (testing "Uploading duplicate screenshots"
      (is (= 201 (:status (mock/upload-chess-image-1 run-id meta-info properties))) "Created status")
      (let [second-request (mock/upload-chess-image-2 run-id meta-info properties)]
        (is (= 200 (:status second-request)) "Not created, but OK status")
        (is (re-find #"already uploaded" (-> second-request :body :error)) "Error message in response")
        (is (map? (-> second-request :body :conflictingEntity)))))

    (testing "Uploading screenshots with same name and meta, but different properties"
      (is (= 201 (:status (mock/upload-chess-image-2 run-id meta-info (assoc properties :resolution "800x600")))) "Created status")))

  (testing "Uploading duplicate screenshots in new run"
    (let [next-run-id (-> (api/post-run! project-name-1 suite-name) :body :id)
          response (mock/upload-chess-image-2 next-run-id meta-info properties)]
      (is (= 201 (:status response))))))
