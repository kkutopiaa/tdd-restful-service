package com.kuan.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * author: qxkk
 * date: 2022/11/9
 */
public class IntegrationTest extends ServletTest {

    private ResourceRouter router;
    private ResourceContext resourceContext;
    private Providers providers;
    private RuntimeDelegate delegate;
    private UriInfo uriInfo;
    private MultivaluedMap<String, String> parameters;

    @Override
    protected Servlet getServlet() {
        Runtime runtime = mock(Runtime.class);
        router = new DefaultResourceRoot(runtime, List.of(new ResourceHandler(UsersApi.class)));
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);
        uriInfo = mock(UriInfo.class);

        parameters = new MultivaluedHashMap<>();
        parameters.put("id", List.of("not-exist"));
        when(uriInfo.getPathParameters()).thenReturn(parameters);

        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.createUriInfoBuilder(any())).thenReturn(new StubUriInfoBuilder(uriInfo));
        when(runtime.getProviders()).thenReturn(providers);
        when(resourceContext.getResource(eq(UsersApi.class))).thenReturn(new UsersApi());

        return new ResourceServlet(runtime);
    }

    @BeforeEach
    public void before() {
        delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        when(delegate.createHeaderDelegate(eq(NewCookie.class)))
                .thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
                    @Override
                    public NewCookie fromString(String value) {
                        return null;
                    }

                    @Override
                    public String toString(NewCookie value) {
                        return value.getName() + "=" + value.getValue();
                    }
                });

        when(providers.getExceptionMapper(any()))
                .thenReturn(exception -> {
                    exception.printStackTrace();
                    return new StubResponseBuilder().status(500).build();
                });


    }

    // get url (root/sub)
    // get url throw exception

    // get url in exist
    @Test
    public void should_return_404_if_url_in_exist() {
        HttpResponse<String> response = get("/customers");
        Assertions.assertEquals(404, response.statusCode());
    }

    @Test
    @Disabled
    public void should_return_404_if_user_not_exist() {
        HttpResponse<String> response = get("/users/not-exist");
        Assertions.assertEquals(404, response.statusCode());
    }

}

record UserData(String name, String email) {

}

class User {
    String id;
    UserData data;

    public User(String id, UserData data) {
        this.id = id;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public UserData getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


@Path("/users")
class UsersApi {

    List<User> users;

    public UsersApi() {
        this.users = List.of(
                new User("john-smith", new UserData("john smith", "john.smith@email.com"))
        );
    }

    @Path("/{id}")
    public UserApi findUserById(@PathParam("id") String id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .map(UserApi::new)
                .orElseThrow(() -> new WebApplicationException(404));
    }

}

class UserApi {

    private User user;

    public UserApi(User user) {
        this.user = user;
    }

    @GET
    public String get() {
        return "";
    }

}
