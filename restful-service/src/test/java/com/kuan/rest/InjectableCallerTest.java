package com.kuan.rest;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * author: qxkk
 * date: 2022/11/9
 */
public abstract class InjectableCallerTest {
    protected ResourceContext context;
    protected UriInfoBuilder builder;
    protected UriInfo uriInfo;
    protected MultivaluedHashMap<String, String> parameters;
    protected LastCall lastCall;
    protected SameServiceInContext service;
    protected Object resource;

    @BeforeEach
    public void before() {
        lastCall = null;
        resource = initResource();

        context = mock(ResourceContext.class);
        builder = mock(UriInfoBuilder.class);
        uriInfo = mock(UriInfo.class);
        service = mock(SameServiceInContext.class);
        parameters = new MultivaluedHashMap<>();

        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(builder.createUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(parameters);
        when(uriInfo.getQueryParameters()).thenReturn(parameters);
        when(context.getResource(eq(SameServiceInContext.class))).thenReturn(service);
    }

    protected abstract Object initResource();

    protected static String getMethodName(String methodName, List<? extends Class<?>> parameterTypes) {
        return methodName + "("
                + parameterTypes.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining("."))
                + ")";
    }

    record LastCall(String name, List<Object> arguments) {

    }
}
