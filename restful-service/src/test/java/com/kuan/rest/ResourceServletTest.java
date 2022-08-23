package com.kuan.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
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



}
