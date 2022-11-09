package com.kuan.rest;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @TestFactory
    public List<DynamicTest> inject_convertible_types() {
        List<DynamicTest> tests = new ArrayList<>();

        List<String> paramTypes = List.of("getPathParam", "getQueryParam");
        List<InjectableTypeTestCase> typeCases = List.of(
                new InjectableTypeTestCase(String.class, "string", "string"),
                new InjectableTypeTestCase(double.class, "3.14", 3.14),
                new InjectableTypeTestCase(float.class, "1.1", (float) 1.1),
                new InjectableTypeTestCase(long.class, "2", (long) 2),
                new InjectableTypeTestCase(int.class, "1", 1),
                new InjectableTypeTestCase(short.class, "128", (short) 128),
                new InjectableTypeTestCase(byte.class, "127", (byte) 127),
                new InjectableTypeTestCase(boolean.class, "TRUE", true),
                new InjectableTypeTestCase(BigDecimal.class, "12345", new BigDecimal("12345")),
                new InjectableTypeTestCase(Converter.class, "Factory", Converter.Factory)
        );

        for (String type : paramTypes) {
            for (InjectableTypeTestCase testCase : typeCases) {
                String displayName = "should inject " + testCase.type().getSimpleName() + " to " + type;
                Executable executable =
                        () -> verifyResourceMethodCalled(type, testCase.type(), testCase.string(), testCase.value());
                tests.add(DynamicTest.dynamicTest(displayName, executable));
            }
        }

        return tests;
    }

    @TestFactory
    public List<DynamicTest> inject_context_object() {
        List<DynamicTest> tests = new ArrayList<>();
        List<InjectableTypeTestCase> typeCases = List.of(
                new InjectableTypeTestCase(SameServiceInContext.class, "N/A", service),
                new InjectableTypeTestCase(ResourceContext.class, "N/A", context),
                new InjectableTypeTestCase(UriInfo.class, "N/A", uriInfo)
        );

        for (InjectableTypeTestCase typeCase : typeCases) {
            String displayName = "should inject " + typeCase.type().getSimpleName() + " to getContext";
            Executable executable =
                    () -> verifyResourceMethodCalled("getContext", typeCase.type(), typeCase.string(), typeCase.value());
            tests.add(DynamicTest.dynamicTest(displayName, executable));
        }

        return tests;
    }

    private void verifyResourceMethodCalled(String method, Class<?> type, String paramString, Object paramValue) {
        parameters.put("param", List.of(paramString));

        callInjectable(method, type);

        assertEquals(getMethodName(method, List.of(type)), lastCall.name());
        assertEquals(List.of(paramValue), lastCall.arguments());
    }

    protected abstract void callInjectable(String method, Class<?> type);

    record LastCall(String name, List<Object> arguments) {

    }

    record InjectableTypeTestCase(Class<?> type, String string, Object value) {

    }
}
