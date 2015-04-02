(defproject cc.qbits/knit "0.3.0"
  :description "Thin wrapper around Java Executors/Threads"
  :url "https://github.com/mpenet/knit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cc.qbits/commons "0.2.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :profiles {:1.4  {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5  {:dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}
             :dev  {:dependencies [[codox "0.6.1"]]}
             :test {:dependencies []}}
  :global-vars {*warn-on-reflection* true})
