# knit

Thin wrapper around Java Executors/Threads, including configurable
`future` and `future-call`.

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
(def a-thread-group (thread-group "knit-group"))
(def tf (thread-factory :thread-group a-thread-group
                        :deamon false))
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
corresponding Java methods.

With all options:
```clojure
(schedule :at-fixed-rate 2 #(println "hello world")
          :initial-delay 1
          :executor (executor :scheduled
                              :num-threads 3
                              :thread-factory a-thread-factory)
          :unit :mins)
```

Time units are `:ns` `:us` `:ms` `:secs` `:mins` `:hours` `:days`.


### Clojure like future with configurable execution context

```clojure
(knit.core/future x (System/currentTimeMillis))
(knit.core/future-call x #(System/currentTimeMillis))
```

## License

Copyright © 2012 Max Penet

Distributed under the Eclipse Public License, the same as Clojure.
