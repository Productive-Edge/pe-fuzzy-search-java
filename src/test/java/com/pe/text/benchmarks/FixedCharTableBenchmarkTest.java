package com.pe.text.benchmarks;

import com.pe.hash.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class FixedCharTableBenchmarkTest {

    @Param({"2", "6", "23", "40"})
    public int length;

    private String pattern;
    private String text;

    private Char2IntMap ph;
    private Char2IntOpenHashMap map;

    @Setup(Level.Invocation)
    public void init() {
        pattern = new Random().ints(' ', 'z')
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

//        System.out.println(pattern);
        phNew();
        mapNew();

        text = new Random().ints(' ', 'z')
                .limit(1000)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void phNew() {
        ph = new Char2IntMap(pattern, -1);
        for (int i = 0; i < pattern.length(); i++)
            ph.put(pattern.charAt(i), 1);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void mapNew() {
        map = new Char2IntOpenHashMap(pattern.length());
        for (int i = 0; i < pattern.length(); i++)
            map.put(pattern.charAt(i), 1);
    }
    
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
                        .shouldDoGC(true)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void mapGet() {
        for (int i = 0; i < text.length(); i++)
            map.getOrDefault(text.charAt(i), -1);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void phGet() {
        for (int i = 0; i < text.length(); i++)
            ph.get(text.charAt(i));
    }

}
