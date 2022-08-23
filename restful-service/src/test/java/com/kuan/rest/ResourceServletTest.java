package com.kuan.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceServletTest extends ServletTest {

    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);

        when(runtime.getResourceRouter())
                .thenReturn(router);
        when(runtime.createResourceContext(any(), any()))
                .thenReturn(resourceContext);

        return new ResourceServlet(runtime);
    }

    @Test
    public void should_use_status_from_response() throws Exception {
        OutboundResponse response = mock(OutboundResponse.class);
        when(response.getStatus())
                .thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        when(router.dispatch(any(), eq(resourceContext)))
                .thenReturn(response);

        HttpResponse<String> httpResponse = get("/test");

        Assertions.assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());

    }



}
