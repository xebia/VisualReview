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

(ns com.xebia.visualreview.util
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.text SimpleDateFormat)
           (java.util Date)))

(defn map-keys [f]
  (fn [m]
    (let [ks (keys m)]
      (set/rename-keys m (zipmap ks (mapv (comp keyword f name) ks))))))

(defn camelize [s]
  (string/replace s #"[_-](\w)" (fn [[_ ^String m]] (.toUpperCase m))))

(defn hyphenize [[c :as s]]
  (str c (string/replace (subs s 1) #"[A-Z]" (fn [^String x] (str \- (.toLowerCase x))))))

(def ^SimpleDateFormat date-format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZ"))

(defn format-date [m k]
  (if-let [date ^Date (k m)]
    (assoc m k (.format date-format date))
    m))