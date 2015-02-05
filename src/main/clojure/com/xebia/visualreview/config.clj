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

(ns com.xebia.visualreview.config
  (:require [environ.core :refer [env]]
            [com.xebia.visualreview.validation :as v]))

(def ^:private config-env-prefix "visualreview-")

(def config-schema
  "Expected keys and related validators for configuration data"
  {:port [Long []]
   :db-uri [String []]
   :db-user [String []]
   :db-password [String [::v/optional]]
   :screenshots-dir [String [::v/optional]]})

(defn- read-setting [entry-keyword]
  "Returns a configuration entry, set by ether a command-line parameter or environment variable.
  While all configuration entries are set using the 'visualreview-' prefix, users of this API should omit this prefix."
  (when (contains? config-schema entry-keyword)
    ((keyword (str config-env-prefix (name entry-keyword))) env)))

(defn- settings []
  (into {} (for [k (keys config-schema)] [k (read-setting k)])))

(defn parsed-settings []
  (v/validate config-schema (settings)))
