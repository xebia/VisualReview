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
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.xebia.visualreview.validation :as v])
  (:import [java.io FileNotFoundException File]))

(def ^{:private true
       :doc     "Expected keys and related validators for configuration data"}config-schema
  {:server-port         [Number []]
   :db-uri              [String []]
   :db-user             [String []]
   :db-password         [String [::v/optional]]
   :screenshots-dir     [String [::v/optional]]
   :enable-http-logging [Boolean [::v/optional]]})

(def default-config {:server-port         "7000"
                     :screenshots-dir     "screenshots"
                     :enable-http-logging false})

(defonce env {})

(def ^:private config-file "./config.edn")
(defn init!
  "Reads the config.edn resource file on the classpath (or the given arg). Parses it and sets the env var"
  ([] (init! config-file))
  ([cfg]
   (let [^File config-file (io/file cfg)]
     (if (.canRead config-file)
      (let [conf (merge default-config (-> config-file slurp edn/read-string))]
       (alter-var-root #'env (fn [_] (v/validate config-schema conf))))
     (throw (FileNotFoundException. (format "The configuration file %s could not be found" cfg)))))))
