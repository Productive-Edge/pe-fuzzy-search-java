package com.pe.text.benchmarks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OutOfOrderExecutionBenchmarkTest {

    private static final int[] ints = new int[1024];
    private static final int[] ires = new int[1024];

    private static final long[] longs = new long[1024];
    private static final long[] lres = new long[1024];

    private static final Random random = new Random();

    static {
        for (int i = 0; i < ints.length; i++) ints[i] = random.nextInt();
        for (int i = 0; i < longs.length; i++) longs[i] = random.nextLong();
    }

    @Disabled("benchmarks have to be run manually")
    @Test
    void runBenchmarks() throws Exception {
        new Runner(
                new OptionsBuilder()
                        .include(this.getClass().getName() + ".*")
                        .mode(Mode.AverageTime)
                        .warmupTime(TimeValue.seconds(1))
                        .warmupIterations(2)
                        .threads(2)
                        .measurementIterations(5)
                        .measurementTime(TimeValue.seconds(1))
                        .forks(1)
                        .shouldDoGC(false)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void sequential() {
        for (int i = 0; i < ints.length; i++) {
            final int deletion = ints[i];
            // these two lines are slower due to out of order optimization can not be applied
            final int substitution = deletion << 1;
            final int insertion = substitution << 1;
            ires[i] = deletion & substitution & insertion;
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void outOfOrder() {
        for (int i = 0; i < ints.length; i++) {
            final int deletion = ints[i];
            // these two lines are faster due to out of order optimization can be applied
            final int substitution = deletion << 1;
            final int insertion = deletion << 2;
            ires[i] = deletion & substitution & insertion;
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void sequential64() {
        for (int i = 0; i < longs.length; i++) {
            final long deletion = longs[i];
            final long substitution = deletion << 1;
            final long insertion = substitution << 1;
            lres[i] = deletion & substitution & insertion;
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void outOfOrder64() {
        for (int i = 0; i < longs.length; i++) {
            final long deletion = longs[i];
            final long substitution = deletion << 1;
            final long insertion = deletion << 2;
            lres[i] = deletion & substitution & insertion;
        }
    }
}