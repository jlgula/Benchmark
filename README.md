# Benchmark related to StackOverflow question: [Is there a way to use Scala sequences with performance comparable to Java arrays?](https://github.com/jlgula/Benchmark/new/main?readme=1)

This benchmark copies data from an array or Scala sequence into an intermediate buffer
and from there to an output stream. My original, naive tests showed better performance
from arrays than sequences, hence I raised the question to StackOverflow. In more
refined tests, the array version is still faster, particularly compared to sourcing
from a vector.

Following the suggestion of [stefanobaghino](https://stackoverflow.com/users/3314107/stefanobaghino), I rewrote the benchmark tests to use the
[Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh) with the [SBT Plugin](https://github.com/sbt/sbt-jmh)

The original code did a double copy:
1) Copy from data in an array or sequence into the intermediate buffer
2) Copy from the intermediate buffer into ByteOutputStream buffers.

I created a NullOutputStream to eliminate the second copy. This class sends 
all output stream writes to the JMH Blackhole mechanism
to make sure they don't get dead code eliminated out of existance but are basically ignored.
I added a baselineWrite test that doesn't do any copy but does invoke the stream write.

Run the benchmark via SBT with something like (3 warmups, 3 iterations):
```agsl
Jmh/run -i 3 -wi 3 -f1 -t1 .*Write.*
```

Varying the parameters of the data size and the size of the intermediate buffer gives the following results
in microseconds on my machine (iMac 3.3 GHz, Ventura 13.4.1, Scala 2.13.5, JDK-17.0.4):

| Method             | Data size = 100K | Data size = 1M | Data size = 10M | Data size 1M = bufferSize |
|--------------------|---------------:|---------------:|----------------:|--------------------------:|
| arraySequenceWrite |  9.089          | 89.827          | 1310.868          |                    71.738 |
| arraySliceWrite    | 8.998        | 89.543        | 1298.947        |                   147.759 |
| arrayWrite         | 1.734         | 17.406         | 331.405         |                    72.471 |
| baselineWrite      | 0.271          | 0.802          | 5.894           |                    36.397 |
| sequenceWrite      | 9.040          | 88.639          | 1310.345          |                    71.612 |
| vectorWrite        | 89.690          | 1045.637          | 16393.666          |                  1208.015 |


I stepped into the various Scala sequence implementations and they all end up invoking System.arraycopy
to do the actual copy but with overhead and not necessarily the same number of copies.

It is possible that the benchmark is chasing cache hit/miss phantoms and that
Sequences and Arrays are basically identical when used this way but the overhead
of Vectors, in particular, seems significant.
