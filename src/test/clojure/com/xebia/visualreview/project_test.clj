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

(ns com.xebia.visualreview.project-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.project :as project]
            [com.xebia.visualreview.persistence.util :as putil]
            [com.xebia.visualreview.test-util :refer :all]))

(deftest get-project-by-name
  (with-mock [putil/query-single {:id 1234}]
    (is (= {:id 1234} (project/get-project-by-name {} "some name")) "retrieves a project from the database"))
  (with-mock [putil/query-single nil]
    (is (nil? (project/get-project-by-name {} "some name")) "returns nil when the project doesn't exist")))
