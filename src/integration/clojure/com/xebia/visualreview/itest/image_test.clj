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

(ns com.xebia.visualreview.itest.image-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [com.xebia.visualreview.image :as image]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.io :as vrio])
  (:import [java.util Calendar]))

(defn- YYYYMMDDH [^Calendar calendar]
  (apply str (interpose "/" (mapv #(.get calendar %) [Calendar/YEAR Calendar/MONTH Calendar/DAY_OF_MONTH Calendar/HOUR_OF_DAY]))))

(use-fixtures :each mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture)

(deftest image-service

  (testing "Storing and retrieving an image based on an image-id"
    (let [expected-path-prefix (YYYYMMDDH (Calendar/getInstance))
          image-1-src (io/as-file (io/resource "tapir.png"))
          image-2-src (io/as-file (io/resource "tapir_hat.png"))
          image-1-id (image/insert-image! mock/*conn* image-1-src)
          image-2-id (image/insert-image! mock/*conn* image-2-src)
          image-1-path (image/get-image-path mock/*conn* image-1-id)
          image-2-path (image/get-image-path mock/*conn* image-2-id)]
      (are [image-id image-path] (= (str expected-path-prefix "/" image-id ".png") image-path)
        image-1-id image-1-path
        image-2-id image-2-path)
      (are [image-src image-path] (= (.length image-src) (.length (io/as-file (str vrio/screenshots-dir "/" image-path))))
        image-1-src image-1-path
        image-2-src image-2-path))))

