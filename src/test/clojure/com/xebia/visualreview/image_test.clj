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

(ns com.xebia.visualreview.image-test
  (:require [clojure.test :refer :all]
            [slingshot.test]
            [com.xebia.visualreview.service.image :as image]
            [com.xebia.visualreview.service.image.persistence :as ip]
            [com.xebia.visualreview.io :as vrio]
            [com.xebia.visualreview.test-util :refer :all]
            [clojure.java.io :as io])
  (:import [java.sql SQLException]
           [java.io IOException]))

(deftest insert-image

  (let [image-1-src (io/as-file (io/resource "tapir.png"))]
    (with-mock [ip/insert-image! 3
                vrio/store-png-image! (throw (IOException. "Disk full"))]
      (is (thrown+-with-msg? service-exception? #"Could not store image with id 3 on filesystem: Disk full"
                             (image/insert-image! {} image-1-src))
          "throws a service exception when the image could not be stored on the file system")))

  (let [image-1-src (io/file "non-existing-file.nope")]
    (is (thrown+-with-msg? service-exception? #"Cannot store image: file cannot be read"
                           (image/insert-image! {} image-1-src))
        "throws a service exception when the image file could not be read"))

  (let [image-1-src (io/as-file (io/resource "tapir.png"))]
    (with-mock [ip/insert-image! (throw (SQLException. "Database error"))]
      (is (thrown+-with-msg? service-exception? #"Could not record new image in the database: Database error"
                             (image/insert-image! {} image-1-src))
          "throws a service exception when the image metadata could not be stored in the database"))))

(deftest get-image-path
  (with-mock [ip/get-image-path "some-dir/some-file.png"]
    (is (= "some-dir/some-file.png" (image/get-image-path {} 1)) "retrieves the image's path"))
  (with-mock [ip/get-image-path (throw (SQLException. "Database error"))]
    (is (thrown+-with-msg? service-exception? #"Could not retrieve image path for image with id 1: Database error"
                           (image/get-image-path {} 1))
        "throws a service exception when the image's path could not be retrieved")))
