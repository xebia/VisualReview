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
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.test-util :refer [start-server stop-server]]
            [taoensso.timbre :as timbre]))

(timbre/set-level! :warn)

(def project-name-1 "Test Project A")
(def suite-name "Test suite")
(def meta-info {:os         "LINUX"
                :version    "31.4.0"})
(def screenshot-properties {:browser "firefox"
                            :resolution "1024x786"})

(defn setup-projects []
  (api/put-project! {:name project-name-1})
  (api/post-run! {:projectName project-name-1 :suiteName suite-name})
  (api/upload-screenshot! 1 {:file           "chess1.png"
                             :meta           meta-info
                             :properties     screenshot-properties
                             :screenshotName "Kasparov vs Topalov - 1999"}))

(background
  (before :contents (mock/setup-db))
  (around :facts (mock/rebind-db-spec ?form))
  (around :facts (mock/rebind-screenshots-dir ?form)))

(facts "Analysis"
  (against-background
    (before :contents (start-server) :after (stop-server)))
  (setup-projects)

  (fact "There is one run with a pending screenshot"
    (count (:body (api/get-projects))) => 1
    (count (:body (api/get-runs {:projectName project-name-1 :suiteName suite-name}))) => 1
    (let [diff (-> (api/get-analysis 1) :body :diffs first)]
      (:status diff) => "pending"))

  (fact "We can accept and reject diffs"
    (api/update-diff-status! 1 1 "ejected") => (contains {:status 422
                                                          :body   #"must be .*rejected"})
    (api/update-diff-status! 1 1 "rejected") => (contains {:status 201
                                                           :body   (contains {:status "rejected"})})
    (:body (api/update-diff-status! 1 1 "pending")) => (contains {:status "pending"})
    (:body (api/update-diff-status! 1 1 "accepted")) => (contains {:status "accepted"}))

  (fact "We cannot upload another screenshot with the same name in the same run"
    ;; TODO: This should be possible. A check for differences in meta-data should be made.
    ;; TODO: When it fails, it should not return status 201. 409 would be better, i.e. we should actually use PUT, not POST
    (api/upload-screenshot! 1 {:file           "chess2.png"
                               :meta           meta-info
                               :properties     screenshot-properties
                               :screenshotName "Kasparov vs Topalov - 1999"}) => (contains {:status 201
                                                                                            :body   (just {:error #"already exists"})}))

  (fact "We can start a new run and upload a new screenshot version"
    (api/post-run! {:projectName project-name-1 :suiteName suite-name})
    (api/upload-screenshot! 2 {:file           "chess2.png"
                               :meta           meta-info
                               :properties     screenshot-properties
                               :screenshotName "Kasparov vs Topalov - 1999"}) => (contains {:status 201}))

  (fact "The new diff is now pending and differs from its previous version"
    (let [{:keys [analysis diffs]} (:body (api/get-analysis 2))
          diff (first diffs)]
      analysis => (contains {:id         2
                             :baselineId 1
                             :runId      2})
      (count diffs) => 1
      diff => (contains {:percentage 1.03
                         :status     "pending"})
      (get-in diff [:before :id]) => 1
      (get-in diff [:after :id]) => 3

      (fact "We can retrieve the diff image"
        (let [response (api/http-get (:path diff))]
          (get-in response [:headers "Content-Type"]) => "image/png"
          (:status response) => 200)))))

