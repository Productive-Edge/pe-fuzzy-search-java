package com.pe.juzzy.benchmarks;

import com.pe.juzzy.JuzzyPattern;
import com.pe.juzzy.Levenshtein;
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

public class LevenshteinDistanceBenchmark {

    public static final String test1a = "This is a test text of testing";
    public static final String test1b = "teksting";
    public static final JuzzyPattern pattern1 = JuzzyPattern.pattern(test1b, test1b.length());
    public static final JuzzyPattern pattern1l = JuzzyPattern.pattern(test1b, 3);


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
    public void bitap1() {
        Levenshtein.distance(test1a, test1b);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bitap1m() {
        pattern1.matcher(test1a).findTheBestMatching();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bitap1l() {
        pattern1l.matcher(test1a).findTheBestMatching();
    }
}
