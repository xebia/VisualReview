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
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.persistence :as p]
            [com.xebia.visualreview.project :as project]
            [clojure.java.io :as io]
            [com.xebia.visualreview.screenshot :as s]))

(use-fixtures :each mock/setup-screenshot-dir-fixture mock/setup-db-fixture)

(deftest screenshot-service

  (testing "Storing and retrieving a screenshot based on a screenshot-id"
    (project/create-project! mock/*conn* "myProject")
    (p/create-suite-for-project! mock/*conn* "myProject" "mySuite")
    (let [run-id (p/create-run! mock/*conn* {:project-name "myProject" :suite-name "mySuite"})
          image-file (io/as-file (io/resource "tapir_hat.png"))
          screenshot-id (s/insert-screenshot! mock/*conn* run-id "myScreenshot" {:browser "chrome" :os "windows"} {:version "4.0"} image-file)
          screenshot (s/get-screenshot-by-id mock/*conn* screenshot-id)]
      (is (= 1 screenshot-id) "The first screenshot-id is 1")
      (is (= {:id              screenshot-id
              :image-id        1
              :meta            {:version "4.0"}
              :properties      {:browser "chrome" :os "windows"}
              :screenshot-name "myScreenshot"
              :run-id          run-id
              :size            (.length image-file)} screenshot)))))
