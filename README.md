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
* it has RegEx similar pattern/matcher API with possibility to stream matching;

## Installation

## Pattern / Matcher API

## History of creation this library

This library was implemented at [Productive Edge LLC](https://www.productiveedge.com/)
to solve a problem of the information extraction from documents with
high [OCR](https://en.wikipedia.org/wiki/Optical_character_recognition) error rate.
The [Workfusion](https://www.workfusion.com/) ML models work well on documents with small amount of OCR errors,
but in our case automation of the information extraction from the scanned documents was not enough,
due OCR errors. Hopefully Workfusion AutoML SDK allows to add custom annotations and features to the documents,
so we can mark important keywords even they have OCR errors inside using fuzzy search.
The **important criteria** for this fuzzy search where - the **precise location of the found matching**,
which doesn't capture unrelated tokens, and **the high performance** at the same time,
due time spent on the training and extraction is very important,
more over Workfusion AutoML SDK might exclude slow custom components on the training stage.

We haven't found anything with these criteria so had to implement own library which we glad to open source.

Example of the custom Workfusion AutoML document annotator with fuzzy searching applied:

```java
public class IsMedical {

    public static class ValueAnnotator implements Annotator<Document> {

        public static final FuzzyPattern MEDICAL = FuzzyPattern.pattern(
                " Medical? ", 3);
        public static final FuzzyPattern IF_BOTH = FuzzyPattern.pattern(
                " (if both, complete 5-11 for dental only.)", 10
        );

        @Override
        public void process(Document document) {
            final String text = document.getText();
            // keywords before the value we would like to extract
            Optional<FuzzyResult> medical = MEDICAL.matcher(text, 0, text.length() / 4).findTheBest();
            medical.ifPresent(r -> document.add(NamedEntity.descriptor().setType(r.pattern().text().toString())
                    .setBegin(r.start()).setEnd(r.end()).setScore(r.similarity())));

            // keywords after the value
            Optional<FuzzyResult> ifBoth = IF_BOTH.matcher(text, 0, text.length() / 4).findTheBest();
            ifBoth.ifPresent(r -> document.add(NamedEntity.descriptor().setType("if_both")
                    .setBegin(r.start()).setEnd(r.end()).setScore(r.similarity())));

            // add value annotation if we know it start and end position in the document text 
            medical.ifPresent(before -> ifBoth.ifPresent(after -> {
                final int d = after.start() - before.end();
                if (d > 0 && d < 10) {
                    document.add(NamedEntity.descriptor().setType("MEDICAL_V")
                            .setBegin(before.end()).setEnd(after.start())
                            .setScore(before.similarity() * after.similarity()));
                }
            }));

        }
    }
    // ...
}
```

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