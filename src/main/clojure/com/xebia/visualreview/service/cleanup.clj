(ns com.xebia.visualreview.service.cleanup
  (:require [com.xebia.visualreview.service.screenshot :as s]
            [com.xebia.visualreview.service.image :as i]
            [com.xebia.visualreview.service.service-util :as sutil]
            [com.xebia.visualreview.service.run :as run]
            [com.xebia.visualreview.config :as config]
            [com.xebia.visualreview.service.suite :as suite]))

(defonce cleanup-orphans-in-progress (atom false))

(defn cleanup-orphans! [conn]
  "Removes unused screenshots and images due to deleted runs"
  (if (true? @cleanup-orphans-in-progress)
    (sutil/throw-service-exception "Cleanup already in progress" :cleanup-already-in-progress)
    (do
      (swap! cleanup-orphans-in-progress not)
      (try
        (sutil/attempt
          (do
            (s/delete-unused-screenshots! conn)
            (i/delete-unused-images! conn))
          "An error occured while cleaning up unused screenshots and images: %s"
          :cleanup-orphans-failed)
        (finally
          (swap! cleanup-orphans-in-progress not))))))

(defn- delete-old-runs! [conn max-runs-per-suite]
  (let
    [run-ids-per-suite (suite/get-run-ids-per-suite conn)
     run-ids-to-delete (flatten (map (fn [suite]
                              (drop max-runs-per-suite (:run-ids suite))) run-ids-per-suite))
     ]
    (map (fn [run-id] (run/delete-run! conn run-id)) run-ids-to-delete)
  ))

(defonce cleanup-old-runs-in-progress (atom false))
(defn cleanup-old-runs! [conn]
  "Deletes all runs from all suites until every suite has a minimum amount of min-runs-per-suite runs, where
              the runs are ordered by start time per suite."
  (if (true? @cleanup-old-runs-in-progress)
    (sutil/throw-service-exception "Cleanup already in progress" :cleanup-already-in-progress)
    (do
      (swap! cleanup-old-runs-in-progress not)
      (try
        (sutil/attempt
          (let
            [max-runs-per-suite (:max-runs-per-suite config/env)]
            (if (> max-runs-per-suite -1)
              (delete-old-runs! conn max-runs-per-suite)))
          "An error occured while cleaning up unused screenshots and images: %s"
          :cleanup-old-runs-failed)
        (finally
          (swap! cleanup-old-runs-in-progress not))))))
