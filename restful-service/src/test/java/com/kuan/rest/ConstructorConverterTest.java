package com.kuan.rest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author: qxkk
 * @Date: 2022/11/8
 */
public class ConstructorConverterTest {

    @Test
    public void should_not_identity_as_constructor_converter() {
        assertFalse(ConstructorConverter.hasConverter(NoConverter.class));
    }

    @Test
    public void should_identity_as_constructor_converter() {
        assertTrue(ConstructorConverter.hasConverter(BigDecimal.class));
    }

    @Test
    public void should_convert_via_converter_constructor() {
        assertEquals(new BigDecimal("12345"), ConstructorConverter.convert(BigDecimal.class, "12345"));
    }

}

class NoConverter {

}