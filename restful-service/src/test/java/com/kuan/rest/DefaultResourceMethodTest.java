package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
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

    private UriInfo uriInfo;
    private MultivaluedHashMap<String, String> parameters;


    private LastCall lastCall;

    record LastCall(String name, List<Object> arguments) {

    }

    private SameServiceInContext service;


    @BeforeEach
    public void before() {
        lastCall = null;
        resource = (CallableResourceMethods) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{CallableResourceMethods.class},
                (proxy, method, args) -> {
                    String name = getMethodName(method.getName(),
                            Arrays.stream(method.getParameters()).map(Parameter::getType).toList());
                    lastCall = new LastCall(name, args != null ? List.of(args) : List.of());
                    return "getList".equals(method.getName()) ? new ArrayList<String>() : null;
                });

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

    private static String getMethodName(String methodName, List<? extends Class<?>> parameterTypes) {
        return methodName + "("
                + parameterTypes.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining("."))
                + ")";
    }

    @Test
    public void should_call_resource_method() {
        DefaultResourceMethod resourceMethod = getResourceMethod("get");
        resourceMethod.call(context, builder);

        assertEquals("get()", lastCall.name);
    }

    @Test
    public void should_call_resource_method_with_void_return_type() {
        DefaultResourceMethod resourceMethod = getResourceMethod("post");

        assertNull(resourceMethod.call(context, builder));
    }

    @Test
    public void should_use_resource_method_generic_return_type() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getList");

        assertEquals(
                new GenericEntity<>(List.of(), CallableResourceMethods.class.getMethod("getList").getGenericReturnType()),
                resourceMethod.call(context, builder));
    }


    record InjectableTypeTestCase(Class<?> type, String string, Object value) {

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
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);
        parameters.put("param", List.of(paramString));

        resourceMethod.call(context, builder);

        assertEquals(getMethodName(method, List.of(type)), lastCall.name());
        assertEquals(List.of(paramValue), lastCall.arguments());
    }

    private DefaultResourceMethod getResourceMethod(String methodName, Class<?>... types) {
        try {
            return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName, types));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    interface CallableResourceMethods {
        @POST
        String post();

        @GET
        String get();

        @GET
        List<String> getList();

        @GET
        String getPathParam(@PathParam("param") String value);

        @GET
        String getPathParam(@PathParam("param") int value);

        @GET
        String getPathParam(@PathParam("param") double value);

        @GET
        String getPathParam(@PathParam("param") float value);

        @GET
        String getPathParam(@PathParam("param") long value);

        @GET
        String getPathParam(@PathParam("param") short value);

        @GET
        String getPathParam(@PathParam("param") byte value);

        @GET
        String getPathParam(@PathParam("param") boolean value);

        @GET
        String getPathParam(@PathParam("param") BigDecimal value);

        @GET
        String getPathParam(@PathParam("param") Converter value);


        @GET
        String getQueryParam(@QueryParam("param") String value);

        @GET
        String getQueryParam(@QueryParam("param") int value);

        @GET
        String getQueryParam(@QueryParam("param") double value);

        @GET
        String getQueryParam(@QueryParam("param") float value);

        @GET
        String getQueryParam(@QueryParam("param") short value);

        @GET
        String getQueryParam(@QueryParam("param") long value);

        @GET
        String getQueryParam(@QueryParam("param") byte value);

        @GET
        String getQueryParam(@QueryParam("param") boolean value);

        @GET
        String getQueryParam(@QueryParam("param") BigDecimal value);

        @GET
        String getQueryParam(@QueryParam("param") Converter value);

        @GET
        String getContext(@Context SameServiceInContext service);

        @GET
        String getContext(@Context ResourceContext service);

        @GET
        String getContext(@Context UriInfo service);

    }

}

enum Converter {
    Primitive, Constructor, Factory;
}

interface SameServiceInContext {

}