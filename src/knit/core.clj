(ns knit.core
  (:refer-clojure :exclude [future future-call])
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
      :or {daemon true}}]
  (reify ThreadFactory
    (newThread [_ f]
      (doto (Thread. ^ThreadGroup thread-group ^Runnable f)
        (.setDaemon (boolean daemon))))))

(defn execute
  "Submits the fn to specified executor, returns a Future"
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
    :fixed  (Executors/newFixedThreadPool (int num-threads) thread-factory)
    :scheduled (Executors/newScheduledThreadPool (int num-threads) thread-factory)))

(defn schedule
  "Return a ScheduledFuture.
`type` can be :with-fixed-delay, :at-fixed-rate, :once
`delay`'s default unit is milliseconds
`f` task (function) to be run"
  ^ScheduledFuture
  [type delay f & {:keys [executor initial-delay unit]
                   :or {executor (knit.core/executor :scheduled)
                        initial-delay 0
                        unit :ms}}]
  (case type
    :with-fixed-delay
    (.scheduleWithFixedDelay ^ScheduledThreadPoolExecutor executor
                             ^Runnable f
                             ^long initial-delay
                             ^long delay
                             (time-units unit))
    :at-fixed-rate
    (.scheduleAtFixedRate ^ScheduledThreadPoolExecutor executor
                          ^Runnable f
                          ^long initial-delay
                          ^long delay
                          (time-units unit))
    :once (.schedule ^ScheduledThreadPoolExecutor executor
                     ^Runnable f
                     ^long delay
                     ^TimeUnit (time-units unit))))


;; Almost identical copies of clojure.core future, only difference is
;; the executor parameter

(defn ^:private binding-conveyor-fn
  [f]
  (let [frame (clojure.lang.Var/getThreadBindingFrame)]
    (fn
      ([]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f))
      ([x]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x))
      ([x y]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x y))
      ([x y z]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (f x y z))
      ([x y z & args]
         (clojure.lang.Var/resetThreadBindingFrame frame)
         (apply f x y z args)))))

(defn ^:static future-call
  "Takes an executor instance and a function of no args and yields a
   future object that will invoke the function in another thread, and
   will cache the result and return it on all subsequent calls to
   deref/@. If the computation has not yet finished, calls to deref/@
   will block, unless the variant of deref with timeout is used."
  [executor f]
  (let [f (binding-conveyor-fn f)
        fut (execute executor f)]
    (reify
      clojure.lang.IDeref
      (deref [_] (.get fut))
      clojure.lang.IBlockingDeref
      (deref
        [_ timeout-ms timeout-val]
        (try (.get fut timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)
             (catch java.util.concurrent.TimeoutException e
               timeout-val)))
      clojure.lang.IPending
      (isRealized [_] (.isDone fut))
      Future
      (get [_] (.get fut))
      (get [_ timeout unit] (.get fut timeout unit))
      (isCancelled [_] (.isCancelled fut))
      (isDone [_] (.isDone fut))
      (cancel [_ interrupt?] (.cancel fut interrupt?)))))

(defmacro future
  "Takes an executor instance and a body of expressions and yields a
   future object that will invoke the body in another thread, and will
   cache the result and return it on all subsequent calls to deref/@. If
   the computation has not yet finished, calls to deref/@ will block,
   unless the variant of deref with timeout is used.."
  [executor & body]
  `(future-call ~executor (^{:once true} fn* [] ~@body)))