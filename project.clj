(defproject cc.qbits/knit "0.3.2"
  :description "Thin wrapper around Java Executors/Threads"
  :url "https://github.com/mpenet/knit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0-beta3"]
                 [cc.qbits/commons "0.5.1"]
                 [org.clojure/core.async "0.4.474"]]
  :global-vars {*warn-on-reflection* true})
