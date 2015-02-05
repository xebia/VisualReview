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
  (:require [taoensso.timbre :as timbre]
            [com.xebia.visualreview.persistence.database :as db]
            [clojure.java.jdbc :as j])
  (:import [java.sql SQLException]))

(defn wrap-exception [f]
  (fn [request]
    (try (f request)
         (catch Exception e
           (do
             (timbre/log :error e (str "A request triggered an unhandled exception, as a result the request was met with a HTTP status 500 response."))
             {:status 500
              :headers {}
              :body "Internal error occurred"})))))

(defn wrap-tx [handler]
  (fn [req]
    (j/with-db-transaction [conn db/conn]
      (try
        (handler (assoc req :tx-conn conn))
        (catch SQLException e
          (timbre/log :error e "SQLException caught.")
          (throw e))
        (catch Exception e
          (timbre/log :error e "Exception occured whilst inside transaction. Transaction was rolled back.")
          (j/db-set-rollback-only! conn)
          (throw e))))))