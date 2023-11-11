package com.pe.text;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiPatternBenchmarkTest {

    public static final FuzzyPattern UT = FuzzyPattern.pattern("ut", 0, true);
    public static final FuzzyPattern DUIS = FuzzyPattern.pattern("Duis", 1);
    public static final FuzzyPattern DOLOR = FuzzyPattern.pattern("dolor", 1);

    public static final FuzzyMultiPattern iterativeMultiPattern = FuzzyMultiPattern.combine(
            UT,
            DUIS,
            DOLOR
    );

    @Deprecated
    public static final FuzzyMultiPattern multiPattern = new MultiplePatterns(new FuzzyPattern[]{
            UT,
            DUIS,
            DOLOR
    });

    public static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
            "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
            "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    public static final String LONG_TEXT = StringUtils.repeat(TEXT, 100);

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

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void iterativeMultiPattern_FindAll() {
        Stream<FuzzyResult> resultStream = iterativeMultiPattern.matcher(TEXT).stream();
        Assertions.assertFalse(
                resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void multiPattern_FindAll() {
        Stream<FuzzyResult> resultStream = multiPattern.matcher(TEXT).stream();
        Assertions.assertFalse(
                resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void oneByOne_FindAll() {
        Stream<FuzzyResult> uts = UT.matcher(TEXT).stream();
        Assertions.assertFalse(
                uts.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());

        Stream<FuzzyResult> duis = DUIS.matcher(TEXT).stream();
        Assertions.assertFalse(
                duis.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());

        Stream<FuzzyResult> $ = DOLOR.matcher(TEXT).stream();
        Assertions.assertFalse(
                $.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void iterativeMultiPattern_FindAll_LongText() {
        Stream<FuzzyResult> resultStream = iterativeMultiPattern.matcher(LONG_TEXT).stream();
        Assertions.assertFalse(
                resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void multiPattern_FindAll_LongText() {
        Stream<FuzzyResult> resultStream = multiPattern.matcher(LONG_TEXT).stream();
        Assertions.assertFalse(
                resultStream.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void oneByOne_FindAll_LongText() {
        Stream<FuzzyResult> uts = UT.matcher(LONG_TEXT).stream();
        Assertions.assertFalse(
                uts.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());

        Stream<FuzzyResult> duis = DUIS.matcher(LONG_TEXT).stream();
        Assertions.assertFalse(
                duis.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());

        Stream<FuzzyResult> $ = DOLOR.matcher(LONG_TEXT).stream();
        Assertions.assertFalse(
                $.map(FuzzyResult::foundText).collect(Collectors.joining(",")).isEmpty());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void iterativeMultiPattern_FindFirst_LongText() {
        Stream<FuzzyResult> resultStream = iterativeMultiPattern.matcher(LONG_TEXT).stream();
        Assertions.assertTrue(resultStream.findFirst().isPresent());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void multiPattern_FindFirst_LongText() {
        Stream<FuzzyResult> resultStream = multiPattern.matcher(LONG_TEXT).stream();
        Assertions.assertTrue(resultStream.findFirst().isPresent());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void oneByOne_FindFirst_LongText() {
        Stream<FuzzyResult> uts = UT.matcher(LONG_TEXT).stream().limit(1);
        Stream<FuzzyResult> duis = DUIS.matcher(LONG_TEXT).stream().limit(1);
        Stream<FuzzyResult> $ = DOLOR.matcher(LONG_TEXT).stream().limit(1);
        Optional<FuzzyResult> first = Stream.of(uts, duis, $).flatMap(Function.identity())
                .min(Comparator.comparingInt(FuzzyResult::start));
        Assertions.assertTrue(first.isPresent());
    }
}
