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

(ns com.xebia.visualreview.analysis.core
  (:import [com.xebia.visualreview PixelComparator DiffReport]))

(defn diff-report
  "Takes 2 inputfiles and returns a map with:
  :diff => A BufferedImage of the diff,
  :percentage => A double with the percentage difference found"
  [file1 file2]
  (let [result ^DiffReport (PixelComparator/processImage file1 file2)]
    {:diff (.getDiffImage result)
     :percentage (.getPercentage result)}))
