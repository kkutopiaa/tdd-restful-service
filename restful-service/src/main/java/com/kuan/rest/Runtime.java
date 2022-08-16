package com.kuan.rest;

import com.tdd.di.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.ext.Providers;

public interface Runtime {

    Providers getProviders();

    ResourceContext createResourceContext(HttpServletRequest request, HttpServletResponse response);

    // 指明是 application scope 的， 还有 request scope 的。
    Context getApplicationContext();

    ResourceRouter getResourceRouter();

}
