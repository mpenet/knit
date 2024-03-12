# knit 

Thin wrapper around Java Executors/Threads, including executors aware
versions of `future` and `future-call`.

## Installation

via tools.deps

## Changelog

### 1.0.0

* **Breaking changes** : there are no longer a single arg versions of
  `knit/future` and `knit/thread`, just use `clojure.core` equivalents
  in these cases. Also the multi arg version of these 2 macros now
  takes the option map as **last** argument instead of first.

## Usage

```Clojure
(use 'qbits.knit)
```

### Executors

Executor can be `:fixed` `:cached` `:single` `:scheduled` `:virtual`, matching the
corresponding Java instances.

```Clojure
(def x (executor :fixed))
```
With all options
```clojure
(def x (executor :fixed {:num-threads 3 :thread-factory a-thread-factory}))
```

Submit a task to executor
```clojure
(execute x #(println "Hello World"))
```

### ThreadFactory

```clojure
(def tf (thread-factory))
```
With all options
```clojure
(def a-thread-group (thread-group "knit-group"))
(def tf (thread-factory {:thread-group a-thread-group
                         :daemon false}))
```

### ThreadGroup
Identical to the Java version

```clojure
(thread-group "name")
(thread-group parent-group "name")
```

### ScheduledFuture

```clojure
(schedule :at-fixed-rate 200 #(println "hello world"))

```
Supports `:at-fixed-rate` `:with-fixed-delay` `:once`, matching the
corresponding [Java methods](http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/ScheduledExecutorService.html).

With all options:
```clojure
(schedule :at-fixed-rate 2 #(println "hello world")
          {:initial-delay 1
          :executor (executor :scheduled
                              :num-threads 3
                              :thread-factory a-thread-factory)
          :unit :minutes})
```

Time units are `:days` `:hours` `:minutes` `:seconds` `:milliseconds` `:microseconds` `:nanoseconds`


### Clojure like future  with configurable execution context

```clojure
(qbits.knit/future (System/currentTimeMillis) {:executor x})
(qbits.knit/future-call #(System/currentTimeMillis) {:executor x})


## License

Copyright © 2015 Max Penet

Distributed under the Eclipse Public License, the same as Clojure.
