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
            [com.xebia.visualreview.service.run :as run]))

(use-fixtures :each mock/logging-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture)

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
              :size            (.length image-file)} screenshot)))))
