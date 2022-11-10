package com.kuan.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @Author: qxkk
 * @Date: 2022/11/9
 */
public class SubResourceLocatorTest extends InjectableCallerTest {

    private UriTemplate.MatchResult result;

    @BeforeEach
    public void before() {
        super.before();
        result = mock(UriTemplate.MatchResult.class);
    }

    @Override
    protected Object initResource() {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{SubResourceMethods.class},
                (proxy, method, args) -> {
                    String name = getMethodName(method.getName(),
                            Arrays.stream(method.getParameters()).map(Parameter::getType).toList());
                    lastCall = new LastCall(name, args != null ? List.of(args) : List.of());

                    if (method.getName().equals("throwWebApplicationException")) {
                        throw new WebApplicationException(300);
                    }

                    return new Message();
                });
    }


    @Test
    public void should_inject_string_path_param_to_sub_resource_method() {
        String method = "getPathParam";
        Class<String> type = String.class;
        String paramString = "path";
        Object paramValue = "path";
        parameters.put("param", List.of(paramString));

        callInjectable(method, type);

        assertEquals(getMethodName(method, List.of(type)), lastCall.name());
        assertEquals(List.of(paramValue), lastCall.arguments());
    }


    @Test
    public void should_get_wrap_around_web_application_exception() {
        parameters.put("param", List.of("param"));

        try {
            callInjectable("throwWebApplicationException", String.class);
        } catch (WebApplicationException e) {
            assertEquals(300, e.getResponse().getStatus());
        }
    }


    @Override
    protected void callInjectable(String method, Class<?> type) {
        try {
            SubResourceLocators.SubResourceLocator locator =
                    new SubResourceLocators.SubResourceLocator(SubResourceMethods.class.getMethod(method, type));
            locator.match(result, "GET", new String[0], context, builder);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    interface SubResourceMethods {
        @Path("/message")
        Message getPathParam(@PathParam("param") String path);

        @Path("/message")
        Message getPathParam(@PathParam("param") int value);

        @Path("/message")
        Message getPathParam(@PathParam("param") double value);

        @Path("/message")
        Message getPathParam(@PathParam("param") float value);

        @Path("/message")
        Message getPathParam(@PathParam("param") long value);

        @Path("/message")
        Message getPathParam(@PathParam("param") short value);

        @Path("/message")
        Message getPathParam(@PathParam("param") byte value);

        @Path("/message")
        Message getPathParam(@PathParam("param") boolean value);

        @Path("/message")
        Message getPathParam(@PathParam("param") BigDecimal value);

        @Path("/message")
        Message getPathParam(@PathParam("param") Converter value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") String value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") int value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") double value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") float value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") short value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") long value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") byte value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") boolean value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") BigDecimal value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") Converter value);

        @Path("/message")
        Message getContext(@Context SameServiceInContext service);

        @Path("/message")
        Message getContext(@Context ResourceContext service);

        @Path("/message")
        Message getContext(@Context UriInfo service);

        @Path("/message/{param}")
        Message throwWebApplicationException(@PathParam("param") String path);
    }

    static class Message {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }
    }

}
