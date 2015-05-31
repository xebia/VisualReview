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

(ns com.xebia.visualreview.service.persistence.database
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io])
  (:import [com.mchange.v2.c3p0 PooledDataSource ComboPooledDataSource]))

(defn pooled-datasource
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMaxPoolSize (:max-pool-size spec))
               (.setMinPoolSize (:min-pool-size spec))
               (.setInitialPoolSize (:init-pool-size spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               (.setAcquireRetryAttempts 3)
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(defonce conn {})

(defn run-init-script [conn]
  (j/execute! conn [(slurp (io/resource "dbscripts/h2/1.sql"))]))

(defn init! [db-uri user pass]
  {:pre [db-uri user]}
  (let [db-spec {:classname      "org.h2.Driver"
                 :subprotocol    "h2"
                 :subname        db-uri
                 :user           user
                 :password       pass
                 :init-pool-size 1
                 :min-pool-size  1
                 :max-pool-size  1}
        conn (alter-var-root #'conn (constantly (pooled-datasource db-spec)))]
    (do
      (run-init-script conn)
      conn)))

(defn close-connection []
  (when-let [db-conn ^PooledDataSource (:datasource conn)]
    (.close db-conn)))
