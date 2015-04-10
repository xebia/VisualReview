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
  (:require [taoensso.timbre :as timbre]))

(def log-lvl-colors {:debug :cyan
                     :info  :yellow
                     :warn  :purple
                     :error :red})

(defn- dev-log-msg-fn
  "<yyyy-MMM-dd> <hh:mm:ss> [LEVEL] - <message> <throwable>
   LEVEL will be shorted to T, D, I, W, E, F and R
   Colors used:
   :debug - cyan
   :info  - yellow
   :warn  - purple
   :error - red"
  [{:keys [level throwable message timestamp]}]
  (let [lvl-char (-> level name first Character/toUpperCase)]
    (timbre/color-str
      (log-lvl-colors level :reset)
      (format "%s %s - %s%s"
              timestamp lvl-char (or message "")
              (or (timbre/stacktrace throwable "\n") "")))))

(def dev-logging-config {:fmt-output-fn     dev-log-msg-fn
                         :timestamp-pattern "yyyy-MM-dd HH:mm:ss"})

(defn set-log-level! [lvl]
  (if (#{:trace :debug :info :warn :error :fatal :report} lvl)
    (timbre/set-level! lvl)
    (throw (IllegalArgumentException. (format "Invalid log level: %s" lvl)))))

(defn set-log-msg-format!
  ([] (set-log-msg-format! :dev))
  ([env]
   (case env
     :dev (timbre/merge-config! dev-logging-config))))

(defmacro with-updated-conf [config-map & body]
  `(timbre/with-logging-config (merge ~(deref timbre/config) dev-logging-config ~config-map)
     ~@body))

;; These log macros are used to abstract the logging used in the project from
;; the logging library. These are macros so that the correct namespace from which
;; the message originated is preserved

(defmacro trace [& msgs]
  `(timbre/log :trace ~@msgs))

(defmacro debug [& msgs]
  `(timbre/log :debug ~@msgs))

(defmacro info [& msgs]
  `(timbre/log :info ~@msgs))

(defmacro warn [& msgs]
  `(timbre/log :warn ~@msgs))

(defmacro error [& msgs]
  `(timbre/log :error ~@msgs))

(defmacro fatal [& msgs]
  `(timbre/log :fatal ~@msgs))

(defmacro report [& msgs]
  `(timbre/log :report ~@msgs))
