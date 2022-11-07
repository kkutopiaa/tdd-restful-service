package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

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
        parameters = new MultivaluedHashMap<>();

        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(builder.createUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(parameters);
        when(uriInfo.getQueryParameters()).thenReturn(parameters);
    }

    private static String getMethodName(String methodName, List<? extends Class<?>> classStream) {
        return methodName + "("
                + classStream.stream()
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

    @Test
    public void should_inject_string_to_path_param() {
        String method = "getPathParam";
        Class<String> type = String.class;
        String paramString = "path";
        String paramValue = "path";

        verifyResourceMethodCalled(method, type, paramString, paramValue);
    }

    @Test
    public void should_inject_int_to_path_param() {
        String method = "getPathParam";
        Class<Integer> type = int.class;
        String paramString = "1";
        int paramValue = 1;

        verifyResourceMethodCalled(method, type, paramString, paramValue);
    }

    private void verifyResourceMethodCalled(String method, Class<?> type, String paramString, Object paramValue) {
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);
        parameters.put("param", List.of(paramString));

        resourceMethod.call(context, builder);

        assertEquals(getMethodName(method, List.of(type)), lastCall.name());
        assertEquals(List.of(paramValue), lastCall.arguments());
    }

    @Test
    public void should_inject_string_to_query_param() {
        String method = "getQueryParam";
        Class<String> type = String.class;
        String paramString = "query";
        String paramValue = "query";

        verifyResourceMethodCalled(method, type, paramString, paramValue);
    }

    @Test
    public void should_inject_int_to_query_param() {
        String method = "getQueryParam";
        Class<Integer> type = int.class;
        String paramString = "1";
        int paramValue = 1;

        verifyResourceMethodCalled(method, type, paramString, paramValue);
    }


    private DefaultResourceMethod getResourceMethod(String methodName, Class... types) {
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
        String getQueryParam(@QueryParam("param") String value);

        @GET
        String getQueryParam(@QueryParam("param") int value);

    }

}
