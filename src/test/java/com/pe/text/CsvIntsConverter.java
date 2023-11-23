package com.pe.text;

import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;

import java.util.regex.Pattern;

public class CsvIntsConverter extends SimpleArgumentConverter {

    private static final Pattern NOT_DIGITS = Pattern.compile("[^\\d-]+");

    @Override
    protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
        if (source instanceof String && int[].class.isAssignableFrom(targetType)) {
            return NOT_DIGITS.splitAsStream((String) source).mapToInt(Integer::parseInt).toArray();
        }
        throw new IllegalArgumentException("Conversion from " + source.getClass() + " to "
                + targetType + " not supported.");
    }
}
