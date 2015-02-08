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
            [com.xebia.visualreview.test-util :as test]
            [clojure.java.io :as io]))

(def ^:private default-opts {:as                  :json
                             :throw-exceptions    false
                             :decode-body-headers true})

(def api-root (str "http://localhost:" test/test-server-port "/api/"))
(defn endpoint [path] (str api-root path))

(defn get-projects []
  (http/get (endpoint "projects") default-opts))

(defn put-project! [params]
  (http/put (endpoint "projects") (merge default-opts {:form-params params})))

(defn get-project [project-id]
  (http/get (endpoint (str "projects/" project-id)) (merge default-opts {:as :json})))

(defn post-run! [params]
  (dissoc (http/post (endpoint "runs") (merge default-opts {:form-params params})) :headers))

(defn get-runs [params]
  (dissoc (http/get (endpoint "runs") (merge default-opts {:query-params params})) :headers))

(defn get-run [run-id]
  (http/get (endpoint (str "runs/" run-id)) default-opts))

(defn get-suites [project-id]
  (dissoc (http/get (endpoint (str "projects/" project-id "/suites")) default-opts) :headers))

(defn- create-multipart-entries [part-name m]
  (reduce (fn [acc [k v]] (conj acc {:name (str part-name "[" (name k) "]") :content v})) [] m))
(defn upload-screenshot! [run-id {:keys [file meta screenshot-name]}]
  (let [file (io/as-file (io/resource file))
        metas (create-multipart-entries "meta" meta)]
    (dissoc (http/post (endpoint (str "runs/" run-id "/screenshots"))
                       (merge default-opts
                              {:multipart (into [{:name "file" :content file :mime-type "image/png"}
                                                 {:name "screenshot-name" :content screenshot-name}]
                                                metas)}))
            :headers)))