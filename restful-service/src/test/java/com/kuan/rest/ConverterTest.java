package com.kuan.rest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author: qxkk
 * @Date: 2022/11/8
 */
public class ConverterTest {

    @Test
    public void should_convert_via_converter_constructor() {
        assertEquals(Optional.of(new BigDecimal("12345")), ConstructorConverter.convert(BigDecimal.class, "12345"));
    }

    @Test
    public void should_not_convert_if_no_converter_constructor() {
        assertEquals(Optional.empty(), ConstructorConverter.convert(NoConverter.class, "12345"));
    }

    @Test
    public void should_convert_via_converter_factory() {
        assertEquals(Optional.of(Converter.Factory), FactoryConverter.convert(Converter.class, "Factory"));
    }


    @Test
    public void should_not_convert_if_no_converter_factory() {
        assertEquals(Optional.empty(), FactoryConverter.convert(NoConverter.class, "12345"));
    }
}

class NoConverter {

    NoConverter valueOf(String value) {
        return new NoConverter();
    }

}