(ns com.xebia.visualreview.service.cleanup
  (:require [com.xebia.visualreview.service.screenshot :as s]
            [com.xebia.visualreview.service.image :as i]))

(defn cleanup-orphans! [conn]
  "Removes unused screenshots and images due to deleted runs"
  (s/delete-unused-screenshots! conn)
  (i/delete-unused-images! conn))
