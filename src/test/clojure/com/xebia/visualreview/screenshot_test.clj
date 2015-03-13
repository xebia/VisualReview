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

(ns com.xebia.visualreview.screenshot-test
  (:require [midje.sweet :refer :all]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [com.xebia.visualreview.screenshot :as s]
            [com.xebia.visualreview.persistence :as p]
            [com.xebia.visualreview.service-util-test :as sutilt])
  (:import (java.sql SQLException)
           (clojure.lang IExceptionInfo)))

(timbre/set-level! :warn)

(facts "Screenshot service"
       (fact "throws a service exception when given run-id does not exist"
             (let [image-file (io/as-file (io/resource "tapir_hat.png"))]
               (s/insert-screenshot! {} 999 "myScreenshot" {:browser "chrome" :os "windows"} {:version "4.0"} image-file)
               => (throws IExceptionInfo (sutilt/is-service-exception? "Could not store screenshot, run id 999 does not exist." ::s/screenshot-cannot-store-in-db-runid-does-not-exist))
             (provided (p/get-run anything 999) => nil)))

       (fact "throws a service exception when screenshot could not be stored in the database"
             (let [run-id 1
                   image-file (io/as-file (io/resource "tapir_hat.png"))
                   image-id 2]
               (s/insert-screenshot! {} run-id "myScreenshot" {:browser "chrome" :os "windows"} {:version "4.0"} image-file)
                                     => (throws IExceptionInfo (sutilt/is-service-exception? "Could not store screenshot in database: Database error" ::s/screenshot-cannot-store-in-db))
               (provided
                 (p/get-run anything anything) => {:id run-id}
                 (p/insert-image! anything anything) => {:id image-id}
                 (p/save-screenshot! anything anything anything anything anything anything anything)
                         =throws=> (SQLException. "Database error"))))

       (fact "throws a specific 'screenshot already exists' service exception when database layer reports a duplicate record"
             (let [run-id 1
                   image-file (io/as-file (io/resource "tapir_hat.png"))
                   image-id 2]
               (s/insert-screenshot! {} run-id "myScreenshot" {:browser "chrome" :os "windows"} {:version "4.0"} image-file)
               => (throws IExceptionInfo (sutilt/is-service-exception? "Could not store screenshot in database: screenshot with name and properties already exists" ::s/screenshot-cannot-store-in-db-already-exists))
               (provided
                 (p/get-run anything anything) => {:id run-id}
                 (p/insert-image! anything anything) => {:id image-id}
                 (p/save-screenshot! anything anything anything anything anything anything anything)
                 =throws=> (sutilt/slingshot-exception {:type :sql-exception :subtype ::p/unique-constraint-violation :message "Duplicate thingy"}))))

       (fact "returns nil when retrieving a screenshot that does not exist"
             (s/get-screenshot-by-id {} 999) => nil
             (provided
               (p/get-screenshot-by-id anything anything) => nil))

       (fact "returns a service exception when an error occurs while retrieving a screenshot"
             (s/get-screenshot-by-id {} 999)
                                     => (throws IExceptionInfo (sutilt/is-service-exception? "Could not retrieve screenshot with id 999: Database error" ::s/screenshot-cannot-retrieve-from-db))
             (provided (p/get-screenshot-by-id anything anything) =throws=> (SQLException. "Database error"))))
