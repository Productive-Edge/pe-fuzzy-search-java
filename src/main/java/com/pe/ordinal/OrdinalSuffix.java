package com.pe.ordinal;

/**
 * Interface for converting integer values to strings with appropriate ordinal suffixes.
 * <p>
 * This interface provides a way to convert numbers into their ordinal form by appending
 * the appropriate suffix. For example, in English:
 * <ul>
 *     <li>1 becomes "1st"</li>
 *     <li>2 becomes "2nd"</li>
 *     <li>3 becomes "3rd"</li>
 *     <li>4 becomes "4th"</li>
 *     <li>11 becomes "11th" (special case)</li>
 *     <li>21 becomes "21st" (back to regular pattern)</li>
 * </ul>
 * <p>
 * Different languages have different rules for ordinal suffixes, so this interface
 * allows for language-specific implementations. The {@link #EN} constant provides
 * an implementation for English ordinal suffixes.
 * <p>
 * This interface is used in the library for generating human-readable error messages
 * and for formatting output in certain contexts.
 *
 * @see #EN
 */
public interface OrdinalSuffix {

    /**
     * A predefined implementation of {@link OrdinalSuffix} for English language ordinal suffixes.
     * <p>
     * This implementation follows the standard English rules for ordinal suffixes:
     * <ul>
     *     <li>Numbers ending in 1 (except 11) use the suffix "st" (e.g., 1st, 21st, 101st)</li>
     *     <li>Numbers ending in 2 (except 12) use the suffix "nd" (e.g., 2nd, 22nd, 102nd)</li>
     *     <li>Numbers ending in 3 (except 13) use the suffix "rd" (e.g., 3rd, 23rd, 103rd)</li>
     *     <li>All other numbers use the suffix "th" (e.g., 4th, 11th, 12th, 13th, 20th)</li>
     * </ul>
     * <p>
     * This constant can be used directly whenever English ordinal suffixes are needed:
     * <pre>{@code
     *     String message = "This is your " + OrdinalSuffix.EN.addTo(attemptCount) + " attempt.";
     * }</pre>
     */
    OrdinalSuffix EN = new OrdinalSuffix() {
        private final String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};

        @Override
        public String addTo(final int ordinal) {
            switch (ordinal % 100) {
                case 11:
                case 12:
                case 13:
                    return ordinal + "th";
                default:
                    return ordinal + suffixes[ordinal % 10];
            }
        }
    };

    /**
     * Converts an integer value to a string with the appropriate ordinal suffix.
     * <p>
     * This method takes an integer and returns a string consisting of the integer followed by
     * the appropriate ordinal suffix according to the rules of the implementing language.
     *
     * <p><strong>Examples (using English rules):</strong>
     * <pre>{@code
     *     OrdinalSuffix.EN.addTo(1);    // Returns "1st"
     *     OrdinalSuffix.EN.addTo(2);    // Returns "2nd"
     *     OrdinalSuffix.EN.addTo(3);    // Returns "3rd"
     *     OrdinalSuffix.EN.addTo(4);    // Returns "4th"
     *     OrdinalSuffix.EN.addTo(11);   // Returns "11th" (special case)
     *     OrdinalSuffix.EN.addTo(21);   // Returns "21st"
     *     OrdinalSuffix.EN.addTo(42);   // Returns "42nd"
     *     OrdinalSuffix.EN.addTo(103);  // Returns "103rd"
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>Negative numbers: Implementation-dependent, but typically treated the same as positive numbers</li>
     *     <li>Zero: Implementation-dependent, but typically returns "0th" in English</li>
     *     <li>Very large numbers: Should work correctly as long as the number can be represented as an int</li>
     * </ul>
     *
     * @param ordinal The integer value to convert.
     * @return A string consisting of the integer value followed by the appropriate ordinal suffix.
     * @see #EN
     */
    String addTo(final int ordinal);
}
