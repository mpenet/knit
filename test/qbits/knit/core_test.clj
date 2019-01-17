(ns qbits.knit.core-test
  (:refer-clojure :exclude [future future-call])
  (:use clojure.test
        qbits.knit)
  (:require [clojure.core.async :as async])
  (:import [java.util.concurrent ExecutorService ThreadPoolExecutor
            Executors$FinalizableDelegatedExecutorService
            ScheduledThreadPoolExecutor]))

(deftest test-futures
  (let [x (executor :single)]
    (is (= 1 @(future 1 {:executor x})))
    (is (= 1 @(future-call (constantly 1) {:executor x})))))

(deftest test-thread
  (let [x (executor :single) ]
    (is (= 1 (async/<!! (thread 1 {:executor x}))))
    (is (= 1 (async/<!! (thread-call (constantly 1)
                                     {:executor x} ))))))

(deftest test-executors
  (is (= Executors$FinalizableDelegatedExecutorService (class (executor :single))))
  (is (= ThreadPoolExecutor (class (executor :cached))))
  (is (= ScheduledThreadPoolExecutor (class (executor :scheduled))))
  (is (= ThreadPoolExecutor (class (executor :fixed)))))


(deftest test-schedule
  (let [r (atom {:with-fixed-delay 0 :at-fixed-rate 0 :once 0})]
    (schedule :once 1000 #(swap! r update-in [:once] inc))
    (schedule :with-fixed-delay 1000 #(swap! r update-in [:with-fixed-delay] inc))
    (schedule :at-fixed-rate 1000 #(swap! r update-in [:at-fixed-rate] inc))

    (Thread/sleep 4500)

    (is (= 1 (:once @r)))
    (is (= 5 (:with-fixed-delay @r)))
    (is (= 5 (:at-fixed-rate @r)))))
