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

(ns com.xebia.visualreview.validation
  (:import [java.util Map])
  (:import [java.util Collection])
  (:require [slingshot.slingshot :as ex]
            [clojure.tools.logging :as log]))

(defn- make-error [err-type err-subtype err-msg]
  {:type err-type
   :subtype err-subtype
   :message err-msg})

(defn- coerce [key value coercer error-message]
  (try
    (coercer value)
    (catch Exception _
      (ex/throw+ (make-error ::invalid ::wrong-type (format error-message (name key)))))))

(defn- coerce-to-type [entry-key value entry-type]
  (if (isa? (type value) entry-type)
    value
    (condp = entry-type
      Number (coerce entry-key value #(Long/parseLong %) "'%s' is not a number")
      String (coerce entry-key value str "'%s' is not parsable to string")
      Map (coerce entry-key value #(if (map? %) % (hash-map %)) "'%s' is not of the right type")
      (ex/throw+ (make-error ::unsupported ::empty (format "Unsupported schema type: %s" entry-type))))))

(defmulti ^:private conforms? (fn [_ rule] rule))

(defmethod conforms? ::screenshot [v _]
  (and
    (:filename v)
    (:tempfile v)
    (= (:content-type v) "image/png")))

(defmethod conforms? ::no-whitespace [v _]
  (not (re-find #"\s" v)))

(defmethod conforms? ::non-empty [v _]
  (not (empty? v)))

(defmethod conforms? ::optional [_ _]
  true)

(defmethod conforms? ::screenshot-meta [v _]
  (every? (some-fn string? number? true? false? vector? nil?) (vals v)))

(defmethod conforms? ::diff-status [v _]
  (contains? #{"pending" "accepted" "rejected"} v))

(defmethod conforms? :default
  [_ rule]
  (log/error (str "Unsupported validation rule " rule))
  false)

(defn- apply-validations
  "Checks if the value conforms to each of the given validations
  Throws an exception if not"
  [value validations]
  (if-some [err (some identity (map #(when-not (conforms? value %) %) validations))]
           (do
             (log/info (format "Validation %s failed" err))
             (ex/throw+ (make-error ::invalid err (format "Validation %s failed" err))))
           value))

(defn validate
  "Validates a map based on a schema. schema is expected to have format:
   {:keyword [Type [validation1 validation2 ..]]}
   data is expected to be a map with keyword keys and string values.

   If the given data adheres to the schema, parse will return a map with keyword keys.
   If the data does *not* adhere to the schema, parse will throw an exception containing
   details of the schema violations."
  [schema data]
  (into {}
        (for [[schema-key [entry-type validations]] schema]
          (let [optional-key? (some #{::optional} validations)
                value (schema-key data)]
            (if value
              [schema-key (-> (coerce-to-type schema-key value entry-type)
                              (apply-validations validations))]
              (when-not optional-key?
                (ex/throw+ (make-error ::invalid ::empty (format "Required parameter '%s' is missing" (name schema-key))))))))))

(defn validations
  "Validates a map based on the schema. Returns a map with the key valid?.

  If :valid?, the parsed map will be given under the key :data
  If not :valid?, a validation error is given under the key :error"
  [schema data]
  (ex/try+
    {:valid? true :data (validate schema data)}
    (catch [:type ::invalid] err
      {:valid? false :error err})))
