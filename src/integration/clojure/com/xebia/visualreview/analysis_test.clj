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

(ns com.xebia.visualreview.analysis-test
  (:require [midje.sweet :refer :all]
            [taoensso.timbre :as timbre]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.test-util :refer [start-server stop-server]]))

(timbre/set-level! :warn)

(def project-name "Test Project A")
(def suite-name "Test suite")
(def meta-info {:os      "LINUX"
                :version "31.4.0"})
(def properties {:browser    "firefox"
                 :resolution "1024x786"})

(def image-ids (atom {}))

(defn post-run-with-screenshots [& {:as fns}]
  (let [run-id (-> (api/post-run! {:projectName project-name :suiteName suite-name}) :body :id)]
    (doseq [[k v] fns]
      (swap! image-ids assoc-in [run-id k] (-> (v run-id meta-info properties) :body :id)))
    run-id))

(defn setup-project []
  (api/put-project! {:name project-name})
  (post-run-with-screenshots :chess mock/upload-chess-image-1 :tapir mock/upload-tapir))

(background
  (before :contents (mock/setup-db))
  (around :facts (mock/rebind-db-spec ?form))
  (around :contents (mock/setup-screenshots-dir ?form)))

(facts "Analysis"
  (against-background
    (before :contents (start-server) :after (stop-server)))
  (setup-project)

  (fact "We can retrieve the analysis for a run"
    (let [analysis-response (api/get-analysis 1)
          {:keys [analysis diffs]} (:body analysis-response)
          diff (first diffs)
          before-screenshot (:before diff)
          after-screenshot (:after diff)]
      (:status analysis-response) => 200
      (fact "The analysis contains a baseline for the suite and other information"
        analysis => (contains {:baselineId   1
                               :creationTime #""
                               :projectName  project-name
                               :suiteName    suite-name}))
      (fact "The diff contains two entries"
        (count diffs) => 2)
      (fact "The status of the diff is pending"
        (:status diff) => "pending")
      (fact "The before and after images are equal"
        (:percentage diff) => 0.0
        (= before-screenshot after-screenshot) => true)
      (fact "We can retrieve the images from the returned paths"
        (let [before-image (api/get-image (:imageId before-screenshot))
              diff-image (api/get-image (:imageId diff))]
          (map :status [before-image diff-image]) => (two-of 200)
          (map #(get-in % [:headers "Content-Type"]) [before-image diff-image]) => (two-of "image/png")))))

  (fact "There is one run with two pending screenshots"
    (count (:body (api/get-projects))) => 1
    (count (:body (api/get-runs {:projectName project-name :suiteName suite-name}))) => 1
    (let [[chess-diff tapir-diff] (-> (api/get-analysis 1) :body :diffs)]
      (:status chess-diff) => "pending"
      (:status tapir-diff) => "pending"

      (fact "The before and after screenshots are the same for the first run"
        ;; TODO: Make support for a nil before-screenshot. That is more correct for the very first screenshot version
        (-> chess-diff :before :id) => (-> chess-diff :after :id)
        (-> tapir-diff :before :id) => (-> tapir-diff :after :id))))

  (fact "We can accept and reject diffs"
    (api/update-diff-status! 1 1 "ejected") => (contains {:status 422 :body #"must be .*rejected"})
    (api/update-diff-status! 1 1 "rejected") => (contains {:status 201 :body (contains {:status "rejected"})})
    (:body (api/update-diff-status! 1 1 "pending")) => (contains {:status "pending"})
    (:body (api/update-diff-status! 1 1 "accepted")) => (contains {:status "accepted"})
    (:body (api/update-diff-status! 1 2 "accepted")) => (contains {:status "accepted"}))

  (fact "After starting a new run and uploading a different tapir image"
    (let [run-id (post-run-with-screenshots :chess mock/upload-chess-image-1 :tapir mock/upload-tapir-hat)
          {:keys [analysis diffs]} (:body (api/get-analysis run-id))
          [chess-diff tapir-diff] diffs]
      analysis => (contains {:id 2 :baselineId 1 :runId 2})

      (fact "The tapir diff is pending and the image differs from its previous version"
        tapir-diff => (contains {:percentage 8.89 :status "pending"})
        (-> tapir-diff :before :id) => (-> (@image-ids 1) :tapir)
        (-> tapir-diff :after :id) => (-> (@image-ids run-id) :tapir))

      (fact "The chess image is unchanged and the diff is automatically accepted"
        chess-diff => (contains {:percentage 0.00 :status "accepted"})
        (-> chess-diff :before :id) => (-> (@image-ids 1) :chess)
        (-> chess-diff :after :id) => (-> (@image-ids run-id) :chess))

      (fact "We reject the tapir diff"
        (:body (api/update-diff-status! run-id (:id tapir-diff) "rejected")) => (contains {:status "rejected"}))))

  (fact "After starting a third run and uploading a different chess image also"
    (let [run-id (post-run-with-screenshots :chess mock/upload-chess-image-2 :tapir mock/upload-tapir-hat)
          [chess-diff tapir-diff] (-> (api/get-analysis run-id) :body :diffs)]

      (fact "The tapir diff is compared with the version from the first run, as it was rejected"
        tapir-diff => (contains {:percentage 8.89 :status "pending"})
        (-> tapir-diff :before :id) => (-> (@image-ids 1) :tapir)
        (-> tapir-diff :after :id) => (-> (@image-ids run-id) :tapir))

      (fact "The chess image is changed with respect to the previous run"
        chess-diff => (contains {:percentage 1.03 :status "pending"})
        (-> chess-diff :before :id) => (-> (@image-ids 2) :chess)
        (-> chess-diff :after :id) => (-> (@image-ids run-id) :chess)))))

