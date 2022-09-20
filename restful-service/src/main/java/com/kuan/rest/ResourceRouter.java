package com.kuan.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface ResourceRouter {

    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> match(String path, String method, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface RootResource extends Resource {
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);
    }

}


class DefaultResourceRoot implements ResourceRouter {

    private Runtime runtime;
    private List<RootResource> rootResources;

    public DefaultResourceRoot(Runtime runtime, List<RootResource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();
        UriInfoBuilder uriInfoBuilder = runtime.createUriInfoBuilder(request);
        Optional<Result> matched = rootResources.stream()
                .map(resource -> new Result(resource.getUriTemplate().match(path), resource))
                .filter(result -> result.matched.isPresent())
                .min(Comparator.comparing(result -> result.matched.get()));
        Optional<ResourceMethod> method = matched.flatMap(
                result -> result.resource.match(result.matched.get().getRemaining(), request.getMethod(),
                        Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uriInfoBuilder)
        );
        GenericEntity<?> entity = method.map(m -> m.call(resourceContext, uriInfoBuilder)).get();
        System.out.println("entity >> " + entity);

        return (OutboundResponse) Response.ok(entity).build();
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) {

    }


}
