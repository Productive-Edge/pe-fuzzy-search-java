package com.pe.text;

import com.pe.ordinal.Ordinal;

import java.util.Arrays;

/**
 * Fuzzy Pattern created as combination of the other multiple fuzzy patterns,
 * it returns non overlapping matchings in order they are in text.
 *
 * <p>
 * This combined pattern is faster in case only one or few first matchings needed:
 *
 * <pre>{@code
 *   private static final FuzzyMultiPattern INGREDIENTS_TO_EXCLUDE = FuzzyMultiPattern.combine(
 *      FuzzyPattern.pattern("Corn Syrup", 3, true), //maximum 3 OCR errors, case insensitive
 *      FuzzyPattern.pattern("Tomato Concentrate", 4, true) //maximum 4 OCR errors, case insensitive
 *   );
 *
 *   public static boolean hasUnwantedIngredientIn(String ketchupIngredientsWithOcrErrors) {
 *       return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients).find();
 *   }
 * }</pre>
 * <p>
 * <p>
 * For case where all matches have to be found and order is not important,
 * it is slightly faster iterate through all findings for each pattern one-by-one
 * rather than {@link FuzzyMultiPattern#combine(FuzzyPattern, FuzzyPattern, FuzzyPattern...)}
 * <pre>{@code
 *
 *     private static final FuzzyPattern[] KEYWORDS = {
 *         FuzzyPattern.pattern("56a. Provider", 3),
 *         FuzzyPattern.pattern("Speciality Code", 4),
 *         FuzzyPattern.pattern("57. Phone", 3),
 *         FuzzyPattern.pattern("52. Phone", 3),
 *         FuzzyPattern.pattern("49. NPI", 2),
 *         FuzzyPattern.pattern("50. License Number", 5),
 *         FuzzyPattern.pattern("57. License Number", 5),
 *         FuzzyPattern.pattern("51. SSN or TIN", 3),
 *         //inside address
 *         FuzzyPattern.pattern("Street", 2, true),
 *         FuzzyPattern.pattern("Suite", 2, true),
 *         FuzzyPattern.pattern("Floor", 2, true),
 *         FuzzyPattern.pattern("Drive", 2, true)
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
public interface FuzzyMultiPattern extends MatcherProvider {

    /**
     * Creates instance of the {@code FuzzyMultiPattern} combining specified fuzzy patterns.
     *
     * @param first  The 1st fuzzy pattern.
     * @param second The 2nd fuzzy pattern.
     * @param others Optional additional fuzzy patterns to combine.
     * @return The instance of multiple fuzzy pattern which is able to match all provided patterns into one scan.
     */
    static FuzzyMultiPattern combine(FuzzyPattern first, FuzzyPattern second, FuzzyPattern... others) {
        FuzzyPattern[] patterns = new FuzzyPattern[others.length + 2];
        patterns[0] = first;
        patterns[1] = second;
        for (int i = 0; i < others.length; i++) patterns[i + 2] = others[i];
        boolean isIterative = true;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] == null) throw new IllegalArgumentException(Ordinal.en(i + 1) + " pattern is null");
            isIterative = isIterative && patterns[i] instanceof IterativeFuzzyPattern;
        }
        if (isIterative) {
            return new IterativeMultiplePatterns(Arrays.copyOf(patterns, patterns.length, IterativeFuzzyPattern[].class));
        }
        return new MultiplePatterns(patterns);
    }
}
