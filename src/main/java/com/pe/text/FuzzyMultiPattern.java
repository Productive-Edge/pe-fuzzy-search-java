package com.pe.text;

/**
 * Interface for the multiple pattern search instance.
 *
 * It might be useful to search multiple patterns in the single scan of the input text:
 *
 * <pre>{@code
 *     private static final FuzzyMultiPattern KEYWORDS = FuzzyMultiPattern.oneOf(
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
 *
 *          KEYWORDS.matcher(text, startFrom).stream().forEach(result ->
 *              document.add(
 *                   NamedEntity.descriptor()
 *                       .setType("W56_"+result.pattern().text().hashCode())
 *                       .setScore(1)
 *                       .setBegin(result.start())
 *                       .setEnd(result.end()))
 *          );
 *      }
 * }</pre>
 */
public interface FuzzyMultiPattern extends MatcherProvider {

    /**
     * Creates instance of the {@code FuzzyMultiPattern} combining specified fuzzy patterns.
     *
     * @param first The 1st fuzzy pattern.
     * @param orSecond The 2nd fuzzy pattern.
     * @param orOthers Optional additional fuzzy patterns to combine.
     *
     * @return The instance of multiple fuzzy pattern which is able to match all provided patterns into one scan.
     */
    static FuzzyMultiPattern combine(FuzzyPattern first, FuzzyPattern orSecond, FuzzyPattern... orOthers) {
        IterativeFuzzyPattern[] patterns = new IterativeFuzzyPattern[orOthers.length + 2];
        patterns[0] = IterativeFuzzyPattern.cast(first, 1);
        patterns[1] = IterativeFuzzyPattern.cast(orSecond, 2);
        for (int i = 0; i < orOthers.length; i++) {
            final int index = i + 2;
            patterns[index] = IterativeFuzzyPattern.cast(orOthers[i], index);
        }
        return new MultiplePatterns(patterns);
    }
}
