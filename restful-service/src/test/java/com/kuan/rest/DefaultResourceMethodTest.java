package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Author: qxkk
 * @Date: 2022/11/2
 */
public class DefaultResourceMethodTest {

    private CallableResourceMethods resource;
    private ResourceContext context;
    private UriInfoBuilder builder;

    @BeforeEach
    public void before() {
        resource = mock(CallableResourceMethods.class);
        context = mock(ResourceContext.class);
        builder = mock(UriInfoBuilder.class);
        when(builder.getLastMatchedResource()).thenReturn(resource);
    }

    @Test
    public void should_call_resource_method() {
        when(resource.get()).thenReturn("resource called");

        DefaultResourceMethod resourceMethod = getResourceMethod("get");

        assertEquals(new GenericEntity("resource called", String.class), resourceMethod.call(context, builder));
    }


    @Test
    public void should_use_resource_method_generic_return_type() throws NoSuchMethodException {
        when(resource.getList()).thenReturn(List.of());

        DefaultResourceMethod resourceMethod = getResourceMethod("getList");

        assertEquals(
                new GenericEntity<>(List.of(), CallableResourceMethods.class.getMethod("getList").getGenericReturnType()),
                resourceMethod.call(context, builder));
    }


    private DefaultResourceMethod getResourceMethod(String methodName) {
        try {
            return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    interface CallableResourceMethods {
        @GET
        String get();

        @GET
        List<String> getList();
    }

}
