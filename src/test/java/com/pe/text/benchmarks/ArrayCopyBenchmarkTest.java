package com.pe.text.benchmarks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ArrayCopyBenchmarkTest {

    private final static int[] ints = new int[128];
    private final static int[] copy = new int[ints.length];

    @Param({"0", "1"})
    public int offset;

    @Param({"4", "8", "16", "32", "48", "64", "128"})
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
    public void systemArrayCopy() {
        System.arraycopy(ints, offset, copy, offset, length - offset);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void loopCopy() {
        int l = length - offset;
        for (int i = offset; i < l; i++) copy[i] = ints[i];
    }
}
