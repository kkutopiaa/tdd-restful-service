package com.kuan.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceDispatcherTest {

    @Test
    public void spike() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ResourceContext context = mock(ResourceContext.class);

        when(request.getServletPath()).thenReturn("/users");
        when(context.getResource(eq(Users.class))).thenReturn(new Users());

        Router router = new Router(Users.class);
        OutboundResponse response = router.dispatch(request, context);
        GenericEntity<String> entity = (GenericEntity<String>) response.getEntity();

        assertEquals("all", entity.getEntity());

    }

    static class Router implements ResourceRouter {
        private Map<Pattern, Class<?>> routerTable = new HashMap<>();

        public Router(Class<Users> rootResource) {
            Path path = rootResource.getAnnotation(Path.class);
            routerTable.put(Pattern.compile(path.value() + "(/.*)?"), rootResource);
       }

        @Override
        public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
            String path = request.getServletPath();
            Pattern matched = routerTable.keySet().stream()
                    .filter(pattern -> pattern.matcher(path).matches()).findFirst().get();
            Class<?> resource = routerTable.get(matched);
            Object object = resourceContext.getResource(resource);

            Method method = Arrays.stream(resource.getMethods())
                    .filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();

            try {
                Object result = method.invoke(object);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return null;
        }
    }

    @Path("/users")
    static class Users {

        @GET
        public String get() {
            return "all";
        }

    }


}
