package com.kuan.rest;

import jakarta.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: qinxuekuan
 * @Date: 2022/10/19
 */
class StubUriInfoBuilder implements UriInfoBuilder {

    private List<Object> matchedResult = new ArrayList<>();

    public StubUriInfoBuilder() {
        matchedResult.add(new SubResourceLocatorsTest.Messages());
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
        return null;
    }

}
