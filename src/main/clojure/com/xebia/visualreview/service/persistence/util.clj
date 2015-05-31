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

(ns com.xebia.visualreview.service.persistence.util
  (:require [clojure.java.jdbc :as j]
            [cheshire.core :as json])
  (:import (java.sql SQLException)))

(defn- ent-fn [^String s] (.replace s \- \_))
(defn- ident-fn [^String s] (.replace (.toLowerCase s) \_ \-))

(def ^:private h2-generated-key (keyword "scope_identity()"))
(defn- extract-generated-id
  "Workaround for incompatibilities of the clojure.java.jdbc update! and insert! methods between JDBC drivers.
   For example: the H2 driver returns a :scope_identity() key, while PostgreSQL's driver returns the actual table column names as keys.
   This function returns the generated id as a number.
   Important note: this function assumes jdbc-returned-keys only returns 1 key (which is true for our application at time of writing)."
  [{h2-id h2-generated-key id :id}]
  (or h2-id id))

(defn insert-single! [conn table row-map & opts]
  (extract-generated-id (first (apply j/insert! conn table row-map :entities ent-fn opts))))
(defn update! [conn table set-map where-clause & opts]
  (apply j/update! conn table set-map where-clause :entities ent-fn opts))

(defn delete! [conn table where-clause & opts]
  (apply j/delete! conn table where-clause :entities ent-fn opts))
(defn query [conn sql-and-params & opts]
  (apply j/query conn sql-and-params :identifiers ident-fn opts))
(defn query-single [& args]
  (first (apply query args)))
(defn unique-constraint-violation? [^SQLException e]
  (= (.getSQLState e) "23505"))

(defn parse-json-fields [& ks]
  (fn [row]
    (reduce #(update-in %1 [%2] json/parse-string true) row ks)))
