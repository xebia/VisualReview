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

(ns com.xebia.visualreview.integration-test
  (:require [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :refer :all]
            [midje.sweet :refer :all]
            [taoensso.timbre :as timbre]
            [com.xebia.visualreview.test-util :refer [start-server stop-server]]))

(timbre/merge-config!
  timbre/example-config {:fmt-output-fn :message
                         :ns-whitelist ["com.xebia.visualreview.integration-test"]})

(background
  (before :contents (setup-db))
  (around :facts (rebind-db-spec ?form))
  (around :facts (setup-screenshots-dir ?form)))

(def project-name-1 "A Test Project")
(def project-name-2 "Another Project")
(def suite-name "Test suite")

(facts "Projects"
  (against-background
    (before :contents (start-server) :after (stop-server)))

  (fact "Should start with empty projects"
    (:body (api/get-projects)) => [])

  (fact "Should validate request"
    (let [response (api/put-project! {})]
      (:status response) => 422
      (:body response) => #"'name' is missing"))

  (fact "Can create two projects"
    (let [response1 (api/put-project! {:name project-name-1})
          response2 (api/put-project! {:name project-name-2})]
      [(:status response1) (:status response2)] => [201 201]
      (:body response1) => {:id 1 :name project-name-1 :suites []})
    (:body (api/get-projects)) => [{:id 1 :name project-name-1} {:id 2 :name project-name-2}])

  (fact "Single project endpoint returns list of suites"
    (:body (api/get-project 1)) => {:id 1 :name project-name-1 :suites []})

  (fact "Project names are unique"
    (let [response (api/put-project! {:name project-name-1})]
      (:status response) => 409
      (:body response) => #"already exists"))

  (fact "Project name can not be empty"
    (let [response (api/put-project! {:name ""})]
      (:status response) => 422
      (:body response) => #"can not be empty")))

(facts "About Runs"
  (against-background
    (before :contents (start-server) :after (stop-server)))

  (fact "There are no runs yet"
    (api/get-runs {:projectName project-name-1 :suiteName suite-name}) => (contains {:status 404}))

  (fact "You can create runs when a project is created"
    (let [response (api/post-run! {:projectName project-name-1 :suiteName suite-name})]
      (:status response) => 201
      (:body response) => (contains {:suiteId 1 :id 1 :status "running"})))

  (fact "The run form-params are validated"
    (let [response (api/post-run! {})]
      (:status response) => 422
      (:body response) => #"'projectName' is missing")
    (api/post-run! {:projectName project-name-1}) => (contains {:status 422
                                                                 :body   #"'suiteName' is missing"})
    (api/post-run! {:projectName "Wrong name" :suiteName ""}) => (contains {:status 422
                                                                              :body   #"can not be empty"}))

  (fact "The runs resource returns the runs for a suite"
    (let [expected-result (contains {:id 1 :status "running" :projectId 1 :suiteId 1})]
      (:body (api/get-runs {:projectName project-name-1 :suiteName suite-name})) => (just [expected-result])
      (:body (api/get-run 1)) => expected-result)))

(facts "About suites"
  (against-background
    (before :contents (start-server) :after (stop-server)))

  (fact "Can't get suites for a nonexisting project"
    (api/get-suites 0) => (contains {:status 404})))


(comment
  ;; Force clear this namespace
  (remove-ns 'com.xebia.visualreview.integration-test)
  )
