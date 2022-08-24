package com.kuan.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceServletTest extends ServletTest {

    private ResourceRouter router;
    private ResourceContext resourceContext;
    private Providers providers;
    private final OutboundResponseBuilder builder = new OutboundResponseBuilder();


    @Override
    protected Servlet getServlet() {
        Runtime runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);

        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getProviders()).thenReturn(providers);

        return new ResourceServlet(runtime);
    }

    @BeforeEach
    public void before() {
        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
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


        when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                                               MediaType mediaType) {
                        return false;
                    }

                    @Override
                    public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations,
                                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                                        OutputStream entityStream)
                            throws WebApplicationException {
                        PrintWriter writer = new PrintWriter(entityStream);
                        writer.write(s);
                        writer.flush();
                    }
                });

    }

    @Test
    public void should_use_status_from_response() throws Exception {
        builder.status(Response.Status.NOT_MODIFIED).build(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }



    @Test
    public void should_use_http_headers_from_response() throws Exception {
        builder.headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
                        new NewCookie.Builder("USER_ID").value("user").build())
                .build(router);

        HttpResponse<String> httpResponse = get("/test");

        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    @Test
    public void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        builder.entity(new GenericEntity<>("entity", String.class), new Annotation[0])
                .build(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals("entity", httpResponse.body());
    }


    class OutboundResponseBuilder {
        Response.Status status = Response.Status.OK;
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutboundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        public OutboundResponseBuilder headers(String name, Object... values) {
            this.headers.addAll(name, values);
            return this;
        }

        public OutboundResponseBuilder entity(GenericEntity<Object> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        void build(ResourceRouter router) {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);

            when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
        }


    }


}
