(defproject cc.qbits/knit "0.3.1"
  :description "Thin wrapper around Java Executors/Threads"
  :url "https://github.com/mpenet/knit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0-RC5"]
                 [cc.qbits/commons "0.4.5"]
                 [org.clojure/core.async "0.2.374"]]
  :codox {:src-dir-uri "https://github.com/mpenet/knit/blob/master/"
          :src-linenum-anchor-prefix "L"
          :defaults {:doc/format :markdown}}
  :global-vars {*warn-on-reflection* true})
