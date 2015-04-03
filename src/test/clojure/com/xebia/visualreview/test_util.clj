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

(ns com.xebia.visualreview.test-util
  (:require [slingshot.support]))

(defn slingshot-exception
  ;TODO: Revisit when Midje has been phased out
  "Creates a slingshot Throwable object so it can be used in Midje's =throws=> prerequisite.
  Credits go to from http://stackoverflow.com/questions/17069584/why-cant-i-use-midge-to-mock-a-function-that-throws-using-slingshots-throw."
  [exception-map]
  (slingshot.support/get-throwable
    (slingshot.support/make-context exception-map
      (str "throw+: " map) nil (slingshot.support/stack-trace))))

(defn service-exception? [ex]
  (and (map? ex)
       (contains? ex :type)
       (contains? ex :code)
       (contains? ex :message)
       (= (:type ex) :service-exception)))

(defmacro with-mock [bindings body]
  (assert (even? (count bindings)) "with-mock requires an even number of arguments for its binding form")
  (let [fns (take-nth 2 bindings)
        rvals (map (fn [rval] `(fn [& _#] ~rval)) (take-nth 2 (rest bindings)))
        pairs (interleave fns rvals)]
    `(with-redefs [~@pairs]
       ~body)))