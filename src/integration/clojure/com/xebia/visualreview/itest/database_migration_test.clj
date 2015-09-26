(ns com.xebia.visualreview.itest.database-migration-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [clojure.java.io :as io]
            [com.xebia.visualreview.logging :as log]
            [com.xebia.visualreview.service.persistence.database :as db]
            [com.xebia.visualreview.service.project :as project]
            [com.xebia.visualreview.service.suite :as suite]
            [com.xebia.visualreview.service.run :as run]
            [com.xebia.visualreview.service.analysis :as analysis]
            [com.xebia.visualreview.service.baseline :as baseline])
  (:import (org.joda.time DateTime)
           (org.joda.time.format DateTimeFormatter DateTimeFormat)))

(def ^:dynamic *conn* {:classname      "org.h2.Driver"
                       :subprotocol    "h2"
                       :subname        "file:./target/temp/vr-db-migration-test.db;TRACE_LEVEL_FILE=2"
                       :user           ""
                       :init-pool-size 1
                       :max-pool-size  1})

;2015-09-09T21:05:43+0200
(def date-time-formatter (DateTimeFormat/forPattern "yyyy-MM-dd'T'HH:mm:ssZ"))
(defn date-time-str [years months days hours minutes seconds]
  (.print date-time-formatter (new DateTime years months days hours minutes seconds)))

(defn- run-db-script! [conn script]
  (j/execute! conn [script]))

(defn- read-db-script [path]
  (let [res (io/resource (str path))]
    (if (nil? res)
      false
      (slurp res))))


(defn- setup-db []
  (log/info "Setting up database-migration-test database")
  (j/with-db-connection [conn *conn*]
                        (j/execute! conn ["DROP ALL OBJECTS"])
                        (log/info "Creating initial v1 data")
                        (run-db-script! conn (read-db-script "dbmigration/dump-1.sql"))
                        (db/update-db-schema! conn)))

(defn- setup-db-fixture [f]
  (let [_ (setup-db)]
    (f)))

(use-fixtures :each setup-db-fixture)

(deftest database-migration

  (testing "Database content after migration"
    (is (= (count (project/get-projects *conn*)) 1))
    (is (= (project/get-project-by-name *conn* "db-migration-project") {:name "db-migration-project", :id 1}))
    (is (= (count (suite/get-suites *conn* 1))))
    (is (= (suite/get-suite-by-name *conn* "db-migration-project" "suite1") {:name "suite1", :project-id 1, :id 1}))

    (is (= (count (run/get-runs *conn* 1 1))))
    (is (= (run/get-run *conn* 1)
           {:project-id 1, :status "running",
            :end-time nil, :start-time (date-time-str 2015 9 9 21 5 43),
            :baseline-tree-id 1, :branch-name "master", :suite-id 1, :id 1}))
    (is (= (analysis/get-full-analysis *conn* 1)
           {:analysis
                   {:suite-name "suite1", :suite-id 1, :project-name "db-migration-project", :project-id 1, :baseline-node 1,
                    :run-id 1, :creation-time (date-time-str 2015 9 9 21 5 43), :id 1},
            :diffs [{:after-size 27552, :analysis-id 1, :after-image-id 1, :before-meta nil, :after-name "First", :before-name nil,
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 2, :percentage 0.00M, :before-image-id nil, :after-meta {}, :after 1, :status "accepted",
                     :before-properties nil, :id 1, :before-size nil, :before nil}

                    {:after-size 30490, :analysis-id 1, :after-image-id 3, :before-meta nil, :after-name "Second", :before-name nil,
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 4, :percentage 0.00M, :before-image-id nil, :after-meta {}, :after 2, :status "rejected",
                     :before-properties nil, :id 2, :before-size nil, :before nil}

                    {:after-size 19984, :analysis-id 1, :after-image-id 5, :before-meta nil, :after-name "Third", :before-name nil,
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 6, :percentage 0.00M, :before-image-id nil, :after-meta {}, :after 3, :status "pending",
                     :before-properties nil, :id 3, :before-size nil, :before nil}]}))

    (is (= (run/get-run *conn* 2)
           {:project-id 1, :status "running",
            :end-time nil, :start-time (date-time-str 2015 9 9 21 8 0),
            :baseline-tree-id 1, :branch-name "master", :suite-id 1, :id 2}))
    (is (= (analysis/get-full-analysis *conn* 2)
           {:analysis
            {:suite-name "suite1", :suite-id 1, :project-name "db-migration-project", :project-id 1, :baseline-node 1,
             :run-id 2, :creation-time (date-time-str 2015 9 9 21 8 0), :id 2},
            :diffs [{:after-size 27552, :analysis-id 2, :after-image-id 7, :before-meta {}, :after-name "First", :before-name "First",
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 8, :percentage 0.00M, :before-image-id 1, :after-meta {}, :after 4, :status "accepted",
                     :before-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :id 4, :before-size 27552, :before 1}
                    {:after-size 30490, :analysis-id 2, :after-image-id 9, :before-meta nil, :after-name "Second", :before-name nil,
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 10, :percentage 0.00M, :before-image-id nil, :after-meta {}, :after 5, :status "accepted",
                     :before-properties nil, :id 5, :before-size nil, :before nil}
                    {:after-size 19984, :analysis-id 2, :after-image-id 11, :before-meta nil, :after-name "Third", :before-name nil,
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 12, :percentage 0.00M, :before-image-id nil, :after-meta {}, :after 6, :status "rejected",
                     :before-properties nil, :id 6, :before-size nil, :before nil}]}))

    (is (= (run/get-run *conn* 3)
           {:project-id 1, :status "running",
            :end-time nil, :start-time (date-time-str 2015 9 9 21 9 51),
            :baseline-tree-id 1, :branch-name "master", :suite-id 1, :id 3} ))
    (is (= (analysis/get-full-analysis *conn* 3)
           {:analysis
            {:suite-name "suite1", :suite-id 1, :project-name "db-migration-project", :project-id 1, :baseline-node 1,
             :run-id 3, :creation-time (date-time-str 2015 9 9 21 9 52), :id 3},
            :diffs [{:after-size 27552, :analysis-id 3, :after-image-id 13, :before-meta {}, :after-name "First", :before-name "First",
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 14, :percentage 0.00M, :before-image-id 7, :after-meta {}, :after 7, :status "accepted",
                     :before-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :id 7, :before-size 27552, :before 4}
                    {:after-size 30696, :analysis-id 3, :after-image-id 15, :before-meta {}, :after-name "Second", :before-name "Second",
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 16, :percentage 24.70M, :before-image-id 9, :after-meta {}, :after 8, :status "accepted",
                     :before-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :id 8, :before-size 30490, :before 5}
                    {:after-size 19984, :analysis-id 3, :after-image-id 17, :before-meta nil, :after-name "Third", :before-name nil,
                     :after-properties {:os "MAC", :browser "firefox", :version "40.0.3", :resolution "800x877"},
                     :image-id 18, :percentage 0.00M, :before-image-id nil, :after-meta {}, :after 9, :status "accepted",
                     :before-properties nil, :id 9, :before-size nil, :before nil}]}))

    (is (= (baseline/get-baseline-head *conn* 1) 1))
    (is (= (baseline/get-baseline-screenshot *conn* 1 "master" "First" (array-map :os "MAC" :browser "firefox" :version "40.0.3" :resolution "800x877"))
           {:image-id 13, :screenshot-name "First", :meta "{}", :properties "{\"os\":\"MAC\",\"browser\":\"firefox\",\"version\":\"40.0.3\",\"resolution\":\"800x877\"}", :size 27552, :id 7} ))
    (is (= (baseline/get-baseline-screenshot *conn* 1 "master" "Second" (array-map :os "MAC" :browser "firefox" :version "40.0.3" :resolution "800x877"))
           {:image-id 15, :screenshot-name "Second", :meta "{}", :properties "{\"os\":\"MAC\",\"browser\":\"firefox\",\"version\":\"40.0.3\",\"resolution\":\"800x877\"}", :size 30696, :id 8} ))
    (is (= (baseline/get-baseline-screenshot *conn* 1 "master" "Third" (array-map :os "MAC" :browser "firefox" :version "40.0.3" :resolution "800x877"))
           {:image-id 17, :screenshot-name "Third", :meta "{}", :properties "{\"os\":\"MAC\",\"browser\":\"firefox\",\"version\":\"40.0.3\",\"resolution\":\"800x877\"}", :size 19984, :id 9} ))

    (is (= (com.xebia.visualreview.service.image.persistence/get-image-path *conn* 1) "2015/8/9/21/1.png"))
    (is (= (com.xebia.visualreview.service.image.persistence/get-image-path *conn* 17) "2015/8/9/21/17.png"))
    (is (= (com.xebia.visualreview.service.image.persistence/get-image-path *conn* 18) "2015/8/9/22/18.png"))))
