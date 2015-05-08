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

(ns com.xebia.visualreview.starter
  (:import [org.eclipse.jetty.server Server])
  (:require [ring.middleware
             [params :as params]
             [keyword-params :refer [wrap-keyword-params]]
             [multipart-params :refer [wrap-multipart-params]]]
            [ring.adapter.jetty :refer [run-jetty]]
            [com.xebia.visualreview.routes :as routes]
            [com.xebia.visualreview.logging :as log]
            [com.xebia.visualreview.middleware :as middleware]))

(def app (-> routes/main-router
             wrap-keyword-params
             wrap-multipart-params
             params/wrap-params
             middleware/wrap-exception))

(defonce server (atom nil))

(defn stop-server []
  (if-let [ws @server]
    (do
      (.stop ^Server ws)
      (reset! server nil)
      (log/info "VisualReview server stopped"))
    (log/info "VisualReview server not running")))

(defn start-server [port]
  (try
    (reset! server (run-jetty #'app {:join? false :port port}))
    (log/info (str "VisualReview server started on port " port))
    (catch Exception e (log/error (str "Could not start server on port " port ": " (.getMessage e))))))
