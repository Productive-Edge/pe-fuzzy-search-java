# pe-fuzzy-search-java

This library implements approximate string matching (fuzzy string searching)
where the building of the full-text search index is overhead (i.e. text where the search happens is new each
time, indexing of the document will take more time than a single or few searches with the help of this library).

Fuzzy string searching finds strings that closely match a given pattern. It allows for variations, errors, or similarities in the strings being compared and relies on algorithms that measure the degree of similarity between strings. This method is used for spell checking, data cleaning, and search engines where it helps retrieve relevant results even when the input query contains errors or variations.

This library is beneficial for the processing of text with errors.

```java
public class Example {
    public static void main(String[] args) {
        // maximum 3 edits (differences) allowed
        FuzzyPattern.compile("Medical?", 3)
                .matcher("4. Dental? [ ! Medicaid? ] I (!f both, complete 3-11 for dental only.i")
                .stream()
                .forEach(System.out::println);
        //Output:
        //FuzzyResult{ \
        // com.pe.text.Bitap32{pattern="Medical?", maxLevenshteinDistance=3, caseInsensitive=false}, \
        // start=15, end=23, distance=1, foundText="Medicai?", edits=[REPLACEMENT]}
    }
}
```

This implementation is based on the [Bitap](https://en.wikipedia.org/wiki/Bitap_algorithm) algorithm with the following
improvements:

* no restriction on the search pattern length;
* case-insensitive matching;
* it doesn't stop on the first finding, which is the worst (i.e., has the maximal allowed distance between the pattern and found
  text), but tries to improve the result (i.e. minimizes distance);
* matching result explanation in detail: as lists of characters and their positions which were deleted, replaced,
  or inserted;
* it is possible to combine multiple patterns into one;
* familiar pattern/matcher API similar to the `java.util.regex.Pattern` and `java.util.regex.Matcher`
  with the possibility of stream matching;
* custom faster "perfect" hashing for a fixed set of characters in a pattern;

## Installation

```xml

<dependency>
    <!-- TODO publish -->
</dependency>
```

## History of creation of this library

This library was implemented at [Productive Edge LLC](https://www.productiveedge.com/)
to solve the problem of information extraction from documents with
high [OCR](https://en.wikipedia.org/wiki/Optical_character_recognition) error rate, where keywords are too unstable for
pattern matching or machine learning.

The most **important criteria** for this problem in our case were:

* **precise location of the found matching**, i.e. capture only necessary tokens;
* **high performance**.

We haven't found anything with these criteria, so we had to implement our own library, which we are glad to open source.

Example of the custom [Workfusion](https://www.workfusion.com/) AutoML document annotator with fuzzy searching applied:

```java
public class IsDental {

    public static final String DENTAL = "dental_checkbox";

    public static final FuzzyPattern OTHER_COVERAGE = FuzzyPattern.compile("OTHER COVERAGE", 6);
    public static final FuzzyPattern NAME_OF_POLICYHOLDER = FuzzyPattern.compile("5. Name of Policyholder/Subscriber in #4 (Last, First, Middle Initial Suffix)", 30);

    public static final FuzzyPattern DENTAL_LABEL = FuzzyPattern.compile("4. Dental?", 4);
    public static final FuzzyPattern MEDICAL_LABEL = FuzzyPattern.compile("Medical?", 4);

    public static class ValueAnnotator implements Annotator<Document> {

        @Override
        public void process(Document document) {
            final String text = document.getText();
            // unique text before
            final int from = OTHER_COVERAGE.matcher(text).findTheBest().map(FuzzyResult::end).orElse(0);
            // unique text after 
            final int to = NAME_OF_POLICYHOLDER.matcher(text, from, text.length() >>> 1).findTheBest()
                    .map(FuzzyResult::start).orElse(text.length() / 2); // 1st half of document

            // search for MEDICAL_LABEL between OTHER_COVERAGE and NAME_OF_POLICYHOLDER, 
            // since it might not be the best matching in the document due to OCR errors   
            Optional<FuzzyResult> medical = MEDICAL_LABEL.matcher(text, from, to).findTheBest();
            medical.ifPresent(r -> document.add(NamedEntity.descriptor().setType(r.pattern().text().toString())
                    .setBegin(r.start()).setEnd(r.end()).setScore(r.similarity())));

            // search for DENTAL_LABEL between OTHER_COVERAGE and MEDICAL_LABEL, 
            // since it might not be the best matching in the document due to OCR errors   
            Optional<FuzzyResult> dental = DENTAL_LABEL.matcher(text, from, medical.map(FuzzyResult::start).orElse(to)).findTheBest();
            dental.ifPresent(r -> document.add(NamedEntity.descriptor().setType(r.pattern().text().toString())
                    .setBegin(r.start()).setEnd(r.end()).setScore(r.similarity())));

            // annotate checkbox value if DENTAL_LABEL and MEDICAL_LABEL where found
            dental.ifPresent(before -> medical.ifPresent(after -> {
                final int d = after.start() - before.end();
                if (d > 0 && d < 20) {
                    document.add(NamedEntity.descriptor().setType(DENTAL)
                            .setBegin(before.end()).setEnd(after.start())
                            .setScore(before.similarity() * after.similarity()));
                }
            }));
        }
    }
    //...
}
```

## Pattern / Matcher API

This library has a similar API to the `java.util.regex.Pattern` and `java.util.regex.Matcher`:

```java
import com.pe.text.FuzzyMatcher;
import com.pe.text.FuzzyPatterns;
import com.pe.text.FuzzyResult;

import java.util.stream.Stream;

class Example {
    // case-sensitive pattern with maximum allowed 3 character edits (insertion / deletion / replacement)
    private static final FuzzyPattern CORN_SYRUP = FuzzyPattern.compile("Corn Syrup", 3);
    // case-insensitive pattern with maximum allowed 5 character edits
    private static final FuzzyPattern CONCENTRATE = FuzzyPattern.compile("Tomato Concentrate", 5, true);
    // it is possible to combine multiple patterns into one
    private static final FuzzyPatterns INGREDIENTS_TO_EXCLUDE = FuzzyPatterns.combine(
            CORN_SYRUP,
            CONCENTRATE
    );

    static boolean hasUnwantedIngredients(String ketchupIngredientsWithOcrErrors) {
        return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients).find();
    }

    static void printUnwantedIngredients(String ketchupIngredientsWithOcrErrors) {
        // optionally you can specify start and end indices for the search
        // by default start is 0 and end is text.length()
        FuzzyMatcher matcher = INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients);
        while (matcher.find()) {
            System.out.println(matcher.foundText()); // FuzzyMatcher implements FuzzyResult
            // use matcher.reset - to manipulate state (change next start, end, or maximum allowed distance
        }
    }

    static Stream<FuzzyResult> streamUnwantedIngredients(String ketchupIngredientsWithOcrErrors) {
        // stream has tiny memory overhead compared to while loop over matcher.find()
        return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients).stream();
    }

}
```

## Matching Results

Default Bitap algorithm returns only one index (end index) where matching is found at the maximum allowed [Levenshtein
distance](https://en.wikipedia.org/wiki/Levenshtein_distance)
(amount of edited characters),
and it doesn't have information about edit operations applied to this matching.
E.g., Default Bitap algorithm for the pattern `ABCD` with a maximum allowed Levenshtein distance `2`
will stop in the text `XXABXD` on the char 'B' (3 - zero-based index `XXA>B<XD`),
without details about the difference between pattern and matched text.

This implementation will return information about the best matching in this case:
XX**ABXD**, start zero-based index is 2, end one is 5, found text `ABXD`, with one replacement 'X' -> 'C' at the 4th
index.

This library prefers replacements to the insertion and deletion when trying to find the best matching,
since OCR errors, in most cases, are fixed by replacing wrongly recognized characters.

## Credits

Lead Developer - Henadz
Yermakavets [@gyermakavets](https://github.com/gyermakavets) / [@genaby](https://github.com/genaby)

## License

The MIT License (MIT)

Copyright (c) 2022 [Productive Edge LLC](https://www.productiveedge.com/)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
