(ns com.xebia.visualreview.schedule
  (:require [cronj.core :as c]
            [slingshot.slingshot :as ex]
            [clojure.tools.logging :as log]
            [com.xebia.visualreview.service.cleanup :as cleanup]
            [com.xebia.visualreview.service.persistence.database :as db]
            [com.xebia.visualreview.service.service-util :as sutil]))

(defn- try-catch-all [form]
  (ex/try+
    ~form
    (catch Object o#
      (log/warn "Caught exception while running task" ~o#))
    (catch Exception e
      (log/warn "Caught exception while running task" e))))

(defn generate-cleanup-task
  []
  {:id       :cleanup-orphans-task
   :handler  (fn [t opts]
               (ex/try+
                 (sutil/attempt
                   (do
                     (log/info "Running cleanup task..")
                     (cleanup/cleanup-old-runs! db/conn)
                     (cleanup/cleanup-orphans! db/conn)
                     (log/info "..cleanup task ended"))
                   "Something went wrong during scheduled cleanup: %s"
                   :cleanup-error)
                 (catch Object o
                   (log/error (:message o)))))
   :schedule (:cleanup-schedule com.xebia.visualreview.config/env)
   :opts     {}})

(defonce ^:private scheduler nil)

(defn is-a-task-running?
  "Returns true when a task is currently being executed."
  []
  (and (not (nil? scheduler)) (pos? (count (c/get-threads scheduler :cleanup-orphans-task)))))

(defn init!
  "Initializes the scheduler and its tasks"
  []
  (if (is-a-task-running?)
    (log/warn "VisualReview's internal scheduler was asked to reinitialize itself while there were still some tasks running. Reinitialization has been cancelled.")
    (do
      (alter-var-root #'scheduler (fn [_] (c/cronj :entries [(generate-cleanup-task)])))
      (c/start! scheduler))))

(defn shutdown!
  "Stops the scheduler and aborts all running tasks."
  []
  (do
    (if (is-a-task-running?)
      (log/warn "VisualReview's scheduler has been shutdown while there were still tasks running."))
    (c/shutdown! scheduler)))

