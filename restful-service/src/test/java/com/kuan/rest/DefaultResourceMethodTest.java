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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                    String name = method.getName() + "("
                            + Arrays.stream(method.getParameters())
                                    .map(p -> p.getType().getSimpleName())
                                    .collect(Collectors.joining("."))
                            + ")";
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
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", String.class);
        parameters.put("path", List.of("path"));

        resourceMethod.call(context, builder);

        assertEquals("getPathParam(String)", lastCall.name());
        assertEquals(List.of("path"), lastCall.arguments());
    }

    @Test
    public void should_inject_int_to_path_param() {
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", int.class);
        parameters.put("path", List.of("1"));

        resourceMethod.call(context, builder);


        assertEquals("getPathParam(int)", lastCall.name());
        assertEquals(List.of(1), lastCall.arguments());
    }

    @Test
    public void should_inject_string_to_query_param() {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", String.class);
        parameters.put("query", List.of("query"));

        resourceMethod.call(context, builder);

        assertEquals("getQueryParam(String)", lastCall.name());
        assertEquals(List.of("query"), lastCall.arguments());

    }

    @Test
    public void should_inject_int_to_query_param() {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", int.class);
        parameters.put("query", List.of("1"));

        resourceMethod.call(context, builder);

        assertEquals("getQueryParam(int)", lastCall.name());
        assertEquals(List.of(1), lastCall.arguments());
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
        String getPathParam(@PathParam("path") String value);

        @GET
        String getPathParam(@PathParam("path") int value);

        @GET
        String getQueryParam(@QueryParam("query") String value);

        @GET
        String getQueryParam(@QueryParam("query") int value);

    }

}
