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

(ns com.xebia.visualreview.middleware
  (:require [slingshot.slingshot :as s]
            [clojure.tools.logging :as log]
            [com.xebia.visualreview.service.persistence.database :as db]
            [clojure.java.jdbc :as j]
            [clojure.string :as string]
            [com.xebia.visualreview.config :as config])
  (:import [java.sql SQLException]
           (java.io ByteArrayInputStream)))

(defn wrap-exception [f]
  (fn [request]
    (s/try+
      (f request)
      (catch [:type :service-exception] {:keys [message code]}
        (do
          (log/error (str "A service exception occured, code " code ", message " message))
          {:status  500
           :headers {}
           :body    (str "Internal service error occured")}))
      (catch SQLException e
        (do
          (log/error (str "An error occured involving the database: " (.getMessage e)) e)
          {:status 500
           :headers {}
           :body "Internal database error occured"}))
      (catch Exception e
        (do
          (log/error (str "A request triggered an unhandled exception, as a result the request was met with a HTTP status 500 response." (.getMessage e)) e)
          {:status  500
           :headers {}
           :body    "Internal error occured"})))))

(defn wrap-tx [handler]
  (fn [req]
    (j/with-db-transaction [conn db/conn]
      (try
        (handler (assoc req :tx-conn conn))
        (catch SQLException e
          (log/error "SQLException caught.")
          (throw e))
        (catch Exception e
          (log/error "Exception occured whilst inside transaction. Transaction was rolled back.")
          (j/db-set-rollback-only! conn)
          (throw e))))))

(defn- http-headers-to-string [headers]
  (string/join "\n" (map (fn [[k v]] (str (name k) ": " v)) headers)))

(defn http-logger [handler]
  (do
  (if (:enable-http-logging config/env)
    (do
      (log/info "HTTP logging enabled")
      (fn [request]
        (let [request-body (slurp (:body request))
              request-headers (http-headers-to-string (:headers request))
              request-log (str "Request: \n"
                               "# uri: " (:uri request) "\n"
                               "# method: " (:request-method request) "\n"
                               "# content-type: " (:content-type request) "\n"
                               "# headers: \n" request-headers "\n"
                               "# body: \n" request-body)
              response (handler (assoc request :body (ByteArrayInputStream. (.getBytes request-body "UTF-8"))))
              response-headers (http-headers-to-string (:headers response))
              response-log (str "Response: \n"
                                "# headers: \n" response-headers "\n"
                                "# body: \n" (:body response) "\n")]
          (do

            (log/info request-log)
            (log/info response-log)
            response))))
  (fn [request]
    (handler request)))))

