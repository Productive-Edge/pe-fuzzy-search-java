package com.pe.text;

import com.pe.ordinal.Ordinal;

import java.util.Arrays;

/**
 * Interface for the multiple pattern search instance.
 * <p>
 * It might be useful to search multiple patterns in the single scan of the input text to get one or few first matchings:
 *
 * <pre>{@code
 *   private static final FuzzyMultiPattern INGREDIENTS_TO_EXCLUDE = FuzzyMultiPattern.combine(
 *      FuzzyPattern.pattern("Corn Syrup", 3, true),
 *      FuzzyPattern.pattern("Tomato Concentrate", 4, true)
 *   );
 *
 *   public static boolean hasUnwantedIngredients(String ketchupIngredients) {
 *       return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients).find();
 *   }
 * }</pre>
 * <p>
 * <b>NOTE:</b> It is better to not use FuzzyMultiPattern to find all matchings on big documents in case performance is important,
 * because it can be significantly slower due to CPU cache misses in comparison with full scan per each FuzzyMatcher.
 * E.g. following code will be slower
 * <pre>{@code
 *
 *     private static final FuzzyMultiPattern KEYWORDS = FuzzyMultiPattern.combine(
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
 *      );
 *
 *      public void process(Document document) {
 *          final String text = document.getText();
 *          final int startFrom = (text.length() * 3) / 4;
 *          // It is better to not use FuzzyMultiPattern
 *          // to mark all matchings where they order is not important,
 *          // but performance factor is critical
 *          KEYWORDS.matcher(text, startFrom).stream().forEach(result ->
 *              document.add(
 *                   NamedEntity.descriptor()
 *                       .setType("W56_"+result.pattern().text())
 *                       .setScore(1)
 *                       .setBegin(result.start())
 *                       .setEnd(result.end()))
 *          );
 *      }
 * }</pre>
 *
 * <b>Faster</b> approach, which do the same:
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
