package com.kuan.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @Author: qxkk
 * @Date: 2022/9/26
 */
public class RootResourceTest {

    @Test
    public void should_get_uri_template_from_path_annotation() {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate template = resource.getUriTemplate();
        assertTrue(template.match("/messages/hello").isPresent());
    }

    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            /messages/hello,       GET,        Messages.hello,          GET and URI match
            /messages/ah,          GET,        Messages.ah,             GET and URI match
            /messages/hello,       POST,       Messages.postHello,      POST and URI match
            /messages/hello,       PUT,        Messages.putHello,       PUT and URI match
            /messages/hello,       DELETE,     Messages.deleteHello,    DELETE and URI match
            /messages/hello,       PATCH,      Messages.patchHello,     PATCH and URI match
            /messages/hello,       HEAD,       Messages.headHello,      HEAD and URI match
            /messages/hello,       OPTIONS,    Messages.optionsHello,   OPTIONS and URI match
            /messages/topics/1234, GET,        Messages.topic1234,      GET with multiply choices
            /messages,             GET,        Messages.get,            GET with resource method without Path
            """)
    public void should_match_resource_method(String path, String httpMethod, String resourceMethod, String context) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        ResourceRouter.ResourceMethod method = resource.match(result, httpMethod,
                new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class)).get();
        assertEquals(resourceMethod, method.toString());
    }


    @Test
    public void should_return_empty_if_uri_not_match() {
        ResourceRouter.RootResource resource = new RootResourceClass(MissingMessages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match("/missing-messages/1").get();

        assertTrue(resource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN},
                        Mockito.mock(UriInfoBuilder.class))
                .isEmpty());
    }

    @Path("/missing-messages")
    static class MissingMessages {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "/messages";
        }

    }

    @Path("/messages")
    static class Messages {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }

        @GET
        @Path("/ah")
        @Produces(MediaType.TEXT_PLAIN)
        public String ah() {
            return "ah";
        }

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }

        @POST
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String postHello() {
            return "postHello";
        }

        @PUT
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String putHello() {
            return "Hello";
        }

        @DELETE
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String deleteHello() {
            return "Hello";
        }

        @HEAD
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String headHello() {
            return "Hello";
        }

        @OPTIONS
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String optionsHello() {
            return "Hello";
        }

        @PATCH
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String patchHello() {
            return "Hello";
        }

        @GET
        @Path("/topics/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String topicId() {
            return "topicId";
        }

        @GET
        @Path("/topics/1234")
        @Produces(MediaType.TEXT_PLAIN)
        public String topic1234() {
            return "topic1234";
        }

    }

}
