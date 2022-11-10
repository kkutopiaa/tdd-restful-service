package com.kuan.rest;


import jakarta.ws.rs.core.UriInfo;

import java.util.Map;

interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResource(Object resource);

    UriInfo createUriInfo();

    void addMatchedPathParameters(Map<String, String> pathParameters);

}
