package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

/**
 * @Author: qxkk
 * @Date: 2022/9/22
 */
public class ResourceRootTest {

    @Test
    public void should() {
        RootResourceClass root = new RootResourceClass(Users.class);

        UriTemplate uriTemplate = root.getUriTemplate();

    }

    @Path("/users")
    static class Users {
        @GET
        public String get() {
            return "users";
        }
    }

    @Path("/user/{id}")
    static class User {
        @GET
        public String get() {
            return "user";
        }
    }

}
