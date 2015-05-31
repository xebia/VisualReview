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

(ns com.xebia.visualreview.middleware-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.middleware :as middleware]
            [com.xebia.visualreview.service.service-util :as service-util]
            [com.xebia.visualreview.test-util :refer :all])
  (:import (java.sql SQLException)))

(deftest wrap-exception
  (is (=  {:status 500
           :headers {}
           :body "Internal error occured"}
          ((middleware/wrap-exception (fn [_] (throw (new Exception "my message")))) {})) "Converts unhandled exceptions to a 500 response")
  (is (=  {:status 500
           :headers {}
           :body "Internal database error occured"}
          ((middleware/wrap-exception (fn [_] (throw (new SQLException "my sql message")))) {})) "Converts unhandled database exceptions to a 500 response")
  (is (=  {:status 500
           :headers {}
           :body "Internal service error occured"}
          ((middleware/wrap-exception (fn [_] (service-util/throw-service-exception "my message" 1234))) {})) "Converts unhandled service exceptions to a 500 response"))

