package com.pe.text;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.stream.Stream;

/**
 * Fallback implementation of the {@link FuzzyPatterns} if one of the patterns doesn't implement {@link IterativeFuzzyPattern}
 * This implementation calls find for all patterns at the begging and then lazily continues searching using priority queue.
 */
class MultiplePatterns implements FuzzyPatterns {

    private static final Comparator<FuzzyMatcher> START_POSITION_COMPARATOR =
            Comparator.comparing(MultiplePatterns::getStartPositionSafely)
                    .thenComparing(MultiplePatterns::getEndPositionSafely);
    private final FuzzyPattern[] patterns;

    public MultiplePatterns(FuzzyPattern[] patterns) {
        this.patterns = patterns;
    }

    private static int getStartPositionSafely(FuzzyMatcher matcher) {
        return matcher.started() ? matcher.start() : -1;
    }

    private static int getEndPositionSafely(FuzzyMatcher matcher) {
        return matcher.started() ? matcher.end() : -1;
    }

    @Override
    public FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    class Matcher implements DefaultFuzzyMatcher {
        final FuzzyMatcher[] matchers;
        final PriorityQueue<FuzzyMatcher> queue;
        private CharSequence text;

        private int toIndex;

        Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.toIndex = toIndex;
            this.matchers = new IterativeFuzzyMatcher[patterns.length];
            this.queue = new PriorityQueue<>(patterns.length, START_POSITION_COMPARATOR);
            for (int i = 0, l = this.matchers.length; i < l; i++) {
                this.matchers[i] = MultiplePatterns.this.patterns[i].matcher(text, fromIndex, toIndex);
                this.queue.add(matchers[i]);
            }
        }

        @Override
        public void reset(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.toIndex = toIndex;
            this.queue.clear();
            for (FuzzyMatcher matcher : this.matchers) {
                matcher.reset(text, fromIndex, toIndex);
            }
        }

        @Override
        public CharSequence text() {
            return this.text;
        }

        @Override
        public boolean find() {
            while (!this.queue.isEmpty()) {
                FuzzyMatcher matcher = this.queue.poll();
                // end position of the current matching
                final int position = !matcher.started() ? -1 : matcher.end();
                // return matcher to the queue if it has next matching
                if (matcher.find())
                    this.queue.add(matcher);
                // get the next matcher and keep it in the queue
                matcher = this.queue.peek();
                // no matchers in the queue - search is stopped
                if (matcher == null) return false;
                // continue looping to initialize (start) all matchers in the queue
                if (!matcher.started()) continue;
                // remove overlapping matchings
                while (matcher.start() < position) {
                    matcher = Objects.requireNonNull(this.queue.poll());
                    matcher.reset(this.text, position, this.toIndex);
                    if (matcher.find()) {
                        this.queue.add(matcher);
                    }
                    matcher = this.queue.peek();
                    if (matcher == null)
                        return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public int to() {
            return this.toIndex;
        }

        @Override
        public boolean started() {
            return this.matchers[0].started();
        }

        @Override
        public boolean completed() {
            return started() && this.queue.isEmpty();
        }

        @Override
        public int from() {
            return this.matchers[0].from();
        }

        @Override
        public int start() {
            return ensureFound().start();
        }

        @Override
        public int end() {
            return ensureFound().end();
        }

        @Override
        public CharSequence foundText() {
            return ensureFound().foundText();
        }

        @Override
        public FuzzyPattern pattern() {
            return ensureFound().pattern();
        }

        @Override
        public int distance() {
            return ensureFound().distance();
        }

        @Override
        public Stream<OperationType> streamEditTypes() {
            return ensureFound().streamEditTypes();
        }

        @Override
        public FuzzyMatcher ensureFound() {
            DefaultFuzzyMatcher.super.ensureFound();
            return this.queue.peek();
        }
    }
}
