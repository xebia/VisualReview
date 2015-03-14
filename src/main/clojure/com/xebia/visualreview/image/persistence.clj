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

(ns com.xebia.visualreview.image.persistence
  (:require [com.xebia.visualreview.persistence.util :as putil]))


(defn insert-image!
  "Adds a new image to the database. Returns the new image's ID."
  [conn directory]
  (putil/insert-single! conn :image { :directory directory }))

(defn get-image-path
  "Gets the path of an image with the given image ID.
  The path will contain the directory structure and file name of the image
  relative to the screenshot directory. Example: '2015/1/15/22/1.png'."
  [conn image-id]
  (let [image (putil/query-single conn
                                  ["SELECT id, directory FROM image WHERE id = ?" image-id])
        directory (:directory image)
        id (:id image)]
    (str directory "/" id ".png")))