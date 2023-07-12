package app.continuity

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.io.OutputStream
import java.util.concurrent.TimeUnit
import scala.collection.ArrayOps
import scala.collection.immutable.{ArraySeq, Vector}
import scala.util.Random

object SequenceTest  {
  @State(Scope.Benchmark)
  class SequenceTest {
    var size: Int = 1000000
    val bufferSize: Int = 4096

    var data: Array[Byte] = _
    var dataAsSeq: IndexedSeq[Byte] = _
    var dataAsVector: Vector[Byte] = _
    var dataAsArraySeq: ArraySeq[Byte] = _

    @Setup
    def prepare(): Unit = {
      data = new Random().nextBytes(size)
      dataAsSeq = data.toIndexedSeq
      dataAsVector = Vector(data: _*)
      dataAsArraySeq = ArraySeq(data: _*)
    }

    @Benchmark
    @BenchmarkMode(Array(Mode.AverageTime))
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    def baselineWrite(hole: Blackhole): Unit = {
      val buffer = new Array[Byte](bufferSize)
      val output = new NullOutputStream(hole)
      var position: Int = 0
      while (position < data.length) {
        val writeSize = Math.min(data.length - position, buffer.length)
        output.write(buffer, 0, writeSize)
        position += writeSize
      }
    }

    @Benchmark
    @BenchmarkMode(Array(Mode.AverageTime))
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    def arrayWrite(hole: Blackhole): Unit = {
      val buffer = new Array[Byte](bufferSize)
      val output = new NullOutputStream(hole)
      var position: Int = 0
      while (position < data.length) {
        val writeSize = Math.min(data.length - position, buffer.length)
        System.arraycopy(data, position, buffer, 0, writeSize)
        output.write(buffer, 0, writeSize)
        position += writeSize
      }
    }

    @Benchmark
    @BenchmarkMode(Array(Mode.AverageTime))
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    def arraySliceWrite(hole: Blackhole): Unit = {
      val buffer = new Array[Byte](bufferSize)
      val output = new NullOutputStream(hole)
      var position: Int = 0
      while (position < data.length) {
        val writeSize = Math.min(data.length - position, buffer.length)
        (data: ArrayOps[Byte]).slice(position, position + writeSize).copyToArray(buffer)
        output.write(buffer, 0, writeSize)
        position += writeSize
      }
    }


    @Benchmark
    @BenchmarkMode(Array(Mode.AverageTime))
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    def sequenceWrite(hole: Blackhole): Unit = {
      val output = new NullOutputStream(hole)
      val buffer = new Array[Byte](bufferSize)
      val data = dataAsSeq
      var position: Int = 0
      while (position < data.length) {
        val writeSize = Math.min(data.length - position, buffer.length)
        data.slice(position, position + writeSize).copyToArray(buffer, 0, writeSize)
        output.write(buffer, 0, writeSize)
        position += writeSize
      }
    }

    @Benchmark
    @BenchmarkMode(Array(Mode.AverageTime))
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    def vectorWrite(hole: Blackhole): Unit = {
      val output = new NullOutputStream(hole)
      val buffer = new Array[Byte](bufferSize)
      val data = dataAsVector
      var position: Int = 0
      while (position < data.length) {
        val writeSize = Math.min(data.length - position, buffer.length)
        data.slice(position, position + writeSize).copyToArray(buffer, 0, writeSize)
        output.write(buffer, 0, writeSize)
        position += writeSize
      }
    }

    @Benchmark
    @BenchmarkMode(Array(Mode.AverageTime))
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    def arraySequenceWrite(hole: Blackhole): Unit = {
      val output = new NullOutputStream(hole)
      val buffer = new Array[Byte](bufferSize)
      val data = dataAsArraySeq
      var position: Int = 0
      while (position < data.length) {
        val writeSize = Math.min(data.length - position, buffer.length)
        data.slice(position, position + writeSize).copyToArray(buffer, 0, writeSize)
        output.write(buffer, 0, writeSize)
        position += writeSize
      }
    }

    class NullOutputStream(hole: Blackhole) extends OutputStream {
      override def write(b: Int): Unit = {
        hole.consume(b)
      }

      override def write(b: Array[Byte]): Unit = {
        hole.consume(b)
      }

      override def write(b: Array[Byte], off: Int, len: Int): Unit = {
        hole.consume(b)
      }
    }
  }

}

