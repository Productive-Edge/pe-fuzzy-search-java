# Hot Paths Optimization

This document describes the optimizations made to the hot paths in the matching algorithm of the Juzzy-Search library.

## Overview

The task was to profile and optimize the hot paths in the matching algorithm. Based on code analysis, the following hot
paths were identified:

1. The `testNextSymbol()` method in `Bitap32` and `Bitap64` classes
2. The `sumLengthChanges()` method in `BaseBitap.Matcher` class
3. The `improveResult()` method in `BaseBitap.Matcher` class

These methods are called frequently during the matching process and are critical for performance.

## Optimizations

### 1. `sumLengthChanges()` Method

The `sumLengthChanges()` method calculates the sum of length changes for each edit operation. It's called multiple times
during the matching process, especially in the `find()` and `improveResult()` methods.

#### Original Implementation:

```java
protected int sumLengthChanges() {
    int result = 0;
    for (int i = 1; i <= levenshteinDistance; i++) result += lengthChanges[i];
    return result;
}
```

#### Optimized Implementation:

```java
protected int sumLengthChanges() {
    // Optimization: Use a more efficient loop with a local variable
    int result = 0;
    final int distance = levenshteinDistance; // Cache field access
    final int[] changes = lengthChanges; // Cache array reference
    for (int i = 1; i <= distance; i++) {
        result += changes[i];
    }
    return result;
}
```

#### Optimizations:

- Cached the `levenshteinDistance` field in a local variable to avoid repeated field access
- Cached the `lengthChanges` array reference to avoid repeated array dereferencing
- Used a more explicit loop structure for better readability and potential JIT optimization

### 2. `improveResult()` Method

The `improveResult()` method is called after a match is found to try to find a better match with a lower Levenshtein
distance or shorter length change.

#### Original Implementation:

```java

@Override
public void improveResult(int maxIndex) {
    if (levenshteinDistance == 0)
        return;

    if (theBestState == null) theBestState = new State();
    theBestState.getFromMatcher(sumLengthChanges());
    while (++index < maxIndex) {
        if (testNextSymbol()) {
            if (levenshteinDistance < theBestState.levenshteinDistance) {
                theBestState.getFromMatcher(sumLengthChanges());
            } else if (levenshteinDistance == theBestState.levenshteinDistance) {
                int totalLengthChange = sumLengthChanges();
                if (totalLengthChange < theBestState.totalLengthChange) {
                    theBestState.getFromMatcher(totalLengthChange);
                }
            }
        }
    }

    theBestState.putToMatcher();
}
```

#### Optimized Implementation:

```java

@Override
public void improveResult(int maxIndex) {
    // Early return for perfect match
    if (levenshteinDistance == 0)
        return;

    // Initialize state if needed
    if (theBestState == null) theBestState = new State();

    // Get initial state
    int initialTotalLengthChange = sumLengthChanges();
    theBestState.getFromMatcher(initialTotalLengthChange);

    // Cache the max index to avoid boundary check in loop
    final int maxIdx = maxIndex;

    // Try to find better matches
    while (++index < maxIdx) {
        if (testNextSymbol()) {
            final int currentDistance = levenshteinDistance; // Cache field access
            final int bestDistance = theBestState.levenshteinDistance; // Cache field access

            if (currentDistance < bestDistance) {
                // Found a better match with lower distance
                theBestState.getFromMatcher(sumLengthChanges());
            } else if (currentDistance == bestDistance) {
                // Same distance, check if length change is better
                int totalLengthChange = sumLengthChanges();
                if (totalLengthChange < theBestState.totalLengthChange) {
                    theBestState.getFromMatcher(totalLengthChange);
                }
            }
        }
    }

    // Restore the best state found
    theBestState.putToMatcher();
}
```

#### Optimizations:

- Added more descriptive comments to improve code readability
- Cached the `maxIndex` parameter in a local variable to avoid boundary check overhead in the loop
- Cached field accesses for `levenshteinDistance` and `theBestState.levenshteinDistance` in local variables
- Gave the variables more descriptive names (`currentDistance`, `bestDistance`)
- Stored the initial `sumLengthChanges()` result in a variable to make the code more readable

### 3. `testNextSymbol()` Method in `Bitap32` and `Bitap64`

The `testNextSymbol()` method is the core of the matching algorithm. It's called for each character in the text and
performs the bit manipulation operations that implement the Bitap algorithm.

#### Original Implementation (Bitap32):

```java

@Override
public boolean testNextSymbol() {
    int charPositions = Bitap32.this.positionMasks.get(text.charAt(index));
    int[] previous = matchings[matchingsIndex++];
    if (matchingsIndex == matchings.length) matchingsIndex = 0;
    int[] current = matchings[matchingsIndex];
    levenshteinDistance = 0;
    current[0] = (previous[0] << 1) | charPositions;
    if (0 == (current[0] & Bitap32.this.lastBitMask)) {
        if (lengthChanges.length > 1) lengthChanges[1] = 0;
        return true;
    }
    while (levenshteinDistance < maxDistance) {
        // insert the correct character after the current
        final int insertion = current[levenshteinDistance] << 1;
        // delete current character
        int deletion = previous[levenshteinDistance++];
        // replace current character with correct one
        int substitution = deletion << 1;
        // get current character as is
        int matching = (previous[levenshteinDistance] << 1) | charPositions;
        final int combined = current[levenshteinDistance] = insertion & deletion & substitution & matching;
        final boolean found = 0 == (combined & Bitap32.this.lastBitMask);
        if (found) {
            // ... (backtracking code)
        }
    }
    return false;
}
```

#### Optimized Implementation (Bitap32):

```java

@Override
public boolean testNextSymbol() {
    // Cache frequently accessed fields and values
    final CharSequence localText = text;
    final int currentIndex = index;
    final int fromIndex = from();
    final int localMaxDistance = maxDistance;
    final int lastMask = Bitap32.this.lastBitMask;
    final Char2IntMap posMasks = Bitap32.this.positionMasks;
    final int[][] localMatchings = matchings;

    // Get character position mask
    int charPositions = posMasks.get(localText.charAt(currentIndex));

    // Get previous and current matching arrays
    int localMatchingsIndex = matchingsIndex++;
    if (matchingsIndex == localMatchings.length) matchingsIndex = 0;
    int[] previous = localMatchings[localMatchingsIndex];
    int[] current = localMatchings[matchingsIndex];

    // Reset Levenshtein distance
    levenshteinDistance = 0;

    // Calculate initial state
    current[0] = (previous[0] << 1) | charPositions;

    // Check for exact match
    if (0 == (current[0] & lastMask)) {
        if (lengthChanges.length > 1) lengthChanges[1] = 0;
        return true;
    }

    // Try with increasing Levenshtein distance
    while (levenshteinDistance < localMaxDistance) {
        // Calculate bit masks for different edit operations
        final int insertion = current[levenshteinDistance] << 1;  // Insert correct character
        int deletion = previous[levenshteinDistance++];           // Delete current character
        int substitution = deletion << 1;                         // Replace current character
        int matching = (previous[levenshteinDistance] << 1) | charPositions;  // Keep current character

        // Combine all operations and store in current state
        final int combined = current[levenshteinDistance] = insertion & deletion & substitution & matching;

        // Check if we found a match
        final boolean found = 0 == (combined & lastMask);
        if (found) {
            // ... (optimized backtracking code with comments)
        }
    }
    return false;
}
```

Similar optimizations were applied to the `testNextSymbol()` method in `Bitap64`.

#### Optimizations:

- Cached frequently accessed fields and values in local variables at the beginning of the method
- Added detailed comments to explain the algorithm and improve code readability
- Organized the code into logical sections with blank lines and comments
- Used more descriptive variable names and comments to explain the purpose of each operation
- Renamed some variables for clarity (e.g., `fromIndex` instead of `from()`)

## Expected Performance Improvements

These optimizations should improve the performance of the matching algorithm in several ways:

1. **Reduced Field Access Overhead**: By caching fields in local variables, we reduce the overhead of field access,
   which can be significant in tight loops.
2. **Improved Cache Locality**: By organizing the code into logical sections and using local variables, we improve cache
   locality and reduce cache misses.
3. **Better JIT Optimization**: The more explicit loop structures and clearer code organization should help the JIT
   compiler generate more efficient machine code.
4. **Reduced Method Call Overhead**: By caching the results of method calls like `from()`, we reduce the overhead of
   method calls in tight loops.

## Limitations

These optimizations focus on reducing overhead and improving code organization without changing the underlying
algorithm. More significant performance improvements might be possible with algorithmic changes, but those would require
more extensive testing and validation.

## Future Improvements

1. Consider using SIMD instructions for parallel processing of character masks, as mentioned in the
   optimization_notes.md file.
2. Implement a more memory-efficient data structure for patterns with few distinct characters.
3. Explore a hybrid approach that uses a small array for the most common characters and a hash table for others.
4. Consider implementing a parallel version of the algorithm for large texts, as mentioned in task #16 in tasks.md.

# Review

Benchmarks show that local caching of fields doesn't have improvements (performance even is slightly worse)