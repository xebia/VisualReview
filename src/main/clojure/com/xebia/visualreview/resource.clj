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

(ns com.xebia.visualreview.resource
  (:require [liberator.core :refer [resource defresource]]
            [cheshire.core :as json]
            [slingshot.slingshot :as ex]
            [com.xebia.visualreview.validation :as v]
            [com.xebia.visualreview.resource.util :refer :all]
            [com.xebia.visualreview.service.analysis :as analysis]
            [com.xebia.visualreview.service.analysis.core :as analysisc]
            [com.xebia.visualreview.io :as io]
            [com.xebia.visualreview.service.image :as image]
            [com.xebia.visualreview.service.screenshot :as screenshot]
            [com.xebia.visualreview.service.project :as project]
            [com.xebia.visualreview.service.suite :as suite]
            [com.xebia.visualreview.service.run :as run]
            [com.xebia.visualreview.service.cleanup :as cleanup]
            [com.xebia.visualreview.service.baseline :as baseline])
  (:import [java.util Map]))

;;;;;;;;; Projects ;;;;;;;;;;;
(def ^:private project-schema
  {:name [String [::v/non-empty]]})

(defn project-resource []
  (json-resource
    :allowed-methods [:get :put]
    :processable? (fn [ctx]
                    (or (get-request? ctx)
                        (let [v (v/validations project-schema (:parsed-json ctx))]
                          (if (:valid? v)
                            {::project-name (-> v :data :name)}
                            [false {::error-msg (handle-invalid v
                                                  ::v/non-empty "Name can not be empty")}]))))
    :handle-unprocessable-entity ::error-msg
    :exists? (fn [ctx]
               (or (get-request? ctx)
                   (when-let [project-id (project/get-project-by-name (tx-conn ctx) (::project-name ctx) :id)]
                     {::project-id project-id})))
    :conflict? (fn [ctx] (::project-id ctx))
    :handle-conflict (fn [ctx] (format "A project with name: '%s' already exists." (::project-name ctx)))
    :put! (fn [ctx]
            (let [new-project-id (project/create-project! (tx-conn ctx) (::project-name ctx))
                  project (suite/get-suites-by-project-id (tx-conn ctx) new-project-id)]
              {::project project}))
    :handle-created ::project
    :handle-ok (fn [ctx] (project/get-projects (tx-conn ctx)))))

(defn project-by-id [project-id]
  (json-resource
    :allowed-methods [:get :delete]
    :processable? (fn [_]
                    (try
                      (when-let [project-id-long (Long/parseLong project-id)]
                        {::project-id project-id-long})
                      (catch NumberFormatException _ false)))
    :exists? (fn [ctx]
               (try
                 (when-let [project (suite/get-suites-by-project-id (tx-conn ctx) (::project-id ctx))]
                   {::project project})
                 (catch NumberFormatException _)))
    :delete! (fn [ctx]
               (let [result (project/delete-project! (tx-conn ctx) (::project-id ctx))]
                 (do
                   (cleanup/cleanup-orphans! (tx-conn ctx))
                   {::project-deleted result})))
    :delete-enacted? (fn [ctx]
                       (::project-deleted ctx))
    :handle-ok ::project))

;;;;;;;;;; Suites ;;;;;;;;;;;
(defn suites-resource [project-id]
  (json-resource
    :allowed-methods [:get]
    :exists? (fn [ctx]
               (try
                 (let [[project-id] (parse-longs [project-id])]
                   (and (project/get-project-by-id (tx-conn ctx) project-id)
                        (when-let [suites (suite/get-suites (tx-conn ctx) project-id)]
                          {::suites suites})))
                 (catch NumberFormatException _)))
    :handle-ok ::suites))

(defn suite-resource [project-id suite-id]
  (json-resource
    :allowed-methods [:get :delete]
    :exists? (fn [ctx]
               (try
                 (let [[project-id suite-id] (parse-longs [project-id suite-id])]
                   (when-let [suite (suite/get-full-suite (tx-conn ctx) project-id suite-id)]
                     {::suite suite}))
                 (catch NumberFormatException _)))
    :delete! (fn [ctx]
               (let [result (suite/delete-suite! (tx-conn ctx) (:id (::suite ctx)))]
                 (do
                   (cleanup/cleanup-orphans! (tx-conn ctx))
                   {::suite-deleted result})))
    :delete-enacted? (fn [ctx]
                       (::suite-deleted ctx))
    :handle-ok ::suite))

(defn suite-status-resource [project-id suite-id]
  (resource
    :available-media-types ["text/plain"]
    :allowed-methods [:get]
    :exists? (fn [ctx]
               (let [[project-id suite-id] (parse-longs [project-id suite-id])
                     suite-status (suite/get-suite-status (tx-conn ctx) project-id suite-id)]
                 (if (= "empty" suite-status)
                   false
                   {:suite-status suite-status})))
    :handle-ok (fn [ctx]
                 (:suite-status ctx))))

;;;;;;;;;;; Runs ;;;;;;;;;;;
(def ^:private run-create-schema
  {:branchName [String []]
   :projectName [String []]
   :suiteName   [String [::v/non-empty]]})

(defn run-resource [run-id]
  (json-resource
    :allowed-methods [:get :delete]
    :exists? (fn [ctx]
               (try
                 (let [[run-id] (parse-longs [run-id])]
                   (when-let [run (run/get-run (tx-conn ctx) run-id)]
                     {::run run}))
                 (catch NumberFormatException _)))
    :delete! (fn [ctx]
               (let [result (run/delete-run! (tx-conn ctx) (:id (::run ctx)))]
               (do
                 (cleanup/cleanup-orphans! (tx-conn ctx))
                 {::run-deleted result })))
    :delete-enacted? (fn [ctx]
                       (::run-deleted ctx))
    :handle-ok ::run))

(def runs-resource
  (json-resource
    :allowed-methods [:get :post]
    :processable? (fn [ctx]
                    (let [v (v/validations run-create-schema (if (get-request? ctx)
                                                               (-> ctx :request :params)
                                                               (:parsed-json ctx)))]
                      (if (:valid? v)
                        {::data (hyphenize-request (:data v))}
                        [false {::error-msg (handle-invalid v
                                              ::v/non-empty "Suite name can not be empty")}])))
    :handle-unprocessable-entity ::error-msg
    :exists? (fn [ctx]
                 (if (get-request? ctx)
                   (when-let [suite (suite/get-suite-by-name (tx-conn ctx) (-> ctx ::data :project-name) (-> ctx ::data :suite-name))]
                     (let [runs (:runs (suite/get-full-suite (tx-conn ctx) (:project-id suite) (:id suite)))]
                       {::runs runs ::suite suite}))
                   (when-let [suite
                              (and
                                (or
                                  (baseline/get-baseline-branch-by-suitename (tx-conn ctx) (-> ctx ::data :suite-name) (-> ctx ::data :branch-name))
                                  (baseline/create-baseline-branch! (tx-conn ctx) 2 (-> ctx ::data :branch-name))
                                  true)
                                (or
                                  (project/get-project-by-name (tx-conn ctx) (-> ctx ::data :project-name))
                                  (project/create-project! (tx-conn ctx) (-> ctx ::data :project-name)))
                                (or
                                  (suite/get-suite-by-name (tx-conn ctx) (-> ctx ::data :project-name) (-> ctx ::data :suite-name))
                                  (suite/get-suite-by-id (tx-conn ctx)
                                                                         (suite/create-suite-for-project! (tx-conn ctx) (-> ctx ::data :project-name) (-> ctx ::data :suite-name))))
                              )]
                   {::suite suite})))
    :can-post-to-missing? false
    :post! (fn [ctx]
             (let [new-run-id (run/create-run! (tx-conn ctx) (:id (::suite ctx)) (-> ctx ::data :branch-name))
                   run (run/get-run (tx-conn ctx) new-run-id)]
               {::run run}))
    :handle-created ::run
    :handle-ok ::runs))

;;;;;;;;;;; Screenshots ;;;;;;;;;;;;;;
(defn- update-diff-status! [conn {:keys [id before after status]} new-status]
  (case [status new-status]
    (["accepted" "pending"] ["accepted" "rejected"]) (baseline/set-baseline! conn id after before)
    (["pending" "accepted"] ["rejected" "accepted"]) (baseline/set-baseline! conn id before after)
    :no-op)
  (analysis/update-diff-status! conn id new-status))

;; The screenshot resource has been split up into separate handler for each http method.
(def ^:private upload-screenshot-schema
  {:file              [Map [::v/screenshot]]
   :screenshotName    [String []]
   :meta              [Map [::v/screenshot-meta]]
   :mask              [Map [::v/optional ::v/screenshot-mask]]
   :properties        [Map [::v/screenshot-meta]]})

(defn- update-screenshot-path [screenshot]
  (update-in screenshot [:path] #(str "/screenshots/" % "/" (:id screenshot) ".png")))

(defn get-screenshots [run-id]
  (json-resource
    :allowed-methods [:get]
    :exists? (fn [ctx]
               (try (let [[run-id] (parse-longs [run-id])
                          run (run/get-run (tx-conn ctx) run-id)]
                      (when run {::run run}))
                    (catch NumberFormatException _)))
    :handle-ok (fn [ctx] (let [screenshots (screenshot/get-screenshots-by-run-id (tx-conn ctx) (-> ctx ::run :id))]
                           (mapv update-screenshot-path screenshots)))))

(defn- proces-diff [conn run-id before-file after-file before-id after-id mask]
  (let [analysis (analysis/get-analysis conn run-id)
        diff-report (analysisc/generate-diff-report before-file after-file mask)
        diff-file-id (image/insert-image! conn (:diff diff-report))
        mask-file-id (image/insert-image! conn (:mask diff-report))
        new-diff-id (analysis/save-diff! conn diff-file-id mask-file-id before-id after-id (:percentage diff-report) (:id analysis))]
    (do
      (.delete (:diff diff-report))
      (analysis/get-diff conn run-id new-diff-id))))

(defn- process-screenshot [conn suite-id run-id branch-name screenshot-name properties meta mask {:keys [tempfile]}]
  (let [screenshot-id (screenshot/insert-screenshot! conn run-id screenshot-name properties meta tempfile)
        screenshot (screenshot/get-screenshot-by-id conn screenshot-id)
        baseline-screenshot (baseline/get-baseline-screenshot conn suite-id branch-name screenshot-name properties)
        before-file (when baseline-screenshot (io/get-file (image/get-image-path conn (:image-id baseline-screenshot))))
        diff (proces-diff conn run-id before-file tempfile (:id baseline-screenshot) screenshot-id mask)]
    (when (and baseline-screenshot (zero? (:percentage diff)))
      (update-diff-status! conn diff "accepted"))
    screenshot))

(defn upload-screenshot [run-id]
  (json-resource
    :allowed-methods [:post]
    :known-content-type? #(re-find #"multipart" (content-type %))
    :malformed? false
    :processable? (fn [ctx]
                    (let [v (v/validations upload-screenshot-schema (-> ctx :request :params
                                                                        (update-in [:meta] json/parse-string true)
                                                                        (update-in [:mask] json/parse-string true)
                                                                        (update-in [:properties] json/parse-string true)))]
                      (if (:valid? v)
                        (let [data (hyphenize-request (:data v))
                              run (run/get-run (tx-conn ctx) (Long/parseLong run-id))]
                          (if (or (= (:status run) "running") (nil? run))
                            {::data data ::run run}
                            [false {::error-msg (format "Run status must be 'running' to upload screenshots. Status is: %s" (:status run))}]))
                        [false {::error-msg (handle-invalid v
                                              ::v/screenshot "'file' is not a valid PNG file"
                                              ::v/screenshot-meta "Invalid meta or properties data. Are the values either strings or numbers?")}])))
    :handle-unprocessable-entity ::error-msg
    :exists? ::run
    :can-post-to-missing? false
    :post! (fn [ctx]
             (ex/try+
               (let [{:keys [meta mask file properties screenshot-name]} (::data ctx)
                     {suite-id :suite-id run-id :id} (::run ctx)
                     branch-name (run/get-branch-name-by-run-id (tx-conn ctx) run-id)
                     screenshot (process-screenshot (tx-conn ctx) suite-id run-id branch-name screenshot-name properties meta mask file)]
                 {::screenshot screenshot ::new? true})
               (catch [:type :service-exception :code ::screenshot/screenshot-cannot-store-in-db-already-exists] _
                 {::screenshot {:error              "Screenshot with identical name and properties was already uploaded in this run"
                                :conflicting-entity (select-keys (::data ctx) [:meta :properties :screenshot-name])}
                  ::new?       false})))
    :new? ::new?
    :respond-with-entity? true
    :handle-created ::screenshot
    :handle-ok ::screenshot))

(defn screenshots-resource [run-id]
  (fn [req]
    (if (get-request? {:request req})
      (get-screenshots run-id)
      (upload-screenshot run-id))))

;; Analysis
(defn- full-path [path id & {:keys [prefix] :or {prefix "/screenshots"}}]
  (str prefix "/" path "/" id ".png"))

(defn- transform-diff [diff]
  {:id         (:id diff)
   :before     (when (:before diff)
                 {:id             (:before diff)
                  :image-id       (:before-image-id diff)
                  :size           (:before-size diff)
                  :meta           (:before-meta diff)
                  :properties     (:before-properties diff)
                  :screenshotName (:before-name diff)})
   :after      {:id             (:after diff)
                :image-id       (:after-image-id diff)
                :size           (:after-size diff)
                :meta           (:after-meta diff)
                :properties     (:after-properties diff)
                :screenshotName (:after-name diff)}
   :status     (:status diff)
   :percentage (:percentage diff)
   :image-id   (:image-id diff)
   :mask-image-id   (:mask-image-id diff)
   })

(defn- transform-analysis [full-analysis]
  (update-in full-analysis [:diffs] #(mapv transform-diff %)))

;; Diff Status
(defn analysis-resource [run-id]
  (json-resource
    :allowed-methods [:get]
    :processable? (fn [ctx]
                    (try
                      (let [[run-id] (parse-longs [run-id])
                            analysis (analysis/get-full-analysis (tx-conn ctx) run-id)]
                        {::analysis analysis})
                      (catch NumberFormatException _)))
    :exists? (fn [ctx] (not (empty? (-> ctx ::analysis :analysis))))
    :handle-ok (comp transform-analysis ::analysis)))

(def ^:private update-diff-status-schema
  {:status [String [::v/diff-status]]})

(defn diff-status-resource [run-id diff-id]
  (json-resource
    :allowed-methods [:post]
    :processable? (fn [ctx]
                    (try
                      (let [[run-id diff-id] (parse-longs [run-id diff-id])
                            v (v/validations update-diff-status-schema (:parsed-json ctx))]
                        (if (:valid? v)
                          {::run-id run-id ::diff-id diff-id ::new-status (-> v :data :status)}
                          [false {::error-msg (handle-invalid v
                                                ::v/diff-status "'status' must be 'pending', 'accepted' or 'rejected'")}]))
                      (catch NumberFormatException _)))
    :handle-unprocessable-entity ::error-msg
    :exists? (fn [ctx]
               (when-let [diff (analysis/get-diff (tx-conn ctx) (::run-id ctx) (::diff-id ctx))]
                 {::diff diff}))
    :can-post-to-missing? false
    :post! (fn [ctx]
             ;; First checks to see if this diff has a baseline-screenshot. If it has, it updates the diff
             ;; Otherwise, if the new-status is "accepted", it sets the after-screenshot as the new baseline
             ;; if it was not set before. If the new-status is "pending" or "rejected" it removes the baseline
             ;; regardless of whether it existed before
             (let [baseline-screenshot (baseline/get-baseline-screenshot-by-diff-id (tx-conn ctx) diff-id)
                   after-id (-> ctx ::diff :after)]
               (when-not baseline-screenshot                ; new screenshot
                 (let [run (run/get-run (tx-conn ctx) run-id)
                       baseline-node (baseline/get-baseline-head (tx-conn ctx) (:suite-id run))]
                   (if (= (::new-status ctx) "accepted")    ; set as baseline-screenshot
                     (let [bl (baseline/get-bl-node-screenshot (tx-conn ctx) baseline-node after-id)]
                       (when (nil? bl)
                         (baseline/create-bl-node-screenshot! (tx-conn ctx) baseline-node after-id)))
                     (baseline/delete-bl-node-screenshot! (tx-conn ctx) baseline-node after-id)))))

             (update-diff-status! (tx-conn ctx) (::diff ctx) (::new-status ctx))
             {::updated-diff (analysis/get-diff (tx-conn ctx) (::run-id ctx) (::diff-id ctx))})
    :handle-created ::updated-diff))

(defn image [image-id]
  (resource
    :available-media-types ["image/png"]
    :allowed-methods [:get]
    :exists? (fn [ctx]
               (let [image-path (image/get-image-path (tx-conn ctx) image-id)]
                 (if (nil? image-path)
                   false
                   {:image-path image-path})))
    :handle-ok (fn [ctx]
                 (io/get-file (:image-path ctx)))))

;; Cleanup
(defn cleanup []
  (resource
    :allowed-methods [:post]
    :post! (fn [ctx] (cleanup/cleanup-orphans! (tx-conn ctx)))
    :handle-created (fn [_] (liberator.representation/ring-response
                           {:status 200}))))
