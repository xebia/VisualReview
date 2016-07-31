(defproject visualreview "0.1.5-SNAPSHOT"
  :description "Provides a productive and human-friendly workflow for catching visual regressions by comparing screenshots"
  :url "https://github.com/xebia/VisualReview"
  :license {:name "Apache Licence 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.2"]                   ;webserver middleware
                 [ring/ring-jetty-adapter "1.3.2"]          ;webserver container
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.3.1"]                        ;routes
                 [liberator "0.12.2"]                       ;resources
                 [cheshire "5.4.0"]                         ;JSON encoding/decoding
                 [ch.qos.logback/logback-classic "1.1.3"]   ;logging
                 [slingshot "0.12.1"]                       ;improved exception handling (to be removed)
                 [org.clojure/java.jdbc "0.3.6"]
                 [com.mchange/c3p0 "0.9.5"]                 ;database connection pooling
                 [com.h2database/h2 "1.4.185"]
                 [joda-time/joda-time "2.8.2"]
                 [org.json/json "20160212"]
                 [im.chit/cronj "1.4.1"]]                   ;task scheduling

  :min-lein-version "2.4.0"

  :jvm-opts ["-Xmx1g"]

  :plugins [[lein-shell "0.4.0"]
            [lein-resource "14.10.1"]]

  :main com.xebia.visualreview.core

  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure" "src/integration/clojure"]
  :java-source-paths ["src/main/java"]
  :resource-paths ["src/main/resources"]

  :shell {:dir "viewer"}

  :resource {:resource-paths ["viewer/dist"]
             :target-path    "target/classes/public"
             :skip-stencil   [#".*"]
             :silent         false}

  :aliases {"integration"   ["with-profile" "+integration" "test"]
            "unit"          ["with-profile" "+unit" "test"]
            "npm-install"   ["shell" "npm" "install"]
            "bower-install" ["shell" "bower" "install"]
            "grunt-build"   ["shell" "grunt" "build"]
            "test-all"      ["do" ["test"] ["shell" "npm" "install"] ["shell" "bower" "install"] ["shell" "grunt" "test"]]}

  :profiles {:dev         {:dependencies   [[clj-http "1.0.1"]]
                           :resource-paths ["src/test/resources" "src/integration/resources"]}
             :uberjar     {:aot        :all
                           :prep-tasks ^:replace [["npm-install"] ["bower-install"] ["grunt-build"]
                                                  ["resource"] ["javac"] ["compile"]]
                           :hooks      [leiningen.resource]}
             :integration {:test-paths     ^:replace ["src/integration/clojure"]
                           :resource-paths ["src/integration/resources"]}
             :unit        {:test-paths ^:replace ["src/test/clojure"]}}

  :jar-name "visualreview-%s.jar"
  :uberjar-name "visualreview-%s-standalone.jar"
  :javac-options ["-target" "1.7" "-source" "1.7"])
