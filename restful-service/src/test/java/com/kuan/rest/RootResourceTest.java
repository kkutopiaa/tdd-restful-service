package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

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


    @Path("/messages")
    static class Messages {

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }

    }

}
