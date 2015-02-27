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

(ns com.xebia.visualreview.service.image
  (:require [com.xebia.visualreview.io :as io]
            [com.xebia.visualreview.persistence :as p])
  (:import (java.util Calendar)))


;; check if there's enough space..?
(defn insert-image!
  "Stores an image. Returns the image ID."
  [conn file]
  {:pre [conn file]}
  (let [now (Calendar/getInstance)
       directory (str (.get now Calendar/YEAR) "/" (.get now Calendar/MONTH) "/" (.get now Calendar/DAY_OF_MONTH) "/" (.get now Calendar/HOUR_OF_DAY))
       image-id (p/insert-image! conn directory)]
       (io/store-image! file directory image-id)
  image-id))

(defn get-image-path
  [conn image-id]
  (p/get-image-path conn image-id))
