package com.pe.text;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiPatternBenchmarkTest {

    public static final FuzzyPattern UT = FuzzyPattern.pattern("ut", 0, true);
    public static final FuzzyPattern DUIS = FuzzyPattern.pattern("Duis", 1);
    public static final FuzzyPattern DOLOR = FuzzyPattern.pattern("dolor", 1);

    public static final FuzzyMultiPattern patterns = FuzzyMultiPattern.combine(
            UT,
            DUIS,
            DOLOR
    );

    @Deprecated
    public static final FuzzyMultiPattern patternsV2 = new MultiplePatterns(new FuzzyPattern[]{
            UT,
            DUIS,
            DOLOR
    });

    public static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
            "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
            "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    public static final String TEXT_LONG = StringUtils.repeat(TEXT, 100);

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
                        .shouldDoGC(true)
                        .build()
        ).run();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void multiPatternInSingleScanAll() {
        Stream<FuzzyResult> resultStream = patterns.matcher(TEXT).stream();
        resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(","));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Deprecated
    public void multiPattern2InSingleScanAll() {
        Stream<FuzzyResult> resultStream = patternsV2.matcher(TEXT).stream();
        resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(","));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void usualPatternInMultiScansAll() {
        Stream<FuzzyResult> uts = UT.matcher(TEXT).stream();
        uts.map(FuzzyResult::foundText).collect(Collectors.joining(","));

        Stream<FuzzyResult> duis = DUIS.matcher(TEXT).stream();
        duis.map(FuzzyResult::foundText).collect(Collectors.joining(","));

        Stream<FuzzyResult> $ = DOLOR.matcher(TEXT).stream();
        $.map(FuzzyResult::foundText).collect(Collectors.joining(","));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void multiPatternInSingleScanLongAll() {
        Stream<FuzzyResult> resultStream = patterns.matcher(TEXT_LONG).stream();
        resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(","));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Deprecated
    public void multiPattern2InSingleScanLongAll() {
        Stream<FuzzyResult> resultStream = patternsV2.matcher(TEXT_LONG).stream();
        resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(","));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void usualPatternInMultiScansLongAll() {
        Stream<FuzzyResult> uts = UT.matcher(TEXT).stream();
        uts.map(FuzzyResult::foundText).collect(Collectors.joining(","));

        Stream<FuzzyResult> duis = DUIS.matcher(TEXT).stream();
        duis.map(FuzzyResult::foundText).collect(Collectors.joining(","));

        Stream<FuzzyResult> $ = DOLOR.matcher(TEXT).stream();
        $.map(FuzzyResult::foundText).collect(Collectors.joining(","));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void multiPatternInSingleScanLongFirst() {
        Stream<FuzzyResult> resultStream = patterns.matcher(TEXT_LONG).stream();
        resultStream.findFirst().get();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Deprecated
    public void multiPattern2InSingleScanLongFirst() {
        Stream<FuzzyResult> resultStream = patternsV2.matcher(TEXT_LONG).stream();
        resultStream.findFirst().get();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void usualPatternInMultiScansLongFirst() {
        Stream<FuzzyResult> uts = UT.matcher(TEXT).stream().limit(1);
        Stream<FuzzyResult> duis = DUIS.matcher(TEXT).stream().limit(1);
        Stream<FuzzyResult> $ = DOLOR.matcher(TEXT).stream().limit(1);
        Stream.of(uts, duis, $).flatMap(Function.identity())
                .min(Comparator.comparingInt(FuzzyResult::start))
                .get();
    }
}
