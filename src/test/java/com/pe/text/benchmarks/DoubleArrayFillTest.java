package com.pe.text.benchmarks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class DoubleArrayFillTest {

    private final static int[] a1 = new int[128];
    private final static int[] a2 = new int[128];

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
                        .warmupIterations(2)
                        .threads(1)
                        .measurementIterations(5)
                        .measurementTime(TimeValue.seconds(2))
                        .forks(1)
                        .shouldDoGC(false)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void arrayFill() {
        Arrays.fill(a1, 1, length, 0);
        Arrays.fill(a2, 1, length, 0);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void doubleLoop() {
        for (int i = 1; i < length; i++) a1[i] = 0;
        for (int i = 1; i < length; i++) a2[i] = 0;
    }

    /**
     * slowest
     */
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void singleLoop() {
        for (int i = 1; i < length; i++) {
            a1[i] = 0;
            a2[i] = 0;
        }
    }

}
