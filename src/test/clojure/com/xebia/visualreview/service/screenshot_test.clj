;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Copyright 2015 Xebia B.V.
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;  http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.xebia.visualreview.service.screenshot-test
  (:require [clojure.test :refer :all]
            [slingshot.test]
            [clojure.java.io :as io]
            [com.xebia.visualreview.service.screenshot :as s]
            [com.xebia.visualreview.service.screenshot.persistence :as sp]
            [com.xebia.visualreview.service.image :as i]
            [com.xebia.visualreview.test-util :refer :all]
            [com.xebia.visualreview.service.run :as run])
  (:import [java.sql SQLException]))

(deftest insert-screenshot
  (let [run-id 1
        image-id 2
        image-file (io/as-file (io/resource "tapir_hat.png"))
        insert-screenshot-fn #(s/insert-screenshot! {} 999 "myScreenshot" {:browser "chrome" :os "windows"} {:version "4.0"} image-file)]
    (is (thrown+-with-msg? service-exception? #"Could not store screenshot, run id 999 does not exist."
                           (with-mock [run/get-run nil]
                             (insert-screenshot-fn))) "Throws a service exception when given run-id does not exist")
    (is (thrown+-with-msg? service-exception? #"Could not store screenshot in database: Database error"
                           (with-mock [run/get-run {:id run-id}
                                       i/insert-image! {:id image-id}
                                       sp/save-screenshot! (throw (SQLException. "Database error"))]
                             (insert-screenshot-fn))) "throws a service exception when screenshot could not be stored in the database")
    (is (thrown+-with-msg? service-exception? #"Could not store screenshot in database: screenshot with name and properties already exists"
                           (with-mock [run/get-run {:id run-id}
                                       i/insert-image! {:id image-id}
                                       sp/save-screenshot! (throw (slingshot-exception {:type :sql-exception :subtype ::sp/unique-constraint-violation :message "Duplicate thingy"}))]
                             (insert-screenshot-fn))))))

(deftest get-screenshot-by-id
  (with-mock [sp/get-screenshot-by-id nil]
    (is (nil? (s/get-screenshot-by-id {} 999)) "Returns nil when retrieving a screenshot that does not exist"))
  (with-mock [sp/get-screenshot-by-id (throw (SQLException. "Database error"))]
    (is (thrown+-with-msg? service-exception? #"Could not retrieve screenshot with id 999: Database error"
                           (s/get-screenshot-by-id {} 999)) "returns a service exception when an error occurs")))

(deftest get-screenshots-by-run-id
  (with-mock [sp/get-screenshots [{:id 1} {:id 2}]]
    (is (= [{:id 1} {:id 2}] (s/get-screenshots-by-run-id {} 123)) "Returns a list of screnshots from a run"))
  (with-mock [sp/get-screenshots (throw (SQLException. "An error occurred 1"))]
    (is (thrown+-with-msg? service-exception? #"Could not retrieve screenshots: An error occurred 1"
                           (s/get-screenshots-by-run-id {} 123)))))
