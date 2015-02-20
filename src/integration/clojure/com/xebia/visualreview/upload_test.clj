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

(ns com.xebia.visualreview.upload-test
  (:require [com.xebia.visualreview.test-util :refer [start-server stop-server]]
            [midje.sweet :refer :all]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock]
            [taoensso.timbre :as timbre]))

(timbre/merge-config!
  timbre/example-config {:fmt-output-fn :message})

(def project-name-1 "Test Project A")
(def project-name-2 "Test Project B")
(def suite-name "Test suite")
(def screenshot-properties {:os         "LINUX"
                            :browser    "firefox"
                            :resolution "1024x786"
                            :version    "31.4.0"})

(def meta-info {:takenBy   "Daniel"
                :timeStamp "2015-02-19T16:34:12"})

(defn setup-projects []
  (api/put-project! {:name project-name-1})
  (api/put-project! {:name project-name-2}))

(background
  (before :contents (mock/setup-db))
  (around :facts (mock/rebind-db-spec ?form))
  (around :facts (mock/rebind-screenshots-dir ?form)))

(facts "Screenshots"
  (against-background
    (before :contents (start-server) :after (stop-server)))
  (setup-projects)

  (fact "There are two test projects"
    (:body (api/get-projects)) => (just [(contains {:id 1 :name project-name-1})
                                         (contains {:id 2 :name project-name-2})]))

  (fact "There no runs or screenshots yet"
    (api/get-runs {:projectName project-name-1 :suiteName suite-name}) => (contains {:status 404}))

  (fact "We can upload screenshots"
    (let [run-id (-> (api/post-run! {:projectName project-name-1 :suiteName suite-name})
                     :body :id)]
      (:body (api/upload-screenshot! run-id
                                     {:file           "tapir.png"
                                      :meta           meta-info
                                      :properties     screenshot-properties
                                      :screenshotName "Tapir"})) => {:id             1
                                                                     :path           "1/1/1"
                                                                     :runId          run-id
                                                                     :screenshotName "Tapir"
                                                                     :size           38116
                                                                     :meta           meta-info
                                                                     :properties     screenshot-properties}))

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
                               :projectName  project-name-1
                               :suiteName    suite-name}))
      (fact "The diff contains a single entry"
        (count diffs) => 1)
      (fact "The status of the diff is pending"
        (:status diff) => "pending")
      (fact "The before and after images are equal"
        (:percentage diff) => 0.0
        (= before-screenshot after-screenshot) => true)
      (fact "We can retrieve the images from the returned paths"
        (let [response (api/http-get (:path before-screenshot))]
          (:status response) => 200
          (get-in response [:headers "Content-Type"]) => "image/png")))))