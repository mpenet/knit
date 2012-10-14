# knit

Thin wrapper around Java Executors/Threads

## Usage

```Clojure
(use 'knit.core)
```

### Executors

Executor can be `:fixed` `:cached` `:single` `:scheduled`, matching the
corresponding Java instances.

```Clojure
(def x (executors :fixed))
```
With all options
```clojure
(def x (executors :fixed :num-threads 3 :thread-factory a-thread-factory))
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
(def tf (thread-factory :thread-group a-thread-group :deamon false))
```

### ScheduledFuture

```clojure
(schedule :at-fixed-rate 200 #(println "hello world"))

```
Supports `:at-fixed-rate` `:with-fixed-delay` `:once`, matching the
corresponding Java methods.

With all options:
```clojure
(schedule :at-fixed-rate 2 #(println "hello world")
          :initital-delay 1
          :num-threads 4
          :thread-factory a-thread-factory
          :unit :mins)
```

Time units are `:ns` `:us` `:ms` `:secs` `:mins` `:hours` `:days`.


### Clojure like future with configurable execution context

```clojure

(def x (executor :fixed :num-threads 3))

(knit.core/future x (System/currentTimeMillis))
(knit.core/future-call x #(System/currentTimeMillis))
```

## License

Copyright © 2012 Max Penet

Distributed under the Eclipse Public License, the same as Clojure.
