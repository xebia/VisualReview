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

(ns com.xebia.visualreview.itest.api-version-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.api-test :as api]
            [com.xebia.visualreview.mock :as mock]
            [com.xebia.visualreview.itest-util :refer [start-server stop-server]]))

(use-fixtures :each mock/logging-fixture mock/rebind-db-spec-fixture mock/setup-screenshot-dir-fixture mock/setup-db-fixture mock/test-server-fixture)

(deftest api-version

  (testing "API version"
    (is (= (:body (api/get-api-version)) "1"))))
