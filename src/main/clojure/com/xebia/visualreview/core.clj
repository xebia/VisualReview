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

(ns com.xebia.visualreview.core
  (:require [slingshot.slingshot :refer [try+]]
            [clojure.tools.logging :as log]
            [com.xebia.visualreview.starter :as starter]
            [com.xebia.visualreview.config :as config]
            [com.xebia.visualreview.io :as io]
            [com.xebia.visualreview.service.persistence.database :as db]
            [com.xebia.visualreview.schedule :as schedule])
  (:gen-class))

(defn- config-settings []
  (try+
    (config/init!)
    (catch Exception e
      (log/error (str "Server configuration error: " (.getMessage e))))))

(defn -main [& _]
  (when-let [{:keys [server-port db-uri db-user db-password screenshots-dir]} (config-settings)]
    (try
      (io/init-screenshots-dir! screenshots-dir)
      (db/init! db-uri db-user db-password)
      (schedule/init!)
      (starter/start-server server-port)
      :ok
      (catch Exception e
        (log/error (str "Error initializing: " (.getMessage e)))
        (starter/stop-server)
        (schedule/shutdown!)
        (db/close-connection)
        (System/exit 1)))))

(defn- restart []
  (do
    (starter/stop-server)
    (-main)))

