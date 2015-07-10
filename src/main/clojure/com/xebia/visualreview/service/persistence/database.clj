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
            [clojure.java.io :as io]
            [com.xebia.visualreview.service.persistence.util :as putil]
            [com.xebia.visualreview.logging :as log])
  (:import [com.mchange.v2.c3p0 PooledDataSource ComboPooledDataSource]
           (org.h2.jdbc JdbcSQLException)))

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

(defn- read-db-script [name]
  (let [res (io/resource (str "dbscripts/h2/" name ".sql"))]
    (if (nil? res)
      false
      (slurp res))))

(defn- run-db-script! [conn script]
  (j/execute! conn [script]))

(defn get-current-schema-version [conn]
  (let [no-schema-version 0]
  (or
    (try
      (putil/query-single conn ["select svalue from SYSTEM where skey = 'schema_version'"]
                        :row-fn (fn [row] (Long/parseLong (:svalue row))))
      (catch JdbcSQLException e
        no-schema-version))
    no-schema-version)))

(defn- update-db-schema! [conn]
  (loop [version (inc (get-current-schema-version conn))]
    (let [db-script (read-db-script version)]
      (when db-script
        (do
          (log/info (str "Running database schema update to version " version " .."))
          (run-db-script! conn db-script)
          (putil/update! conn :system {:svalue version} ["skey = 'schema_version'"])
          (log/info (str "..done schema update to version " version))
          (recur (inc version)))))))

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
      (log/info "Initializing database..")
      (update-db-schema! conn)
      (log/info "..done initializing database")
      conn)))

(defn close-connection []
  (when-let [db-conn ^PooledDataSource (:datasource conn)]
    (.close db-conn)))
