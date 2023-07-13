# Benchmark related to StackOverflow question: [Is there a way to use Scala sequences with performance comparable to Java arrays?](https://github.com/jlgula/Benchmark/new/main?readme=1)

This benchmark copies data from an array or Scala sequence into an intermediate buffer
and from there to an output stream. The original question was why writing immutable sequences
to an OutputStream is slower than keeping data in a mutable arrays. Since OutputStream
writes expect an array argument, when writing a sequence, it has to be buffered through
an array, requiring an extra copy. But that copy from a sequence is significantly slower than the same
copy coming from an array. 

The copy is obviously not needed if the source is known to
be an array as it can be written directly to the stream but that means propagating mutable
data through the system and hoping nobody changes it.

Following the suggestion of [stefanobaghino](https://stackoverflow.com/users/3314107/stefanobaghino), I rewrote the benchmark tests to use the
[Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh) with the [SBT Plugin](https://github.com/sbt/sbt-jmh)

The original code did a double copy:
1) Copy from data in an array or sequence into the intermediate buffer
2) Copy from the intermediate buffer into ByteOutputStream buffers.

Since I really only interested in the first copy,
I created a NullOutputStream to eliminate the second copy. NullOutputStream sends 
all output stream writes to the JMH Blackhole mechanism
to make sure they don't get dead code eliminated out of existance but are basically ignored.
I added a baselineWrite test that doesn't do the source copy but does invoke the stream write.
I added another version that writes directly from the array to the output stream
eliminating the extra buffer copy. Using the Blackhole OutputStream makes this
more or less instantaneous and so the benchmark just measures a single write call to
NullOutputStream.

Somebody suggested using sliding rather than slice to access portions of the source
sequence, eliminating the while loop. Using sliding from a sequence still requires the copy to an array for the OutputStream write
and sliding seems to be very slow.

Run the benchmark via SBT with something like (3 warmups, 3 iterations):
```agsl
Jmh/run -i 3 -wi 3 -f1 -t1 .*Write.*
```

The default values in the code are data size 1M and buffer size 4096. Varying the parameters of the data size and the size of the intermediate buffer gives the following results
in microseconds on my machine (iMac 3.3 GHz, Ventura 13.4.1, Scala 2.13.5, JDK-17.0.4):

| Method             | Data size = 100K | Data size = 1M | Data size = 10M | Data size 1M = bufferSize |
|--------------------|---------------:|---------------:|----------------:|--------------------------:|
| arraySequenceWrite |  9.089          | 89.827          | 1310.868          |                    71.738 |
| arraySliceWrite    | 8.998        | 89.543        | 1298.947        |                   147.759 |
| arrayWrite         | 1.734         | 17.406         | 331.405         |                    72.471 |
| baselineWrite      | 0.271          | 0.802          | 5.894           |                    36.397 |
| sequenceWrite      | 9.040          | 88.639          | 1310.345          |                    71.612 |
| slidingSequenceWrite        | 327633.306          | 3261447.140          | 33343846.372          |                  8740.842 |
| unbufferedArrayWrite        | 0.003          | 0.003          | 0.003          |             0.003 |
| vectorWrite        | 89.690          | 1045.637          | 16393.666          |                  1208.015 |


I stepped into the various Scala sequence implementations and they all end up invoking System.arraycopy
to do the actual copy but with overhead and not necessarily the same number of copies.

It is possible that the benchmark is chasing cache hit/miss phantoms and that
Sequences and Arrays are basically identical when used this way but the overhead
of Vectors, in particular, seems significant.
