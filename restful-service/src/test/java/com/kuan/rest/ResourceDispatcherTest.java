package com.kuan.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceDispatcherTest {
    private Runtime runtime;
    private RuntimeDelegate delegate;
    private HttpServletRequest request;
    private ResourceContext context;

    @BeforeEach
    public void before() {
        runtime = mock(Runtime.class);
        delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());

        request = mock(HttpServletRequest.class);
        context = mock(ResourceContext.class);
        when(request.getServletPath()).thenReturn("/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaders(eq(HttpHeaders.ACCEPT)))
                .thenReturn(new Vector<>(List.of(MediaType.WILDCARD)).elements());
    }


    @Test
    public void should_user_matched_root_resource() {
        ResourceRouter.RootResource matched = mock(ResourceRouter.RootResource.class);
        UriTemplate matchedUriTemplate = mock(UriTemplate.class);
        UriTemplate.MatchResult result = mock(UriTemplate.MatchResult.class);
        ResourceRouter.ResourceMethod method = mock(ResourceRouter.ResourceMethod.class);
        when(matched.getUriTemplate()).thenReturn(matchedUriTemplate);
        when(matchedUriTemplate.match(any())).thenReturn(Optional.of(result));
        when(matched.match(any(), any(), any(), any())).thenReturn(Optional.of(method));
        GenericEntity entity = new GenericEntity("matched", String.class);
        when(method.call(any(), any())).thenReturn(entity);

        ResourceRouter.RootResource unmatched = mock(ResourceRouter.RootResource.class);
        UriTemplate unmatchedUriTemplate = mock(UriTemplate.class);
        when(unmatched.getUriTemplate()).thenReturn(unmatchedUriTemplate);
        when(unmatchedUriTemplate.match(any())).thenReturn(Optional.empty());

        ResourceRouter router = new DefaultResourceRoot(List.of(matched, unmatched));
        OutboundResponse response = router.dispatch(request, context);
        GenericEntity genericEntity = response.getGenericEntity();
        assertSame(entity, genericEntity);
        assertEquals(200, response.getStatus());
    }

}
