package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Author: qxkk
 * @Date: 2022/11/9
 */
public class SubResourceLocatorTest {

    private UriTemplate.MatchResult result;
    private ResourceContext context;
    private UriInfoBuilder builder;
    private UriInfo uriInfo;
    private LastCall lastCall;
    private MultivaluedMap<String, String> parameters;

    record LastCall(String name, List<Object> arguments) {
    }

    private Object resource;

    @BeforeEach
    public void before() {
        lastCall = null;
        resource = initResource();


        context = mock(ResourceContext.class);
        builder = mock(UriInfoBuilder.class);
        uriInfo = mock(UriInfo.class);
        result = mock(UriTemplate.MatchResult.class);
        parameters = new MultivaluedHashMap<>();

        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(builder.createUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(parameters);
    }

    private Object initResource() {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{SubResourceMethods.class},
                (proxy, method, args) -> {
                    String name = getMethodName(method.getName(),
                            Arrays.stream(method.getParameters()).map(Parameter::getType).toList());
                    lastCall = new LastCall(name, args != null ? List.of(args) : List.of());
                    return new Message();
                });
    }


    @Test
    public void should_inject_string_path_param_to_sub_resource_method() throws NoSuchMethodException {
        String method = "getPathParam";
        Class<String> type = String.class;
        String paramString = "path";
        Object paramValue = "path";
        parameters.put("param", List.of(paramString));

        callInjectable(method, type);

        assertEquals(getMethodName(method, List.of(type)), lastCall.name());
        assertEquals(List.of(paramValue), lastCall.arguments());
    }

    private void callInjectable(String method, Class<?> type) throws NoSuchMethodException {
        SubResourceLocators.SubResourceLocator locator =
                new SubResourceLocators.SubResourceLocator(SubResourceMethods.class.getMethod(method, type));
        locator.match(result, "GET", new String[0], context, builder);
    }


    private static String getMethodName(String methodName, List<? extends Class<?>> classStream) {
        return methodName + "("
                + classStream.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining("."))
                + ")";
    }

    interface SubResourceMethods {
        @Path("/message")
        Message getPathParam(@PathParam("param") String path);
    }

    static class Message {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }
    }

}
