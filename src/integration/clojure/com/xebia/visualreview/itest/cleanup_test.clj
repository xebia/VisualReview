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

(ns com.xebia.visualreview.itest.cleanup-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.service.project :as project]
            [com.xebia.visualreview.service.screenshot :as s]
            [com.xebia.visualreview.service.suite :as suite]
            [com.xebia.visualreview.service.run :as run]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.service.image.persistence :as ip]
            [com.xebia.visualreview.service.image :as i]))

(use-fixtures :each mock/logging-fixture mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture mock/test-server-fixture)


(deftest screenshot-service

  (testing "Deleting unused screenshots and images"
    (testing "should delete screenshots and images that were not part of an existing run and/or baseline")
    (let [project-name "myProject2" suite-name "mySuite"
          _ (api/put-project! {:name project-name})
          run1-id (-> (api/post-run! project-name suite-name) :body :id)
          run1-screenshot1 (-> (mock/upload-tapir run1-id {:browser "chrome" :os "windows"} {:version "4.0"}) :body)
          run1-screenshot2 (-> (mock/upload-tapir-hat run1-id {:browser "chrome" :os "windows"} {:version "4.0"}) :body)

          ; makes screenshot1 part of this suite's baseline
          diff-update-result (api/update-diff-status-of-screenshot run1-id (:imageId run1-screenshot1) "accepted")
          run1-analysis (-> (api/get-analysis run1-id) :body)
          run1-analysis-diffs (:diffs run1-analysis)

          unconnected-image-id (i/insert-image! mock/*conn*  (io/as-file (io/resource "tapir.png")))

          unused-image-ids-before-run-deletion (ip/get-unused-image-ids mock/*conn*)
          unused-screenshot-ids-before-run-deletion (s/get-unused-screenshot-ids mock/*conn*)

          delete-run-result (api/delete-run! run1-id)

          unused-image-ids-after-run-deletion (ip/get-unused-image-ids mock/*conn*)
          unused-screenshot-ids-after-run-deletion (s/get-unused-screenshot-ids mock/*conn*)

          cleanup-result (api/perform-cleanup)

          unused-image-ids-after-cleanup (ip/get-unused-image-ids mock/*conn*)
          unused-screenshot-ids-after-cleanup (s/get-unused-screenshot-ids mock/*conn*)

          ; do another run to see if the baseline is still intact
          run2-id (-> (api/post-run! project-name suite-name) :body :id)
          _ (-> (mock/upload-tapir run2-id {:browser "chrome" :os "windows"} {:version "4.0"}) :body)
          run2-analysis-diffs (-> (api/get-analysis run2-id) :body :diffs)]

      ; sanity checks
      (is (number? unconnected-image-id))
      (is (not (nil? (:id run1-screenshot1))))
      (is (not (nil? (:id run1-screenshot2))))
      (is (= 204 (:status delete-run-result)))
      (is (= 201 (:status diff-update-result)))

      (is (= 0 (count unused-screenshot-ids-before-run-deletion)))
      (is (= 1 (count unused-image-ids-before-run-deletion)))
      (is (= [unconnected-image-id] unused-image-ids-before-run-deletion))

      (is (= 1 (count unused-screenshot-ids-after-run-deletion)))
      (is (= [(:id run1-screenshot2)] unused-screenshot-ids-after-run-deletion))

      (is (= 3 (count unused-image-ids-after-run-deletion)))
      (is (= (sort [unconnected-image-id
                    (:imageId (first run1-analysis-diffs))
                    (:imageId (second run1-analysis-diffs))])
             (sort unused-image-ids-after-run-deletion)))

      (is (= 200 (:status cleanup-result)))

      (is (= 0 (count unused-screenshot-ids-after-cleanup)))
      (is (= 0 (count unused-image-ids-after-cleanup)))

      ; test if the deletion really did leave the first screenshot intact
      ; for it to be used in the second run as a 'before' image
      (is (= 1 (count run2-analysis-diffs)))
      (is (= (:imageId run1-screenshot1) (-> (first run2-analysis-diffs) :before :imageId)))
      )))
