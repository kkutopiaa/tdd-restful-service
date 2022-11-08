package com.kuan.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @Author: qxkk
 * @Date: 2022/11/8
 */
public class ConstructorConverterTest {

    @Test
    public void should_not_identity_as_constructor_converter() {
        assertFalse(ConstructorConverter.hasConverter(NoConverter.class));
    }


}

class NoConverter {

}