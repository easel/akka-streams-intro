# Built-in Stages

## Single Stream

```scala
val singleFlow: Flow[String] = Flow[Int]
  .buffer(10, OverflowStrategy.backpressure) // buffer and apply backpressure
  .buffer(10, OverflowStrategy.dropTail) // buffer and drop old
  .delay(10.millis, DelayOverflowStrategy.backpressure) // force a delay downstream
  .throttle(1, 10.millis, 10, ThrottleMode.Shaping) // throttle to 1 in 10 milliseconds
  .map(_.toString) // map to a new type
  .async // introduce an async boundary
  .grouped(2) // convert every pair of ints to a Seq(1, 2)
  .mapConcat(identity) // expand the groups back to ints
  .mapAsyncUnordered(10)(Future.successful) // do 10 things asynchronously
  .intersperse(",") // intersperse "," similar to mkString
```

## Multi Stream
```scala
flow
    .broadcast //(1 input, n outputs) signals each output given an input signal,
    .zip // (2 inputs => 1 output) create tuples of (A, B) from a stream of A and a stream of B
    .unzip // (1 input => 2 outputs) unzip tuples of (A, B) into two streams one type A and one of type B
    .merge // (n inputs , 1 output), picks signals randomly from inputs pushing them one by one to its output,
    .concat //  (2 inputs, 1 output), first consume one stream, then the second stream
    .interleave // mix N elements from A, then N elements from B
```



# Other Use Cases

- Stream results from Database via Slick
- Stream HTTP requests (akka-http / play)
- Stream HTTP responses (akka-http / play)
- Stream query results from ElasticSearch
- Distribute heavy computation using Akka Cluster
- Replace "work pulling" pattern
