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

(ns com.xebia.visualreview.service.image-test
  (:require [midje.sweet :refer :all]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [com.xebia.visualreview.service.image :as image]
            [com.xebia.visualreview.persistence :as p]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.io :as vrio]
            [com.xebia.visualreview.service.service-util-test :as sutilt])
  (:import (java.util Calendar)
           (java.io IOException)
           (clojure.lang IExceptionInfo)
           (java.sql SQLException)))

(timbre/set-level! :warn)

(background
  (before :contents (mock/setup-db))
  (around :facts (mock/rebind-db-spec ?form))
  (around :facts (mock/setup-screenshots-dir ?form)))

(facts "Image service"
       (fact "stores and retrieves an image based on an image-id"
             (let [now (Calendar/getInstance)
                   expected-path-prefix (str (.get now Calendar/YEAR) "/" (.get now Calendar/MONTH) "/" (.get now Calendar/DAY_OF_MONTH) "/" (.get now Calendar/HOUR_OF_DAY))
                   image-1-src (io/as-file (io/resource "tapir.png"))
                   image-2-src (io/as-file (io/resource "tapir_hat.png"))
                   image-1-id (image/insert-image! mock/*conn* image-1-src)
                   image-2-id (image/insert-image! mock/*conn* image-2-src)
                   image-1-path (image/get-image-path mock/*conn* image-1-id)
                   image-2-path (image/get-image-path mock/*conn* image-2-id)]
               image-1-path => (str expected-path-prefix "/" image-1-id ".png")
               image-2-path => (str expected-path-prefix "/" image-2-id ".png")
               (.length (io/as-file (str vrio/screenshots-dir "/" image-1-path))) => (.length image-1-src )
               (.length (io/as-file (str vrio/screenshots-dir "/" image-2-path))) => (.length image-2-src )))
       (facts "insert-image! function"
              (fact "throws a service exception when the image could not be stored on the file system"
                    (let [image-1-src (io/as-file (io/resource "tapir.png"))]
                    (image/insert-image! mock/*conn* image-1-src)
                                         => (throws IExceptionInfo (sutilt/is-service-exception? "Could not store image with id 3 on filesystem: Disk full" "img-cannot-store-on-fs"))
                    (provided (vrio/store-image! anything anything anything) =throws=> (IOException. "Disk full"))))

              (fact "throws a service exception when the image metadata could not be stored in the database"
                    (let [image-1-src (io/as-file (io/resource "tapir.png"))]
                      (image/insert-image! mock/*conn* image-1-src)
                      => (throws IExceptionInfo (sutilt/is-service-exception? "Could not record new image in the database: Database error" "img-cannot-store-on-db"))
                      (provided (p/insert-image! anything anything) =throws=> (SQLException. "Database error"))))))