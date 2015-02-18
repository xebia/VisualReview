(defproject visualreview "0.0.1-SNAPSHOT"
  :description "Provides a productive and human-friendly workflow for testing and reviewing your web application's layout
across several browsers, resolutions and platforms."
  :url "https://github.com/xebia/VisualReview"
  :license {:name "Apache Licence 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [environ "1.0.0"]                          ;configuration
                 [ring/ring-core "1.3.2"]                   ;webserver middleware
                 [ring/ring-jetty-adapter "1.3.2"]          ;webserver container
                 [compojure "1.3.1"]                        ;routes
                 [liberator "0.12.2"]                       ;resources
                 [com.taoensso/timbre "3.3.1"]              ;logging
                 [slingshot "0.12.1"]                       ;improved exception handling
                 [org.clojure/java.jdbc "0.3.6"]
                 [com.mchange/c3p0 "0.9.5"]                 ;database connection pooling
                 [com.h2database/h2 "1.4.185"]]

  :min-lein-version "2.4.0"

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
             :silent         true}                         ; only prints errors

  :aliases {"integration"   ["with-profile" "+integration" "midje"]
            "unit"          ["with-profile" "+unit" "midje"]
            "test"          ["midje"]
            "npm-install"   ["shell" "npm" "install"]
            "bower-install" ["shell" "bower" "install"]
            "grunt-build"   ["shell" "grunt" "build"]}

  :profiles {:dev-common  {:dependencies   [[midje "1.6.3"]
                                            [clj-http "1.0.1"]]
                           :plugins        [[lein-environ "1.0.0"]
                                            [lein-midje "3.1.3"]]
                           :resource-paths ["src/integration/resources"]}
             :dev         [:dev-common :dev-overrides]
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

