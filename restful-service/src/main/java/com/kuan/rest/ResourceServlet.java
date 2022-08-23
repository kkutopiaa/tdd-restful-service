package com.kuan.rest;

import jakarta.servlet.http.HttpServlet;

public class ResourceServlet extends HttpServlet {

    private Runtime runtime;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
    }
}
