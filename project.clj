(defproject com.keminglabs/c2 "0.2.4-SNAPSHOT"
  :description "Declarative data visualization in Clojure(Script)."
  :url "http://keminglabs.com/c2/"
  :license {:name "BSD" :url "http://www.opensource.org/licenses/BSD-3-Clause"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2227"]
                 [org.clojure/core.match "0.2.0-alpha12"
                  :exclusions [org.clojure/core.logic]]
                 [clj-iterate "0.96"]

                 ;;CLJS
                 [com.keminglabs/singult "0.1.7-SNAPSHOT"]
                 [com.keminglabs/reflex "0.1.2-SNAPSHOT"]]

  :profiles {:dev {:dependencies [[midje "1.5.0"]
                                  [com.keminglabs/vomnibus "0.3.0"]]}}

  :min-lein-version "2.0.0"

  :plugins [[com.keminglabs/cljx "0.2.1"]
            [lein-cljsbuild "1.0.3"]
            [lein-midje "3.0.0"]
            [lein-marginalia "0.7.0"]]

  :source-paths ["src/clj" "src/cljs"
                 ;;See src/cljx/README.markdown
                 ".generated/clj" ".generated/cljs"

                 ;;Uncomment & change accordingly if you want to build/test with a different version of ClojureScript besides what comes with cljsbuild.
                 ;;For details: https://github.com/emezeske/lein-cljsbuild/issues/58
                 ;;"../software/clojurescript/src/clj" "../software/clojurescript/src/cljs"

                 ;;Use a marginalia fork that documents .cljx files
                 ;;"../software/marginalia/src"
                 ]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path ".generated/clj"
                   :rules cljx.rules/clj-rules}

                  {:source-paths ["src/cljx"]
                   :output-path ".generated/cljs"
                   :extension "cljs"
                   :rules cljx.rules/cljs-rules}]}

  :cljsbuild {:builds {:test {:source-paths ["test/integration/cljs"]
                              :compiler {:output-to "out/test/integration.js"
                                         :optimizations :simple
                                         :pretty-print true}}

                       :scratch {:source-paths ["test/scratch"]
                                 :compiler {:output-to "out/scratch.js"
                                            :optimizations :advanced}}}

              :test-commands {"integration" ["phantomjs" "test/integration/runner.coffee"]}}


  ;;generate cljx before JAR
  :hooks [cljx.hooks])
