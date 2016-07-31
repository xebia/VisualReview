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

(ns com.xebia.visualreview.service.analysis.core
  (:require [com.xebia.visualreview.service.service-util :as sutil]
            [clojure.java.io :as io]
            [cheshire.core :as json])
  (:import [com.xebia.visualreview PixelComparator DiffReport]
           [javax.imageio ImageIO]
           [java.io File]))

(defn- generate-empty-diff-file
  []
  (let [output-file (File/createTempFile "vr-empty-diff-" ".tmp")]
    (with-open [empty-diff-reader (clojure.java.io/input-stream (io/resource "1x1.png"))]
      (io/copy empty-diff-reader output-file))
    output-file))

(defn generate-diff-report
  "Takes 2 inputfiles and returns a map with:
  :diff => A image file of the diff,
  :percentage => A double with the percentage difference found

  If file1 or file2 is nil, the diff will be a default transparant 1x1 png and
  percentage will be 0.0"
  [file1 file2 mask compare-settings]
  (if (or (nil? file1) (nil? file2))
    {:diff (generate-empty-diff-file) :percentage 0.0 :mask (generate-empty-diff-file)}
    (let [result ^DiffReport (PixelComparator/processImage file1 file2 mask (json/generate-string compare-settings))
          diff-file (File/createTempFile "vr-diff-" ".tmp")
          mask-file (File/createTempFile "vr-mask-" ".tmp")
          write-success? (and (if (nil? (.getMaskImage result)) (generate-empty-diff-file) (ImageIO/write (.getMaskImage result) "png" mask-file)) (ImageIO/write (.getDiffImage result) "png" diff-file))]
      (do
        (sutil/assume (true? write-success?) (str "Could not write diff/mask images to temporary file " (.getAbsolutePath () diff-file)) ::diff-could-not-write-on-fs))
      {:diff       diff-file
       :percentage (.getPercentage result)
       :mask mask-file
       })))
