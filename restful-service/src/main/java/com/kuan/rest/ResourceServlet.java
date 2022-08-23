package com.kuan.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ResourceServlet extends HttpServlet {

    private Runtime runtime;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();
        OutboundResponse response = router.dispatch(req, runtime.createResourceContext(req, resp));
        resp.setStatus(response.getStatus());
    }
}
