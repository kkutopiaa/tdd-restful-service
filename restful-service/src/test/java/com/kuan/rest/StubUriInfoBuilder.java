package com.kuan.rest;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: qinxuekuan
 * @Date: 2022/10/19
 */
class StubUriInfoBuilder implements UriInfoBuilder {

    private List<Object> matchedResult = new ArrayList<>();

    private UriInfo uriInfo;

    public StubUriInfoBuilder() {
        matchedResult.add(new SubResourceLocatorsTest.Messages());
    }

    public StubUriInfoBuilder(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public Object getLastMatchedResource() {
        return matchedResult.get(matchedResult.size() - 1);
    }

    @Override
    public void addMatchedResource(Object resource) {
        matchedResult.add(resource);
    }

    @Override
    public UriInfo createUriInfo() {
        return uriInfo;
    }

    public MultivaluedMap<String, String> getPathParameters() {
        return null;
    }

}
