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

(ns com.xebia.visualreview.service.middleware-test
  (:require [clojure.test :refer :all]
            [com.xebia.visualreview.service.service-util :as service-util]
            [com.xebia.visualreview.test-util :refer :all]
            [com.xebia.visualreview.config :as config]
            [clojure.tools.logging :as log]
            [com.xebia.visualreview.middleware :as middleware])
  (:import (java.sql SQLException)
           (java.io StringReader)
           (clojure.tools.logging.impl Logger LoggerFactory)))

(deftest wrap-exception
  (testing "wraps generic exceptions and outputs it as a 500 response"
    (is (= {:status  500
            :headers {}
            :body    "Internal error occured"}
           ((middleware/wrap-exception (fn [_] (throw (new Exception "my message")))) {})) "Converts unhandled exceptions to a 500 response")
    (is (= {:status  500
            :headers {}
            :body    "Internal database error occured"}
           ((middleware/wrap-exception (fn [_] (throw (new SQLException "my sql message")))) {})) "Converts unhandled database exceptions to a 500 response")
    (is (= {:status  500
            :headers {}
            :body    "Internal service error occured"}
           ((middleware/wrap-exception (fn [_] (service-util/throw-service-exception "my message" 1234))) {})) "Converts unhandled service exceptions to a 500 response")))


(defn logs-collector [atm] (fn [& args]
                             (let [args-map {:level (nth args 1) :message (last args)}]
                               (swap! atm #(conj % args-map)))))

(def mock-logger (reify
                   Logger
                   (enabled? [_ _] true)
                   Object
                   (toString [_] "fake-logger")))

(def mock-logger-factory (reify LoggerFactory
                           (get-logger [_ logger-ns] mock-logger)))

(defn setup-mock-logger
  [logging-config-enabled handler-response body]
  (binding [log/*logger-factory* mock-logger-factory]
    (let [log*-args (atom [])]
      (with-redefs [log/log* (logs-collector log*-args)
                    config/env {:enable-http-logging logging-config-enabled}]
        (let [handler (fn [_] handler-response)
              logger (middleware/http-logger handler)]
          (body logger log*-args))))))

(deftest http-logger
  (testing "Logs request and response headers and body when enabled"
    (setup-mock-logger
      true {:headers {:headera "valuea" :headerb "valueb"} :body "response body"}
      (fn [logger log-args]
        (is (= {:level :info :message "HTTP logging enabled"} (nth @log-args 0)) "Should log that HTTP logging has been enabled")
        (logger {:uri          "someUri"
                 :body         (StringReader. "someBody")
                 :method       " someMethod "
                 :content-type " someContentType "
                 :headers      {:header1 " value1 " :header2 " value2 "}})
        (is (= 3 (count @log-args)))
        (is (= {:level :info :message (str
                                        "Request: \n"
                                        "# uri: someUri\n"
                                        "# method: \n"
                                        "# content-type:  someContentType \n"
                                        "# headers: \n"
                                        "header2:  value2 \n"
                                        "header1:  value1 \n"
                                        "# body: \n"
                                        "someBody")} (nth @log-args 1)) "Should log request headers and body")
        (is (= {:level :info :message (str
                                        "Response: \n"
                                        "# headers: \n"
                                        "headera: valuea\n"
                                        "headerb: valueb\n"
                                        "# body: \n"
                                        "response body\n")} (nth @log-args 2)) "Should log response headers and body"))))

  (testing "Does not logs request and response headers and body when disabled"
    (setup-mock-logger
      false {:headers {:headera "valuea" :headerb "valueb"} :body "response body"}
      (fn [logger log-args]
        (is (= 0 (count @log-args)))
        (logger {:uri          "someUri"
                 :body         (StringReader. "someBody")
                 :method       " someMethod "
                 :content-type " someContentType "
                 :headers      {:header1 " value1 " :header2 " value2 "}})
        (is (= 0 (count @log-args)))))))
