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

(ns com.xebia.visualreview.service-util-test
  (:use [midje.sweet])
  (:require [com.xebia.visualreview.service-util :as util]
            [slingshot.slingshot :as ex])
  (:import (clojure.lang IExceptionInfo)))

(defn is-service-exception? [message code]
  (chatty-checker [exception] (and
                    (= :service-exception (get-in (.getData exception) [:object :type]))
                    (= message  (get-in (.getData exception) [:object :message]))
                    (= code  (get-in (.getData exception) [:object :code])))))

(defn throw-java-exception-with-message [message]
  (throw (Exception. message)))

(defn throw-slingshot-exception-with-message [message]
  (ex/throw+ {:type :my-exception :message message}))

(defn slingshot-exception
  "Creates a slingshot Throwable object so it can be used in Midje's =throws=> prerequisite.
  Credits go to from http://stackoverflow.com/questions/17069584/why-cant-i-use-midge-to-mock-a-function-that-throws-using-slingshots-throw."
  [exception-map]
  (slingshot.support/get-throwable (slingshot.support/make-context exception-map (str "throw+: " map) nil (slingshot.support/stack-trace))))

(facts "service-util"
       (facts "attempt"
              (facts "throws a slingshot exception of type 'service-exception'"
                     (fact "when the form throws a java exception"
                           (util/attempt (throw-java-exception-with-message "my exception message") "something went wrong: %s" :1234)
                           => (throws IExceptionInfo (is-service-exception? "something went wrong: my exception message", :1234)))
                     (fact "when the form throws a slingshot exception"
                           (util/attempt (throw-slingshot-exception-with-message "my exception message") "something went wrong: %s" :1234)
                           => (throws IExceptionInfo (is-service-exception? "something went wrong: my exception message", :1234))))

              (fact "returns the form's value when no exception occured"
                    (util/attempt "my return value" "errormessage" :errorcode)
                    => "my return value"))

       (facts "assume"
              (fact "throws a service exception when the form returns false"
                    (util/assume (= (+ 2 2) 5) "two plus two is not five" :225))
                    => (throws IExceptionInfo (is-service-exception? "two plus two is not five" :225))

              (fact "does not throw an exception when the form returns true"
                    (util/assume (= (+ 2 2) 4) "two plus two should be five" :225)
                    => nil)))
