package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Author: qxkk
 * @Date: 2022/11/2
 */
public class DefaultResourceMethodTest {

    @Test
    public void should_call_resource_method() throws NoSuchMethodException {
        CallableResourceMethods resource = mock(CallableResourceMethods.class);
        ResourceContext context = mock(ResourceContext.class);
        UriInfoBuilder builder = mock(UriInfoBuilder.class);
        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(resource.get()).thenReturn("resource called");

        DefaultResourceMethod resourceMethod =
                new DefaultResourceMethod(CallableResourceMethods.class.getMethod("get"));
        assertEquals(new GenericEntity("resource called", String.class), resourceMethod.call(context, builder));
    }

    interface CallableResourceMethods {
        @GET
        String get();
    }

}
