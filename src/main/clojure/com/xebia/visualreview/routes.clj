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

(ns com.xebia.visualreview.routes
  (:require [compojure.core :refer :all]
            [ring.util.response :refer [resource-response redirect]]
            [compojure.route :as route]
            [com.xebia.visualreview.middleware :as m]
            [com.xebia.visualreview.resource :as resource]
            [com.xebia.visualreview.io :as io]
            [com.xebia.visualreview.config :as config]))

(def ^:private resources-root "public")

(def api-routes
  (m/wrap-tx
    (routes
      (ANY "/projects" [] (resource/project-resource))
      (context "/projects/:project-id" [project-id]
        (ANY "/" [] (resource/get-project project-id))
        (ANY "/suites" [] (resource/suites-resource project-id))
        (context "/suites/:suite-id" [suite-id]
          (ANY "/" [suite-id] (resource/suite-resource project-id suite-id))))
      (ANY "/runs" req resource/runs-resource)
      (context "/runs/:run-id" [run-id]
        (ANY "/" [] (resource/run-resource run-id))
        (ANY "/screenshots" [] (resource/screenshots-resource run-id))
        (ANY "/analysis" [] (resource/analysis-resource run-id))
        (ANY "/analysis/diffs/:diff-id" [diff-id] (resource/diff-status-resource run-id diff-id)))
      (route/not-found nil))))

(defroutes main-router
  (GET "/" [] (resource-response "index.html" {:root resources-root}))
  (context "/api" req api-routes)
  (context "/screenshots" []
    (route/files "/" {:root io/screenshots-dir})
    (route/not-found nil))
  (route/resources "/")
  (route/not-found "Page not found")
  (constantly (redirect "/")))
