package com.pe.hash;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class FixedCharTableUtfBenchmarkTest {

    @Param({"2", "12", "40"})
    public int length;

    FixedCharTable universal;
    FixedCharTable cuckoo;

    FixedCharTable generic;

    private String pattern;
    private String text;

    @Setup(Level.Invocation)
    public void init() {
        pattern = new Random().ints(' ', (char) 0xffff)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

//        System.out.println(pattern);
        universalNew();
        cuckooNew();
        genericNew();

        text = new Random().ints(' ', (char) 0xffff)
                .limit(1000)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    //    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void universalNew() {
        universal = new FCTUniversal(pattern);
    }

    //    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void cuckooNew() {
        FCTUniversal u = new FCTUniversal(pattern);
        FCTCuckoo ck = new FCTCuckoo(Arrays.stream(u.chars).filter(c -> c >= 0).toArray(), 200);
        if (!ck.found()) {
            System.err.println("Not found for " + pattern);
            cuckoo = u;
        } else {
            cuckoo = ck;
        }
    }

    //    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void genericNew() {
        generic = FixedCharTable.from(pattern);
    }

    @Disabled("benchmarks have to be run manually")
    @Test
    void runBenchmarks() throws Exception {
        new Runner(
                new OptionsBuilder()
                        .include(this.getClass().getName() + ".*")
                        .mode(Mode.AverageTime)
                        .warmupTime(TimeValue.milliseconds(500))
                        .warmupIterations(1)
                        .threads(1)
                        .measurementIterations(3)
                        .measurementTime(TimeValue.milliseconds(500))
                        .forks(1)
                        .shouldDoGC(true)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void universalGet() {
        for (int i = 0; i < text.length(); i++)
            universal.indexOf(text.charAt(i));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void cuckooGet() {
        for (int i = 0; i < text.length(); i++)
            cuckoo.indexOf(text.charAt(i));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void genericGet() {
        for (int i = 0; i < text.length(); i++)
            cuckoo.indexOf(text.charAt(i));
    }
}
