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

(ns com.xebia.visualreview.config-test
  (:require [midje.sweet :refer :all]
            [com.xebia.visualreview.config :refer :all]
            [environ.core :as environ]))

(def stub-env {:visualreview-port        1234
               :visualreview-db-uri      "//localhost:5432/testdb"
               :visualreview-db-user     "testuser"
               :visualreview-db-password "test123"
               :some-other-env-var       "some-value"})

(defmacro with-env [env & body]
  `(with-redefs [environ/env ~env]
     ~@body))

(facts "about parsed-settings"
  (fact "it validates and parses environment variables prefixed with 'visual-review-'. it retrieves only the fields defined in the config-schema"
    (with-env stub-env
      (parsed-settings)) => {:port        1234
                             :db-uri      "//localhost:5432/testdb"
                             :db-user     "testuser"
                             :db-password "test123"})
  (fact "the port should be a number or parseable to a number"
    (with-env (update-in stub-env [:visualreview-port] str)
      (:port (parsed-settings)) => 1234)
    (with-env (assoc stub-env :visualreview-port "Not a number")
      (parsed-settings) => (throws Exception #"not a number"))))
