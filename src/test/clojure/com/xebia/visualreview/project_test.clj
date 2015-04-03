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
  (:require [midje.sweet :refer :all]
            [com.xebia.visualreview.project :as project]
            [com.xebia.visualreview.persistence.util :as putil]))

(facts "Project service"
       (facts "get-project-by-name"
              (fact "retrieves a project from the database"
                    (project/get-project-by-name {} "some name")
                    => {:id 1234}
                    (provided
                      (putil/query-single anything anything anything anything) => {:id 1234}))
              (fact "trhrows a  nil when the project does not exist"
                    (project/get-project-by-name {} "some name")
                    => nil
                    (provided
                      (putil/query-single anything anything anything anything) => nil))))
