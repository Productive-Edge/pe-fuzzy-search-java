package com.pe.text;

import com.pe.ordinal.OrdinalSuffix;

/**
 * Fuzzy Pattern created as combination of the other multiple fuzzy patterns,
 * it returns non overlapping matchings in order they are in text.
 *
 * <p>
 * This combined pattern is faster in case only one or few first matchings needed:
 *
 * <pre>{@code
 *   private static final FuzzyPatterns INGREDIENTS_TO_EXCLUDE = FuzzyPatterns.of(
 *      FuzzyPattern.compile("Corn Syrup", 3, true), //maximum 3 OCR errors, case insensitive
 *      FuzzyPattern.compile("Tomato Concentrate", 4, true) //maximum 4 OCR errors, case insensitive
 *   );
 *
 *   public static boolean hasUnwantedIngredientIn(String ketchupIngredientsWithOcrErrors) {
 *       return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients).find();
 *   }
 * }</pre>
 * <p>
 * <p>
 * For case where all matches have to be found and order is not important,
 * it is slightly faster to iterate through all findings for each pattern one-by-one
 * rather than {@link FuzzyPatterns#combine(FuzzyMatcherProvider, FuzzyMatcherProvider, FuzzyMatcherProvider...)}
 * <pre>{@code
 *
 *     private static final FuzzyPattern[] KEYWORDS = {
 *         FuzzyPattern.compile("56a. Provider", 3),
 *         FuzzyPattern.compile("Speciality Code", 4),
 *         FuzzyPattern.compile("57. Phone", 3),
 *         FuzzyPattern.compile("52. Phone", 3),
 *         FuzzyPattern.compile("49. NPI", 2),
 *         FuzzyPattern.compile("50. License Number", 5),
 *         FuzzyPattern.compile("57. License Number", 5),
 *         FuzzyPattern.compile("51. SSN or TIN", 3),
 *         //inside address
 *         FuzzyPattern.compile("Street", 2, true),
 *         FuzzyPattern.compile("Suite", 2, true),
 *         FuzzyPattern.compile("Floor", 2, true),
 *         FuzzyPattern.compile("Drive", 2, true)
 *      };
 *
 *      public void process(Document document) {
 *          final String text = document.getText();
 *          final int startFrom = (text.length() * 3) / 4;
 *          // here order of found matches is not important, so we can simply iterate over each FuzzyPattern,
 *          // to have slightly better performance
 *          Arrays.stream(KEYWORDS)
 *              .flatMap(pattern -> pattern.matcher(text, startFrom).stream())
 *              .forEach(result ->
 *                  document.add(
 *                      NamedEntity.descriptor()
 *                          .setType("W56_"+result.pattern().text())
 *                          .setScore(1)
 *                          .setBegin(result.start())
 *                          .setEnd(result.end()))
 *              );
 *      }
 * }</pre>
 */
public interface FuzzyPatterns extends FuzzyMatcherProvider {

    /**
     * Creates instance of the {@code FuzzyPatterns} combining specified fuzzy patterns.
     *
     * @param first  The 1st fuzzy pattern.
     * @param second The 2nd fuzzy pattern.
     * @param others Optional additional fuzzy patterns to combine.
     * @return The instance of multiple fuzzy pattern which is able to match all provided patterns into one scan.
     * @throws NullPointerException in case any of arguments is null
     */
    static FuzzyPatterns combine(FuzzyMatcherProvider first, FuzzyMatcherProvider second, FuzzyMatcherProvider... others) {
        if (first == null)
            throw new NullPointerException("1st pattern is null");
        if (second == null)
            throw new NullPointerException("2nd pattern is null");
        for (int i = 0; i < others.length; i++)
            if (others[i] == null)
                throw new NullPointerException(OrdinalSuffix.EN.addTo(i + 1) + " pattern is null");

        return first.combineWith(second, others);
    }

    /**
     * Returns iterable over combined fuzzy patterns on this instance
     *
     * @return iterable over combined fuzzy patterns on this instance
     */
    Iterable<? extends FuzzyMatcherProvider> patterns();
}
