package com.pe.text.benchmarks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class LengthChangesBenchmarkTest {

    @Param({"4", "16", "64", "128"})
    public int length;

    @Disabled("benchmarks have to be run manually")
    @Test
    void runBenchmarks() throws Exception {
        new Runner(
                new OptionsBuilder()
                        .include(this.getClass().getName() + ".*")
                        .mode(Mode.AverageTime)
                        .warmupTime(TimeValue.seconds(1))
                        .warmupIterations(1)
                        .threads(1)
                        .measurementIterations(3)
                        .measurementTime(TimeValue.seconds(1))
                        .forks(1)
                        .shouldDoGC(false)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bytesArray() {
        Random random = new Random(0x5eedL);
        byte[] bytes = new byte[length];
        for (int i = 0; i < 100; i++) {
            for (int j = 1; j < length; j++) {
                final int bitValue = random.nextBoolean() ? 1 : 0;
                bytes[j] = (byte) (random.nextBoolean() ? -bitValue : bitValue);
            }
            int sum = 0;
            for (int j = 1; j < length; j++) sum += bytes[j];
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void charsArray() {
        Random random = new Random(0x5eedL);
        char[] chars = new char[length];
        for (int i = 0; i < 100; i++) {
            for (int j = 1; j < length; j++) {
                final int bitValue = random.nextBoolean() ? 1 : 0;
                chars[j] = (char) (random.nextBoolean() ? -bitValue : bitValue);
            }
            int sum = 0;
            for (int j = 1; j < length; j++) sum += chars[j];
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void intsArray() {
        Random random = new Random(0x5eedL);
        int[] ints = new int[length];
        for (int i = 0; i < 100; i++) {
            for (int j = 1; j < length; j++) {
                final int bitValue = random.nextBoolean() ? 1 : 0;
                ints[j] = random.nextBoolean() ? -bitValue : bitValue;
            }
            int sum = 0;
            for (int j = 1; j < length; j++) sum += ints[j];
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void longsArray() {
        Random random = new Random(0x5eedL);
        long[] longs = new long[length];
        for (int i = 0; i < 100; i++) {
            for (int j = 1; j < length; j++) {
                final long bitValue = random.nextBoolean() ? 1L : 0L;
                longs[j] = random.nextBoolean() ? -bitValue : bitValue;
            }
            long sum = 0L;
            for (int j = 1; j < length; j++) sum += longs[j];
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void longBits() {
        Random random = new Random(0x5eedL);
        for (int i = 0; i < 100; i++) {
            long bits = 0L, signs = 0L;
            for (int j = 1; j < length; j++) {
                bits <<= 1;
                bits &= random.nextBoolean() ? 1L : 0L;
                signs <<= 1;
                signs &= random.nextBoolean() ? 1L : 0L;
            }
            long sum = 0L;
            for (int j = 1; j < length; j++) {
                sum += (signs & 1L) == 0L ? (bits & 1L) : -(bits & 1L);
                signs >>>= 1;
                bits >>>= 1;
            }
        }
    }

}
