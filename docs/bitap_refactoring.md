# Bitap Implementations Refactoring

## Overview

This document describes the refactoring of the Bitap implementations (Bitap32, Bitap64, Bitap65Plus) to reduce code
duplication. The refactoring uses a template pattern approach to share common logic while allowing for type-specific
implementations.

## Problem

The original implementation had three separate Bitap classes (Bitap32, Bitap64, Bitap65Plus) that shared significant
code but were implemented separately. This led to code duplication and made maintenance more difficult.

## Solution

A template pattern approach was used to refactor the code:

1. Created a new abstract class `BitapBase<T>` that extends `BaseBitap` and defines abstract methods for type-specific
   operations
2. Refactored the three Bitap implementations to extend `BitapBase<T>` with the appropriate type parameter:
    - `Bitap32` extends `BitapBase<Integer>`
    - `Bitap64` extends `BitapBase<Long>`
    - `Bitap65Plus` extends `BitapBase<BitVector>`
3. Implemented the abstract methods in each concrete class to handle the type-specific bit operations

## Implementation Details

### BitapBase<T>

The `BitapBase<T>` class defines the following abstract methods that must be implemented by concrete subclasses:

- `getPositionMask(char c)`: Gets the position mask for a character
- `getLastBitMask()`: Gets the last bit mask used for the stop condition
- `hasZeroAtLastBit(T value)`: Checks if the value has a zero at the last bit position
- `leftShiftAndOr(T value, T positionMask)`: Performs a left shift by 1 and OR operation
- `leftShift(T value)`: Performs a left shift by 1 operation
- `bitwiseAnd(T... values)`: Performs a bitwise AND operation on multiple values
- `createMinusOne()`: Creates a new instance with all bits set to -1

It also defines an abstract inner class `BaseMatcher` that extends `BaseBitap.Matcher` to provide a template for the
matcher implementation.

### Concrete Implementations

Each concrete implementation (Bitap32, Bitap64, Bitap65Plus) extends `BitapBase<T>` with the appropriate type parameter
and implements the abstract methods to handle the type-specific bit operations.

The `testNextSymbol()` method in each implementation's Matcher class was refactored to use the template methods instead
of direct bit operations, reducing code duplication while maintaining the same functionality.

## Benefits

1. **Reduced Code Duplication**: Common logic is now defined in a single place
2. **Improved Maintainability**: Changes to the algorithm only need to be made in one place
3. **Type Safety**: The generic type parameter ensures type safety for the bit operations
4. **Flexibility**: New Bitap implementations can be added more easily by extending `BitapBase<T>` with a new type

## Testing

All existing tests for the Bitap implementations pass with the refactored code, confirming that the functionality
remains the same.

# Review

Performance dropped (execution time increased on):

* 32b - 30%
* 64b - 100%
* 65+ - 7%

Codebase was increased 