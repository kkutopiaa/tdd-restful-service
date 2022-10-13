package com.kuan.rest;


interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResource(Object resource);

}
