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
(def meta-info {:os         "LINUX"
                :browser    "firefox"
                :resolution "1024x786"
                :version    "31.4.0"})

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

  (fact "We can upload screenshots"
    (let [run-id (-> (api/post-run! {:project-name project-name-1 :suite-name suite-name})
                     :body :id)]
      (:body (api/upload-screenshot! run-id
                                     {:file            "tapir.png"
                                      :meta            meta-info
                                      :screenshot-name "Tapir"})) => {:id             1
                                                                      :browser        "firefox"
                                                                      :os             "LINUX"
                                                                      :path           "1/1/1"
                                                                      :resolution     "1024x786"
                                                                      :runId          run-id
                                                                      :screenshotName "Tapir"
                                                                      :size           38116
                                                                      :version        "31.4.0"})))