package com.pe.ordinal;

/**
 * Interface defines conversion of integer value to the string with the corresponding suffix.
 * E.g. Integer with value 2 should be converted into string "2nd" in the English.
 */
public interface OrdinalSuffix {

    /**
     * English ordinal suffixes implementation
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
     * Converts specified integer value to the string which consists of this value and corresponding ordinal suffix.
     * E.g. {@code 2} will be converted into {@code "2nd"} in the English.
     *
     * @param ordinal integer value to convert into string with ordinal suffix.
     * @return string which consists of this value and corresponding ordinal suffix.
     */
    String addTo(final int ordinal);
}
