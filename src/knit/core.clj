(ns knit.core
  (:import [java.util.concurrent Executors ExecutorService Future
            ScheduledExecutorService ScheduledFuture ScheduledThreadPoolExecutor
            ThreadFactory TimeUnit]))

(def time-units
  {:ns TimeUnit/NANOSECONDS
   :us TimeUnit/MICROSECONDS
   :ms TimeUnit/MILLISECONDS
   :secs TimeUnit/SECONDS
   :mins TimeUnit/MINUTES
   :hours TimeUnit/HOURS
   :days TimeUnit/DAYS})

(defn thread-group
  "Returns a new ThreadGroup instance to be used in thread-factory"
  ^ThreadGroup
  ([^ThreadGroup parent ^String name]
     (ThreadGroup. parent name))
  ([^String name]
     (ThreadGroup. name)))

(defn thread-factory
  "Returns a new ThreadFactory instance"
  [& {:keys [daemon thread-group]
      :or {daemon true
           thread-group (.getThreadGroup (Thread/currentThread))}}]
  (reify ThreadFactory
    (newThread [_ f]
      (doto (Thread.  ^Runnable f)
        (.setDaemon (boolean daemon))))))

(defn execute
  ""
  ^Future
  [^ExecutorService executor ^Callable f]
  (.submit executor f))

(defn executor
  "Returns ExecutorService.
`type` can be :single, :cached, :fixed or :scheduled, this matches the
corresponding Java instances"
  ^ExecutorService
  [type & {:keys [thread-factory num-threads]
           :or {num-threads (int 1)
                thread-factory (Executors/defaultThreadFactory)}}]
  (case type
    :single (Executors/newSingleThreadExecutor thread-factory)
    :cached  (Executors/newCachedThreadPool thread-factory)
    :fixed  (Executors/newFixedThreadPool thread-factory (int num-threads))
    :scheduled (Executors/newScheduledThreadPool (int num-threads) thread-factory)))

(defn schedule
  "Return a ScheduledFuture.
`type` can be :with-fixed-delay, :at-fixed-rate, :once
`delay`'s default unit is milliseconds
`f` task (function) to be run"
  ^ScheduledFuture
  [type delay f & {:keys [initital-delay num-threads thread-factory unit]
                   :or {initital-delay 0
                        num-threads (int 1)
                        thread-factory (Executors/defaultThreadFactory)
                        unit :ms}}]
  (let [x ^ScheduledThreadPoolExecutor (executor :scheduled
                                                 :thread-factory thread-factory
                                                 :num-threads (int num-threads))]
    (case type
      :with-fixed-delay
      (.scheduleWithFixedDelay x
                               ^Runnable f
                               ^long initital-delay
                               ^long delay
                               (time-units unit))

      :at-fixed-rate
      (.scheduleAtFixedRate x
                            ^Runnable f
                            ^long initital-delay
                            ^long delay
                            (time-units unit))

      :once (.schedule x
                       ^Runnable f
                       ^long delay
                       ^TimeUnit (time-units unit)))))
