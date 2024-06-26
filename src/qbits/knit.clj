(ns qbits.knit
  (:refer-clojure :exclude [future future-call])
  (:require [qbits.commons.enum :as qc])
  (:import (java.util.concurrent Executors ExecutorService Future
                                 ScheduledFuture
                                 ScheduledExecutorService
                                 ThreadFactory TimeUnit)
           (java.util.concurrent.atomic AtomicLong)))

(set! *warn-on-reflection* true)

(def time-units (qc/enum->map TimeUnit))

(defn thread-group
  "Returns a new ThreadGroup instance to be used in thread-factory"
  ^ThreadGroup
  ([^ThreadGroup parent ^String name]
   (ThreadGroup. parent name))
  ([^String name]
   (ThreadGroup. name)))

(defn thread-factory [& {:keys [fmt priority daemon]}]
  (let [thread-cnt (AtomicLong. 0)]
    (reify ThreadFactory
      (newThread [_ f]
        (let [thread (Thread. ^Runnable f)]
          (when (some? daemon)
            (.setDaemon thread (boolean daemon)))
          (when fmt
            (.setName thread (format fmt (.getAndIncrement thread-cnt))))
          (when priority
            (.setPriority thread (int priority)))
          thread)))))

(defn submit
  "Submits the fn to specified executor, returns a Future"
  ^Future
  [^ExecutorService executor ^Callable f]
  (.submit executor f))

(def ^:deprecated execute
  "Submits the fn to specified executor, returns a Future"
  submit)

(defn executor
  "Returns ExecutorService.
  `type` can be :single, :cached, :fixed or :scheduled, this matches the
  corresponding Java instances"
  ^ExecutorService
  ([type] (executor type nil))
  ([type & {:keys [thread-factory num-threads]
            :or {num-threads (int 1)
                 thread-factory (Executors/defaultThreadFactory)}}]
   (case type
     :single (Executors/newSingleThreadExecutor thread-factory)
     :cached (Executors/newCachedThreadPool thread-factory)
     :fixed (Executors/newFixedThreadPool (int num-threads) thread-factory)
     :scheduled (Executors/newScheduledThreadPool (int num-threads) thread-factory)
     :scheduled-single (Executors/newSingleThreadScheduledExecutor thread-factory)
     :thread-per-task (Executors/newThreadPerTaskExecutor thread-factory)
     :virtual (Executors/newVirtualThreadPerTaskExecutor))))

(defn schedule
  "Return a ScheduledFuture.
  `type` can be :with-fixed-delay, :at-fixed-rate, :once
  `delay`'s default unit is milliseconds
  `f` task (function) to be run"
  ^ScheduledFuture
  ([type delay f] (schedule type delay f nil))
  ([type delay f & {:keys [executor initial-delay unit]
                    :or {initial-delay 0
                         unit :milliseconds}}]
   (let [executor (or executor (qbits.knit/executor :scheduled))]
     (case type
       :with-fixed-delay
       (.scheduleWithFixedDelay ^ScheduledExecutorService executor
                                ^Runnable f
                                ^long initial-delay
                                ^long delay
                                (time-units unit))
       :at-fixed-rate
       (.scheduleAtFixedRate ^ScheduledExecutorService executor
                             ^Runnable f
                             ^long initial-delay
                             ^long delay
                             (time-units unit))
       :once (.schedule ^ScheduledExecutorService executor
                        ^Runnable f
                        ^long delay
                        ^TimeUnit (time-units unit))))))

(def binding-conveyor-fn (var-get #'clojure.core/binding-conveyor-fn))
(def deref-future (var-get #'clojure.core/deref-future))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; futures (needs proxy);;;;;;;;;;;;;;;;;;
(defn future-call
  "Takes a function of no args and yields a future object that will
  invoke the function in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block, unless the variant
  of deref with timeout is used. See also - realized?."
  {:added "1.1"
   :static true}
  [f {:as _options
      :keys [executor preserve-bindings?]
      :or {preserve-bindings? true
           executor clojure.lang.Agent/soloExecutor}}]

  (let [f (if preserve-bindings?
            (binding-conveyor-fn f)
            f)
        fut (submit executor f)]
    (reify
      clojure.lang.IDeref
      (deref [_] (deref-future fut))
      clojure.lang.IBlockingDeref
      (deref
        [_ timeout-ms timeout-val]
        (deref-future fut timeout-ms timeout-val))
      clojure.lang.IPending
      (isRealized [_] (.isDone fut))
      java.util.concurrent.Future
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
  [& args]
  (assert (and (>= (count args) 2)
               (map? (last args))))
  `(future-call (^{:once true} fn* [] ~@(butlast args))
                ~(last args)))
