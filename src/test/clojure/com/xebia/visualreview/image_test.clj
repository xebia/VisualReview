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
  (:require [com.xebia.visualreview.image :as image]
            [com.xebia.visualreview.image.persistence :as ip]
            [com.xebia.visualreview.io :as vrio]
            [com.xebia.visualreview.service-util-test :as sutilt]
            [midje.sweet :refer :all]
            [clojure.java.io :as io])
  (:import [java.io IOException File]
           [clojure.lang IExceptionInfo]
           [java.sql SQLException]))

(facts "Image service"
  (facts "insert-image!"
    (fact "throws a service exception when the image could not be stored on the file system"
      (let [image-1-src (io/as-file (io/resource "tapir.png"))]
        (image/insert-image! {} image-1-src)
        => (throws IExceptionInfo (sutilt/is-service-exception? "Could not store image with id 3 on filesystem: Disk full" ::image/image-cannot-store-on-fs))
        (provided
          (ip/insert-image! anything anything) => 3
          (vrio/store-image! anything anything anything) =throws=> (IOException. "Disk full"))))

    (fact "throws a service exception when the image file could not be read"
      (let [image-1-src (File. "non-existing-file.nope")]
        (image/insert-image! {} image-1-src)
        => (throws IExceptionInfo (sutilt/is-service-exception? "Cannot store image: file cannot be read" ::image/image-cannot-store-on-fs-cannot-read))))

    (fact "throws a service exception when the image metadata could not be stored in the database"
      (let [image-1-src (io/as-file (io/resource "tapir.png"))]
        (image/insert-image! {} image-1-src)
        => (throws IExceptionInfo (sutilt/is-service-exception? "Could not record new image in the database: Database error" ::image/image-cannot-store-on-db))
        (provided (ip/insert-image! anything anything) =throws=> (SQLException. "Database error")))))

  (facts "get-image-path"
    (fact "retrieves the image's path"
      (image/get-image-path {} 1)
      => "some-dir/some-file.png"
      (provided (ip/get-image-path anything 1) => "some-dir/some-file.png"))
    (fact "throws a service exception when the image's path could not be retrieved"
      (image/get-image-path {} 1)
      => (throws IExceptionInfo (sutilt/is-service-exception? "Could not retrieve image path for image with id 1: Database error" ::image/image-could-not-retrieve-path))
      (provided (ip/get-image-path anything 1) =throws=> (SQLException. "Database error")))))
