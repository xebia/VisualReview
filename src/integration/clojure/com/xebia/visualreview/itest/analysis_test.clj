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

(ns com.xebia.visualreview.itest.analysis-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.itest-util :refer [start-server stop-server]]))

(def project-name "Test Project A")
(def suite-name "Test suite")
(def meta-info {:os      "LINUX"
                :version "31.4.0"})
(def properties {:browser    "firefox"
                 :resolution "1024x786"})

(def image-ids (atom {}))

(defn post-run-with-screenshots [& {:as fns}]
  (let [run-id (-> (api/post-run! project-name suite-name) :body :id)]
    (doseq [[k v] fns]
      (swap! image-ids assoc-in [run-id k] (-> (v run-id meta-info properties) :body :id)))
    run-id))

(defn setup-project []
  (api/put-project! {:name project-name})
  (post-run-with-screenshots :chess mock/upload-chess-image-1 :tapir mock/upload-tapir))

(defn- content-type [response]
  (get-in response [:headers "Content-Type"]))

(use-fixtures :each mock/logging-fixture mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture mock/test-server-fixture)

(deftest analysis
  (setup-project)

  (testing "Retrieving analysis for a run"
    (let [analysis-response (api/get-analysis 1)
          {:keys [analysis diffs]} (:body analysis-response)
          [before-screenshot after-screenshot] ((juxt :before :after) (first diffs))]
      (is (= 200 (:status analysis-response)) "OK status")
      (are [k v] (if (fn? v) (v (k analysis)) (= (k analysis) v))
        :baselineNode 1
        :creationTime string?
        :projectName project-name
        :suiteName suite-name)
      (is (= 2 (count diffs)) "The diff contains two entries")
      (is (= "pending" (:status (first diffs))) "The first status is pending")
      (is (= "pending" (:status (second diffs))) "The second status is pending")
      (is (and (zero? (:percentage (first diffs)))
               (= before-screenshot after-screenshot)) "The before and after images are equal")

      (testing "Retrieving images from returned paths"
        (let [before-image (api/get-image (:imageId before-screenshot))
              diff-image (api/get-image (:imageId (first diffs)))]
          (is (= 200 (:status before-image)) "OK status")
          (is (= 200 (:status diff-image)) "OK status")
          (is (= "image/png" (content-type before-image)) "Image has content-type image/png")
          (is (= "image/png" (content-type diff-image)) "Diff has content-type image/png")))))

  (testing "Analysis status"
    (is (= 1 (count (:body (api/get-runs project-name suite-name)))) "There is one run")
    (let [[chess-diff tapir-diff] (-> (api/get-analysis 1) :body :diffs)]
      (is (= "pending" (:status chess-diff)) "The chess diff is pending")
      (is (= "pending" (:status tapir-diff)) "The tapir diff is pending")

      ;; TODO: Support a nil before-screenshot. That is more correct for the very first screenshot version
      "The before and after screenshots are the same for the first run"
      (are [diff] (= (-> diff :before :id) (-> diff :after :id))
        chess-diff tapir-diff)))

  (testing "Diff approval process"
    (let [response (api/update-diff-status! 1 1 "ejected")]
      (is (= 422 (:status response)) "Unprocessable entity")
      (is (re-find #"must be .*rejected" (:body response)))
      (are [run-id diff-id http-status new-status] (let [r (api/update-diff-status! run-id diff-id new-status)]
                                                     (and (= (:status r) http-status)
                                                          (= (-> r :body :status) new-status)))
        1 1 201 "rejected"
        1 1 201 "accepted"
        1 2 201 "accepted")))

  (testing "New run with different tapir image"
    (let [run-id (post-run-with-screenshots :chess mock/upload-chess-image-1 :tapir mock/upload-tapir-hat)
          {:keys [analysis diffs]} (:body (api/get-analysis run-id))
          [chess-diff tapir-diff] diffs]
      (are [k v] (= (k analysis) v)
        :id 2
        :baselineNode 1
        :runId 2)

      (is (= "pending" (:status tapir-diff)) "The tapir diff is pending")
      (is (= 8.89 (:percentage tapir-diff)))
      (is (> (-> tapir-diff :after :id) (-> tapir-diff :before :id)) "The tapir image differs from its previous version")

      (is (zero? (:percentage chess-diff)) "The chess diff is unchanged")
      (is (= "accepted" (:status chess-diff)) "The chess diff is automatically accepted")

      (is (= 201 (:status (api/update-diff-status! run-id (:id tapir-diff) "rejected"))) "Rejecting the tapir diff")))

  (testing "Third run with a different chess image"
    (let [run-id (post-run-with-screenshots :chess mock/upload-chess-image-2 :tapir mock/upload-tapir-hat)
          [chess-diff tapir-diff] (-> (api/get-analysis run-id) :body :diffs)]
      (is (= (:tapir (@image-ids (- run-id 2))) (-> tapir-diff :before :id)) "The tapir diff is compared with the version from the first run, as the second version")
      (is (= "pending" (:status tapir-diff)) "The tapir diff is pending")
      (is (= 8.89 (:percentage tapir-diff)) "The percentage difference is the same as in the first run")

      (is (= 1.03 (:percentage chess-diff)) "The chess image is changed with respect to the previous run")
      (is (= "pending" (:status chess-diff)) "The chess diff is pending")
      (is (= (:chess (@image-ids (dec run-id))) (-> chess-diff :before :id)) "The chess image is compared to the previous run"))))
