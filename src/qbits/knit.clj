(ns qbits.knit
  (:refer-clojure :exclude [future future-call])
  (:require
   [qbits.commons.enum :as qc]
   [clojure.core.async :as async]
   [clojure.core.async.impl.protocols :as impl])
  (:import
   [java.util.concurrent Executors ExecutorService Future
    ScheduledExecutorService ScheduledFuture ScheduledThreadPoolExecutor
    ThreadFactory TimeUnit]))

(def time-units (qc/enum->map TimeUnit))

(defn thread-group
  "Returns a new ThreadGroup instance to be used in thread-factory"
  ^ThreadGroup
  ([^ThreadGroup parent ^String name]
     (ThreadGroup. parent name))
  ([^String name]
     (ThreadGroup. name)))

(defn thread-factory
  "Returns a new ThreadFactory instance"
  [{:keys [daemon thread-group]
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
  ([type] (executor type nil))
  ([type {:keys [thread-factory num-threads]
          :or {num-threads (int 1)
               thread-factory (Executors/defaultThreadFactory)}}]
   (case type
     :single (Executors/newSingleThreadExecutor thread-factory)
     :cached  (Executors/newCachedThreadPool thread-factory)
     :fixed  (Executors/newFixedThreadPool (int num-threads) thread-factory)
     :scheduled (Executors/newScheduledThreadPool (int num-threads) thread-factory))))

(defn schedule
  "Return a ScheduledFuture.
`type` can be :with-fixed-delay, :at-fixed-rate, :once
`delay`'s default unit is milliseconds
`f` task (function) to be run"
  ^ScheduledFuture
  ([type delay f] (schedule type delay f nil))
  ([type delay f {:keys [executor initial-delay unit]
                    :or {initial-delay 0
                         unit :milliseconds}}]
   (let [executor (or executor (qbits.knit/executor :scheduled))]
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
  ([f] (future-call f nil))
  ([f {:as options
       :keys [executor preserve-bindings?]
       :or {preserve-bindings? true
            executor clojure.lang.Agent/soloExecutor}}]

   (let [f (if preserve-bindings?
             (binding-conveyor-fn f)
             f)
         fut (execute executor f)]
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
       (cancel [_ interrupt?] (.cancel fut interrupt?))))))

(defmacro future
  "Takes an executor instance and a body of expressions and yields a
   future object that will invoke the body in another thread, and will
   cache the result and return it on all subsequent calls to deref/@. If
   the computation has not yet finished, calls to deref/@ will block,
   unless the variant of deref with timeout is used.."
  [options & body]
  `(future-call (^{:once true} fn* [] ~@body) ~options))

(def thread-macro-executor (var-get #'async/thread-macro-executor))

(defn thread-call
  "Executes f in another thread, returning immediately to the calling
  thread. An optional second argument allows to pass an executor that
  implements clojure.core.async.impl.protocols/Executor. Returns a
  channel which will receive the result of calling f when completed."
  ([f] (thread-call f nil))
  ([f {:keys [executor]}]
   (let [c (async/chan 1)]
     (let [binds (clojure.lang.Var/getThreadBindingFrame)]
       (execute (or executor thread-macro-executor)
                  (fn []
                    (clojure.lang.Var/resetThreadBindingFrame binds)
                    (try
                      (let [ret (f)]
                        (when-not (nil? ret)
                          (async/>!! c ret)))
                      (finally
                        (async/close! c))))))
     c)))

(defmacro thread
  "Same as thread but takes an option map first:
  :executor - An executor that implements
clojure.core.async.impl.protocols/Executor"
  [opts & body]
  `(thread-call (fn [] ~@body) ~opts))
