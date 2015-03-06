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

(ns com.xebia.visualreview.service-util
  (:require [slingshot.slingshot :as ex]))

(defn rethrow [message code]
  (ex/throw+ {:type ::service-exception :code code :message message}))

(defmacro attempt
  "Attempts to execute the given form. If the form throws an exception (of either the Java or slingshot kind),
  it will return a slingshot exception with type 'service-exception', with the given error message and code.

  Parameter err-msg may contain '%s', which will be replaced by the original exception's message. For example:
  (attempt (something-that-triggers-an-exception) \"Could not do the thing: %s\" 1234)"
  [form err-msg err-code]
  `(ex/try+
     ~form
     (catch Exception e#
       (rethrow (format ~err-msg (.getMessage e#)) ~err-code))
     (catch Object o#
       (rethrow (format ~err-msg (:message o#)) ~err-code))))
