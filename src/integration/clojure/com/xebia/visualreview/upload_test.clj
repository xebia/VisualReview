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
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock :refer [upload-chess-image-1 upload-chess-image-2 upload-tapir]]
            [midje.sweet :refer :all]))

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

(background
  (before :contents (mock/setup-db))
  (around :facts (mock/rebind-db-spec ?form))
  (around :contents (mock/setup-screenshots-dir ?form)))

(facts "Screenshots"
  (against-background
    (before :contents (start-server) :after (stop-server)))
  (setup-projects)

  (fact "There are two test projects"
    (:body (api/get-projects)) => (just [(contains {:id 1 :name project-name-1})
                                         (contains {:id 2 :name project-name-2})]))

  (fact "There no runs or screenshots yet"
    (api/get-runs {:projectName project-name-1 :suiteName suite-name}) => (contains {:status 404}))

  (let [run-id (-> (api/post-run! {:projectName project-name-1 :suiteName suite-name}) :body :id)]
    (fact "We can upload screenshots"
      (upload-tapir run-id meta-info properties) => (contains {:status 201
                                                               :body   {:id             1
                                                                        :runId          run-id
                                                                        :imageId        1
                                                                        :screenshotName "Tapir"
                                                                        :size           38116
                                                                        :meta           meta-info
                                                                        :properties     properties}}))

    (fact "We cannot upload screenshots with the same name and properties in the same run"
      (upload-chess-image-1 run-id meta-info properties) => (contains {:status 201})
      (upload-chess-image-2 run-id meta-info properties) => (contains {:status 200
                                                                       :body   (just {:error             #"already uploaded"
                                                                                      :conflictingEntity map?})}))

    (fact "We can upload screenshots with the same name and meta, but different properties"
      (upload-chess-image-2 run-id meta-info (assoc properties :resolution "800x600")) => (contains {:status 201})))

  (let [next-run-id (-> (api/post-run! {:projectName project-name-1 :suiteName suite-name}) :body :id)]
    (fact "In a new run, we can upload a screenshot with identical name and props as in the previous run"
      (upload-chess-image-2 next-run-id meta-info properties) => (contains {:status 201}))))