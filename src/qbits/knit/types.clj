(ns qbits.knit.types
  (:require
   [clojure.core.typed :as t]))

(t/def-alias TimeUnitValue (U ':ns ':us ':ms ':secs ':mins ':hours ':days))
(t/def-alias ExecutorType (U ':single ':cached ':fixed ':scheduled))
(t/def-alias ScheduledExecutorType (U ':with-fixed-delay ':at-fixed-rate ':once))
