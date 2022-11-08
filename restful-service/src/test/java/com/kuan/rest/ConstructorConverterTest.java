package com.kuan.rest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author: qxkk
 * @Date: 2022/11/8
 */
public class ConstructorConverterTest {

    @Test
    public void should_convert_via_converter_constructor() {
        assertEquals(Optional.of(new BigDecimal("12345")), ConstructorConverter.convert(BigDecimal.class, "12345"));
    }

}