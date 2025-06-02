# Character Position Mask Generation Optimization

## Overview

This document describes the optimizations made to the character position mask generation in the Bitap32 and Bitap64
classes.

## Problem

The original implementation used a hash table (Char2IntMap/Char2LongMap) for storing and retrieving character position
masks. This approach has several inefficiencies:

1. Hash table lookups have overhead compared to direct array indexing
2. For ASCII-only patterns (which are common), a simpler data structure could be more efficient
3. The case-insensitive handling duplicated work for uppercase and lowercase characters

## Solution

The optimization introduces a direct array lookup for ASCII characters, which avoids the overhead of hash table lookups
for common cases:

1. Added a direct array (asciiPositionMasks) for ASCII characters (0-127)
2. Added a flag (useAsciiOptimization) to determine whether the ASCII optimization can be used
3. Modified the character position mask generation to use the direct array for ASCII-only patterns
4. Updated the testNextSymbol and related methods to use the optimized lookup when possible

## Implementation Details

### ASCII Optimization Check

The implementation first checks if all characters in the pattern are ASCII:

1. Iterate through each character in the pattern
2. Check if the character code is less than 128 (ASCII range)
3. For case-insensitive matching, also check the uppercase and lowercase versions
4. If any character is outside the ASCII range, disable the ASCII optimization

### Optimized Lookup

When looking up a character's position mask:

1. Check if ASCII optimization is enabled and the character is in ASCII range
2. If so, use direct array indexing: `asciiPositionMasks[currentChar]`
3. Otherwise, fall back to the hash table: `positionMasks.get(currentChar)`

## Expected Performance Improvements

1. **Faster Lookups**: Direct array indexing is much faster than hash table lookups, especially for frequently accessed
   elements
2. **Reduced Memory Overhead**: The ASCII array has a fixed size (128 elements) and doesn't require hash table
   structures
3. **Better Cache Locality**: The ASCII array is contiguous in memory, which improves cache performance

## Limitations

1. The optimization only applies to ASCII-only patterns
2. For non-ASCII patterns, the implementation falls back to the original hash table approach
3. The ASCII array still requires 128 elements, which might be wasteful for patterns with few distinct characters

## Future Improvements

1. Consider a hybrid approach that uses a small array for the most common characters and a hash table for others
2. Implement a more memory-efficient data structure for patterns with few distinct characters
3. Explore SIMD instructions for parallel processing of character masks

# Review

This optimization is better to implement as FixedCharTable interface, instead of changing Bitap32 & Bitap64 (BTW
Bitap65Plus was not updated).