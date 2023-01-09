package com.pe.juzzy.benchmarks;

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

public class CyclesExecution {

    private static final int[] ints = new int[1024];

    @Disabled
    @Test
    public void runBenchmarks() throws Exception {
        new Runner(
                new OptionsBuilder()
                        .include(this.getClass().getName() + ".*")
                        .mode(Mode.AverageTime)
                        .warmupTime(TimeValue.seconds(1))
                        .warmupIterations(2)
                        .threads(2)
                        .measurementIterations(5)
                        .measurementTime(TimeValue.seconds(2))
                        .forks(1)
                        .shouldDoGC(false)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void cycleUsual() {
        for (int i = 0; i < ints.length; i++) ints[i] = i;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void cycleInlineLength() {
        for (int i = 0, l = ints.length; i < l; i++) ints[i] = i;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void cycleFinalVarLength() {
        final int l = ints.length;
        for (int i = 0; i < l; i++) ints[i] = i;
    }
}