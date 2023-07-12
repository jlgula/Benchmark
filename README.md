# Benchmark related to StackOverflow question: [Is there a way to use Scala sequences with performance comparable to Java arrays?](https://github.com/jlgula/Benchmark/new/main?readme=1)

Following the suggestion of stefanobaghino, I rewrote the benchmark tests to use the
[Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh) with the [SBT Plugin](https://github.com/sbt/sbt-jmh)

The original code did a double copy:
1) Copy from data into the intermediate buffer
2) Copy from the intermediate buffer into ByteOutputStream buffers.

I created a NullOutputStream to eliminate the second copy. This class sends 
all output stream writes to the JMH Blackhole mechanism
to make sure they don't get dead code eliminated out of existance but are basically ignored.

Run the benchmark via SBT with something like (3 warmups, 3 iterations):
Jmh/run -i 3 -wi 3 -f1 -t1 .*Write.*

Varying the parameters of the data size and the size of the intermediate buffer gives the following results
on my machine (iMac 3.3 GHz, Ventura 13.4.1, Scala 2.13.5, JDK-17.0.4):

| Method             | Data size = 1M | Data size = 10M | Data size 10M = bufSize |
|--------------------|---------------:|----------------:|-------------------------|
| arraySequenceWrite | 2.311          | 17.902          | 1554.723                |
| arraySliceWrite    | 101.275        | 1507.398        | 3244.150                |
| arrayWrite         | 20.553         | 429.147         | 1709.232                |
| baselineWrite      | 0.824          | 6.079           | 553.850                 |
| sequenceWrite      | 2.312          | 17.681          | 1660.771                |
| vectorWrite        | 5.145          | 14.136          | 17221.674               |

I have no good explanation of why the array version takes longer, the slice version
even longer and the code seems to blow up when the buffer matches the data size.
In principal this should be fastest as the loop is only executed once.

I stepped into the various Scala sequences and they all end up invoking System.arraycopy
to do the actual copy.

My suspicion is that this benchmark is chasing cache hit/miss phantoms and that
Sequences and Arrays are basically identical when used this way.
