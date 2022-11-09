package com.kuan.rest;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;
import java.util.stream.Collectors;

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
