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

(ns com.xebia.visualreview.resource.baseline
  (:require [liberator.core :refer [resource]]
            [com.xebia.visualreview.resource.util :refer :all]
            [com.xebia.visualreview.validation :as v]
            [com.xebia.visualreview.service.analysis :as p]
            [slingshot.slingshot :as ex]
            [com.xebia.visualreview.service.baseline :as baseline]))

(def ^:private create-branch-schema
  {:baselineNode  [Number []]
   :newBranchName [String [::v/non-empty]]})

(def create-branch
  (json-resource
    :allowed-methods [:post]
    :processable? (fn [ctx]
                    (let [[suite-id] (parse-longs [(-> ctx :request :params :suite-id)])
                          v (v/validations create-branch-schema (:parsed-json ctx))]
                      (if (:valid? v)
                        {::new-branch-name (-> v :data :newBranchName)
                         ::baseline-node (-> v :data :baselineNode)
                         ::suite-id        suite-id}
                        [false {::error-msg (handle-invalid v
                                              ::v/non-empty "Branch name may not be empty")}])))
    :handle-unprocessable-entity ::error-msg
    :exists? (fn [{node ::baseline-node :as ctx}]
               (:id (baseline/get-baseline-node (tx-conn ctx) node)))
    :can-post-to-missing? false
    :post! (fn [{baseline-node ::baseline-node new-branch-name ::new-branch-name suite-id ::suite-id :as ctx}]
             (ex/try+
               (baseline/create-baseline-branch! (tx-conn ctx) baseline-node new-branch-name)
               {::result (baseline/get-baseline-branch (tx-conn ctx) suite-id new-branch-name)
                ::new?   true}
               (catch [:subtype ::p/unique-constraint-violation] _
                 {::error "A branch with that name already exists"
                  ::new? false})))
    :new? ::new?
    :respond-with-entity? true
    :handle-ok ::error
    :handle-created ::result))
