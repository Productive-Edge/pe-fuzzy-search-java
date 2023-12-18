# pe-fuzzy-search-java

This library implements approximate string matching (fuzzy string searching)
where the building of the full-text search index is overhead (i.e. text where search happen is new each
time, indexing of the document will take more time than single or few searches with help of this library).

```java
public class Example {
    public static void main(String[] args) {
        // maximum 3 edits (differences) allowed
        FuzzyPattern.compile("Medical?", 3)
                .matcher("4. Dental? [ ! Medicai? ] I (!f both, complete 3-11 for dental oniy.i")
                .stream()
                .forEach(System.out::println);
        //Output:
        //FuzzyResult{ \
        // com.pe.text.Bitap32{pattern="Medical?", maxLevenshteinDistance=3, caseInsensitive=false}, \
        // start=15, end=23, distance=1, foundText="Medicai?", edits=[REPLACEMENT]}
    }
}
```

This implementation is based on the [Bitap](https://en.wikipedia.org/wiki/Bitap_algorithm) algorithm with following
improvements:

* no restriction on the search pattern length;
* case-insensitive matching;
* it doesn't stop on the first finding, which is the worst (i.e. has maximal allowed distance between pattern and found
  text), but tries to improve result (i.e. minimizes distance);
* matching result explanation in details: as lists of characters and their positions which were deleted, replaced,
  or inserted;
* it is possible to combine multiple patterns into the one;
* familiar pattern/matcher API similar to the `java.util.regex.Pattern` and `java.util.regex.Matcher`
  with possibility to stream matching;

## Installation

## History of creation this library

This library was implemented at [Productive Edge LLC](https://www.productiveedge.com/)
to solve the problem of the information extraction from documents with
high [OCR](https://en.wikipedia.org/wiki/Optical_character_recognition) error rate.
We use [Workfusion](https://www.workfusion.com/) AI for the information extraction. Trained models work well on
documents
with small amount of OCR errors, but in our case automation of the information extraction from the scanned documents was
not enough,
due OCR errors. Hopefully Workfusion AutoML SDK allows to add custom annotations and features to the documents,
so we can mark important keywords even they have OCR errors inside using fuzzy search.
The **important criteria** for our need in fuzzy search were:

* the **precise location of the found matching**, i.e. capture only necessary tokens;
* **the high performance** at the same time,
  due importance of spent time on the model training and execution.

We haven't found anything with these criteria so had to implement own library which we glad to open source.

Example of the custom Workfusion AutoML document annotator with fuzzy searching applied:

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
            // since it might be not the best matching in the document due OCR errors   
            Optional<FuzzyResult> medical = MEDICAL_LABEL.matcher(text, from, to).findTheBest();
            medical.ifPresent(r -> document.add(NamedEntity.descriptor().setType(r.pattern().text().toString())
                    .setBegin(r.start()).setEnd(r.end()).setScore(r.similarity())));

            // search for DENTAL_LABEL between OTHER_COVERAGE and MEDICAL_LABEL, 
            // since it might be not the best matching in the document due OCR errors   
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

This library has similar API to the `java.util.regex.Pattern` and `java.util.regex.Matcher`:

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
    // it is possible to combine multiple patterns into the one
    private static final FuzzyPatterns INGREDIENTS_TO_EXCLUDE = FuzzyPatterns.combine(
            CORN_SYRUP,
            CONCENTRATE
    );

    static boolean hasUnwantedIngredients(String ketchupIngredientsWithOcrErrors) {
        return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients).find();
    }

    static void printUnwantedIngredients(String ketchupIngredientsWithOcrErrors) {
        // optionally you can specify start and end indices for search
        // by default start is 0 and end is text.length()
        FuzzyMatcher matcher = INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients);
        while (matcher.find()) {
            System.out.println(matcher.foundText()); // FuzzyMatcher implements FuzzyResult
            // use matcher.reset - to manipulate state (change next start, end, or maximum allowed distance
        }
    }

    static Stream<FuzzyResult> streamUnwantedIngredients(String ketchupIngredientsWithOcrErrors) {
        // stream has tiny memory overhead in comparison with while loop over matcher.find()
        return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredients).stream();
    }

}
```

## Matching Results

Default Bitap algorithm returns only one index (end index) where matching found at maximum allowed [Levenshtein
distance](https://en.wikipedia.org/wiki/Levenshtein_distance)
(amount of edited characters),
and it doesn't have information about edit operations applied to this matching.
E.g. Default Bitap algorithm for the pattern `ABCD` with maximum allowed Levenshtein distance `2`
will stop in the text `XXABXD` on the char 'B' (3 - zero based index), with no other details.

Our implementation will return information about the best matching in this case:
XX**ABXD**, start zero based index = 2, end = 5, found text `ABXD`, with one replacement 'X' -> 'C' at the 4th index.

This library prefers replacements to the insertion and deletion when tries to find the best matching,
since OCR errors in the most cases are fixed by replacement wrongly recognized character.

Anyway we share applied edit operations and their indices, head and tail replacements
can be treated as inserts if it is better for your task.

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