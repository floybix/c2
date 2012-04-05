(defproject com.keminglabs/c2 "0.1.0-beta2-SNAPSHOT"
  :description "Declarative data visualization in Clojure(Script)."
  :url "http://keminglabs.com/c2/"
  :license {:name "BSD" :url "http://www.opensource.org/licenses/BSD-3-Clause"}

  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/core.match "0.2.0-alpha9"]
                 [clj-iterate "0.96"]]

  :profiles {:dev {:dependencies [[midje "1.3.1"]
                                  [lein-midje "1.0.8"]
                                  [com.stuartsierra/lazytest "1.2.3"]]
                   ;;Required for lazytest.
                   :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"
                                  "stuartsierra-snapshots" "http://stuartsierra.com/m2snapshots"}}}
  
  :plugins [[com.keminglabs/cljx "0.1.0"]]

  :source-paths ["src/clj" "src/cljs"
                 ;;See src/cljx/README.markdown
                 ".generated/clj" ".generated/cljs"]
  
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path ".generated/clj"
                   :rules cljx.rules/clj-rules}
                  
                  {:source-paths ["src/cljx"]
                   :output-path ".generated/cljs"
                   :extension "cljs"
                   :rules cljx.rules/cljs-rules}]}
  
  ;;generate cljx before JAR
  :hooks [cljx.hooks])
