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

(ns com.xebia.visualreview.resource.util
  (:require [liberator.core :as liberator]
            [liberator.representation :as representation]
            [cheshire.core :as json]
            [com.xebia.visualreview.util :as util])
  (:import [com.fasterxml.jackson.core JsonParseException]))

(def ^:private camelize-map (util/map-keys util/camelize))

(def hyphenize-request (util/map-keys util/hyphenize))

(defn- camelize-response [x]
  (cond
    (vector? x) (mapv camelize-response x)
    (map? x) (reduce (fn [acc [k v]] (conj acc [k (camelize-response v)]))
                     {}
                     (camelize-map x))
    :else x))

(defn get-request? [ctx] (= (-> ctx :request :request-method) :get))
(defn delete-request? [ctx] (= (-> ctx :request :request-method) :delete))
(defn put-or-post-request? [ctx] (#{:put :post} (get-in ctx [:request :request-method])))
(defn content-type [ctx] (get-in ctx [:request :headers "content-type"]))

(defn json-resource [& args]
  (apply liberator/resource
         :available-media-types ["application/json"]
         :known-content-type? (fn [ctx] (if (put-or-post-request? ctx)
                                          (re-find #"^application/json" (content-type ctx))
                                          true))
         :malformed? (fn [{{body :body} :request}]
                       (try
                         [false {:parsed-json (-> body
                                                   slurp
                                                   (json/parse-string true))}]
                         (catch JsonParseException _
                           [true {::message "Malformed JSON request body"}])))
         :handle-malformed ::message
         :as-response (fn [d ctx] (representation/as-response (camelize-response d) ctx))
         args))

(defn tx-conn [ctx]
  (-> ctx :request :tx-conn))

(defn parse-longs [xs] (mapv #(Long/parseLong %) xs))

(defmacro handle-invalid [validation & cs]
  (assert (even? (count cs)))
  `(let [err# (:error ~validation)]
     (case (:subtype err#)
       ~@cs
       (:message err#))))
