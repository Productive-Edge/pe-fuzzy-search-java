package com.pe.text.benchmarks;

import com.pe.text.FuzzyPattern;
import com.pe.text.Levenshtein;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@Deprecated
public class LevenshteinDistanceBenchmarkTest {

    public static final String test1a = "This is a test text of testing";
    public static final String test1b = "teksting";
    public static final FuzzyPattern noLimitPattern = FuzzyPattern.pattern(test1b, test1b.length());
    public static final FuzzyPattern limit3Pattern = FuzzyPattern.pattern(test1b, 3);


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
                        .measurementTime(TimeValue.seconds(2))
                        .forks(1)
                        .shouldDoGC(true)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void apache1() {
        LevenshteinDistance.getDefaultInstance().apply(test1a, test1b);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Deprecated
    public void bitap1() {
        Levenshtein.distance(test1a, test1b);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bitap1m() {
        noLimitPattern.matcher(test1a).findTheBest();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bitap1l() {
        limit3Pattern.matcher(test1a).findTheBest();
    }
}
