(ns qbits.knit.core-test
  (:refer-clojure :exclude [future future-call])
  (:require [clojure.test :refer [deftest is]]
            [qbits.knit :as k])
  (:import [java.util.concurrent ExecutorService ThreadPoolExecutor
            Executors$FinalizableDelegatedExecutorService
            ScheduledThreadPoolExecutor]))

(deftest test-futures
  (let [x (k/executor :single)]
    (is (= 1 @(k/future 1 {:executor x})))
    (is (= 1 @(k/future-call (constantly 1) {:executor x})))))

(deftest test-executors
  (is (= Executors$FinalizableDelegatedExecutorService (class (k/executor :single))))
  (is (= ThreadPoolExecutor (class (k/executor :cached))))
  (is (= ScheduledThreadPoolExecutor (class (k/executor :scheduled))))
  (is (= ThreadPoolExecutor (class (k/executor :fixed)))))

(deftest test-schedule
  (let [r (atom {:with-fixed-delay 0 :at-fixed-rate 0 :once 0})]
    (k/schedule :once 1000 #(swap! r update-in [:once] inc))
    (k/schedule :with-fixed-delay 1000 #(swap! r update-in [:with-fixed-delay] inc))
    (k/schedule :at-fixed-rate 1000 #(swap! r update-in [:at-fixed-rate] inc))

    (Thread/sleep 4500)

    (is (= 1 (:once @r)))
    (is (= 5 (:with-fixed-delay @r)))
    (is (= 5 (:at-fixed-rate @r)))))
