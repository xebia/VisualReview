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
  (:require [slingshot.slingshot :as ex]
            [com.xebia.visualreview.logging :as log]))

(defn get-message-from-object-or-exception
  [object-or-exception]
  (if (instance? Exception object-or-exception)
    (.getMessage object-or-exception)
    (:message object-or-exception)))

(defn throw-service-exception [message code]
  (ex/throw+ {:type :service-exception :code code :message message}))

(defn rethrow-as-service-exception [object-or-exception message code]
  (let [exception-msg (get-message-from-object-or-exception object-or-exception)]
    (throw-service-exception (format message exception-msg) code)))

(defmacro attempt
  "Attempts to execute the given form. If the form throws an exception (of either the Java or slingshot kind),
  it will return a slingshot exception with type 'service-exception', with the given error message and code.

  Parameter err-msg may contain '%s', which will be replaced by the original exception's message. For example:
  (attempt (something-that-triggers-an-exception) \"Could not do the thing: %s\" 1234)"
  [form err-msg err-code]
  `(ex/try+
     ~form
     (catch Object o#
       (let [exception-msg# (get-message-from-object-or-exception o#)]
         (do
           (log/debug (str "An error has occured and was caught as a service exception: " exception-msg#))
           (throw-service-exception (format ~err-msg exception-msg#) ~err-code))))))

(defmacro assume
  "When the given form returns a falsy value, assume will throw a service exception with the given error message and code.
  'Assume' is primarily intended for easy sanity checking of input values for forms in the service layer."
  [form err-msg err-code]
  `(when (not ~form) (throw-service-exception ~err-msg ~err-code)))