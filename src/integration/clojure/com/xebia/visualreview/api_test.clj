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

(ns com.xebia.visualreview.api-test
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [com.xebia.visualreview.itest-util :as test]))

(def ^:private default-opts {:content-type        :json
                             :as                  :json
                             :throw-exceptions    false
                             :decode-body-headers true})

(def server-root (str "http://localhost:" test/test-server-port "/"))
(def api-root (str server-root "api/"))
(defn endpoint [& parts]
  (str api-root (apply str (interpose \/ parts))))

(defn get-api-version []
  (http/get (str server-root "api/version")))

(defn get-projects []
  (http/get (endpoint "projects") default-opts))

(defn put-project! [params]
  (http/put (endpoint "projects") (merge default-opts {:body (json/generate-string params)})))

(defn get-project [project-id]
  (http/get (endpoint "projects" project-id) (merge default-opts {:as :json})))

(defn delete-project! [project-id]
  (http/delete (endpoint "projects" project-id) nil))

(defn post-run! [project-name suite-name]
  (dissoc (http/post (endpoint "runs") (merge default-opts {:body (json/generate-string {:projectName project-name
                                                                                         :suiteName suite-name})})) :headers))

(defn get-runs [project-name suite-name]
  (dissoc (http/get (endpoint "runs") (merge default-opts {:query-params {:projectName project-name
                                                                          :suiteName suite-name}})) :headers))
(defn get-run [run-id]
  (http/get (endpoint "runs" run-id) default-opts))

(defn delete-run! [run-id]
  (http/delete (endpoint "runs" run-id) nil))

(defn get-suites [project-id]
  (dissoc (http/get (endpoint "projects" project-id "suites") default-opts) :headers))

(defn upload-screenshot! [run-id {:keys [file meta screenshotName properties compareSettings]}]
  (let [file (io/as-file (io/resource file))]
    (dissoc (http/post (endpoint "runs" run-id "screenshots")
                       (merge (dissoc default-opts :content-type)
                              {:multipart [{:name "file" :content file :mime-type "image/png"}
                                           {:name "screenshotName" :content screenshotName}
                                           {:name "meta" :content (json/generate-string meta)}
                                           {:name "compareSettings" :content (json/generate-string compareSettings)}
                                           {:name "properties" :content (json/generate-string properties)}]}))
            :headers)))

(defn http-get [path]
  (http/get (str server-root path) {:throw-exceptions false}))

(defn get-image [image-id]
  (http/get (endpoint "image" image-id) {:throw-exceptions false}))

(defn get-analysis [run-id]
  (dissoc (http/get (endpoint "runs" run-id "analysis") default-opts) :headers))

(defn perform-cleanup []
  (dissoc (http/post (endpoint "cleanup") default-opts) :headers))

(defn- find-diff-with-after-image-id [diffs image-id]
  (first (filter (fn [diff] (= (:imageId (:after diff)) image-id)) diffs)))

(defn update-diff-status! [run-id diff-id status]
  (dissoc (http/post (endpoint "runs" run-id "analysis" "diffs" diff-id)
                     (merge default-opts {:body (json/generate-string {:status status})}))
          :headers))

(defn update-diff-status-of-screenshot [run-id screenshot-image-id new-status]
  (let [analysis (:body (get-analysis run-id))
        diff (find-diff-with-after-image-id (:diffs analysis) screenshot-image-id)]
    (update-diff-status! run-id (:id diff) new-status)))

