package com.kuan.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;

public class ResourceServlet extends HttpServlet {

    private Runtime runtime;
    private Providers providers;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
        this.providers = runtime.getProviders();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();

        OutboundResponse response;
        try {
            response = router.dispatch(req, runtime.createResourceContext(req, resp));
        } catch (WebApplicationException exception) {
            response = (OutboundResponse) exception.getResponse();
        } catch (Throwable throwable) {
            try {
                ExceptionMapper exceptionMapper = providers.getExceptionMapper(throwable.getClass());
                response = (OutboundResponse) exceptionMapper.toResponse(throwable);
            } catch (WebApplicationException exception) {
                response = (OutboundResponse) exception.getResponse();
            }catch (Throwable throwable1) {
                ExceptionMapper exceptionMapper = providers.getExceptionMapper(throwable1.getClass());
                response = (OutboundResponse) exceptionMapper.toResponse(throwable1);
            }
        }


        respond(resp, response);

    }

    private void respond(HttpServletResponse resp, OutboundResponse response) throws IOException {
        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        for (String name : headers.keySet()) {
            for (Object value : headers.get(name)) {
                RuntimeDelegate.HeaderDelegate headerDelegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name, headerDelegate.toString(value));
            }
        }

        GenericEntity entity = response.getGenericEntity();
        if (entity != null) {
            MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(),
                    response.getAnnotations(), response.getMediaType());
            writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(),
                    response.getAnnotations(), response.getMediaType(), response.getHeaders(), resp.getOutputStream());
        }
    }
}
