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

(ns com.xebia.visualreview.logging
  (:import [ch.qos.logback.classic Level Logger]
           [org.slf4j LoggerFactory]))

(def logger ^Logger (LoggerFactory/getLogger "visualreview-server"))

(defn set-log-level! [lvl]
  (case lvl
    :trace (.setLevel logger Level/TRACE)
    :debug (.setLevel logger Level/DEBUG)
    :info (.setLevel logger Level/INFO)
    :warn (.setLevel logger Level/WARN)
    :error (.setLevel logger Level/ERROR)
    (throw (IllegalArgumentException. (format "Invalid log level: %s" lvl)))))

;; These log macros are used to abstract the logging used in the project from
;; the logging library. These are macros so that the correct namespace from which
;; the message originated is preserved

(defmacro trace [& msgs]
  `(.trace logger (print-str ~@msgs)))

(defmacro debug [& msgs]
  `(.debug logger (print-str ~@msgs)))

(defmacro info [& msgs]
  `(.info logger (print-str ~@msgs)))

(defmacro warn [& msgs]
  `(.warn logger (print-str ~@msgs)))

(defmacro error [& msgs]
  `(.error logger (print-str ~@msgs)))
