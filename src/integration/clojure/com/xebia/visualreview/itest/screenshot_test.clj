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

(ns com.xebia.visualreview.itest.screenshot-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.service.project :as project]
            [com.xebia.visualreview.service.screenshot :as s]
            [com.xebia.visualreview.service.suite :as suite]
            [com.xebia.visualreview.service.run :as run]
            [com.xebia.visualreview.api-test :as api]))

(use-fixtures :each mock/logging-fixture mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture mock/test-server-fixture)


(deftest screenshot-service

  (testing "Storing and retrieving a screenshot based on a screenshot-id"
    (project/create-project! mock/*conn* "myProject")
    (let [suite-id (suite/create-suite-for-project! mock/*conn* "myProject" "mySuite")
          run-id (run/create-run! mock/*conn* suite-id)
          image-file (io/as-file (io/resource "tapir_hat.png"))
          screenshot-id (s/insert-screenshot! mock/*conn* run-id "myScreenshot" {:browser "chrome" :os "windows"} {:version "4.0"} image-file)
          screenshot (s/get-screenshot-by-id mock/*conn* screenshot-id)]
      (is (= 1 screenshot-id) "The first screenshot-id is 1")
      (is (= {:id              screenshot-id
              :image-id        1
              :meta            {:version "4.0"}
              :properties      {:browser "chrome" :os "windows"}
              :screenshot-name "myScreenshot"
              :size            (.length image-file)} screenshot))))

  (testing "Deleting unused screenshots"
    (testing "should only delete screenshots that were part of a deleted run and not part of a baseline")
    (let [project-name "myProject2" suite-name "mySuite"
          _ (api/put-project! {:name project-name})
          run1-id (-> (api/post-run! project-name suite-name) :body :id)
          run1-screenshot1 (-> (mock/upload-tapir run1-id {:browser "chrome" :os "windows"} {:version "4.0"}) :body)
          run1-screenshot2 (-> (mock/upload-tapir-hat run1-id {:browser "chrome" :os "windows"} {:version "4.0"}) :body)

          ; makes screenshot1 part of this suite's baseline
          diff-update-result (api/update-diff-status-of-screenshot run1-id (:imageId run1-screenshot1) "accepted")

          delete-run-result (api/delete-run! run1-id)

          unused-screenshot-ids (s/get-unused-screenshot-ids mock/*conn*)
          _ (s/delete-unused-screenshots! mock/*conn*)
          unused-screenshot-ids-after-deletion (s/get-unused-screenshot-ids mock/*conn*)
          deleted-run-id-shots (-> (s/get-screenshots-by-run-id mock/*conn* run1-id) :body)

          ; do another run to see if the baseline is still intact
          run2-id (-> (api/post-run! project-name suite-name) :body :id)
          _ (-> (mock/upload-tapir run2-id {:browser "chrome" :os "windows"} {:version "4.0"}) :body)
          run2-analysis-diffs (-> (api/get-analysis run2-id) :body :diffs)
          ]

      ; sanity checks
      (is (not (nil? (:id run1-screenshot1))))
      (is (not (nil? (:id run1-screenshot2))))
      (is (= 204 (:status delete-run-result)))
      (is (= 201 (:status diff-update-result)))

      ; should only contain run1-screenshot2 as it's not being used in a run *and* a baseline
      (is (= 1 (count unused-screenshot-ids)))
      (is (= [(:id run1-screenshot2)] unused-screenshot-ids))
      (is (nil? deleted-run-id-shots))
      (is (= 0 (count unused-screenshot-ids-after-deletion)))

      ; test if the deletion really did leave the first screenshot intact
      ; for it to be used in the second run as a 'before' image
      (is (= 1 (count run2-analysis-diffs)))
      (is (= (:imageId run1-screenshot1) (-> (first run2-analysis-diffs) :before :imageId)))
      )))
