(ns com.xebia.visualreview.service.branch
    (:require [com.xebia.visualreview.service.persistence.util :as putil]
      [com.xebia.visualreview.service.service-util :as sutil]))

(defn branch-exists
      "Checks if a branch exists. Returns the run id's of runs of this branch (I think.)."
      [conn project-name suite-name branch-name]
        (putil/query-single conn
                            ["SELECT run.id FROM run
                                 JOIN suite ON run.suite_id = suite.id
                                 JOIN project ON suite.project_id = project.id
                                 WHERE project.name = ?
                                   AND suite.name = ?
                                   AND run.branch_name = ?" project-name suite-name branch-name]
                            ))