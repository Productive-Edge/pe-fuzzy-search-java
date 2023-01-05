package by.gena.ordinal;

public class Ordinal {
    private static final String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

    public static String en(final int ordinal) {
        switch (ordinal % 100) {
            case 11:
            case 12:
            case 13:
                return ordinal + "th";
            default:
                return ordinal + suffixes[ordinal % 10];
        }
    }
}
