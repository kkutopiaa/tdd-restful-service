package com.kuan.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public interface ResourceRouter {

    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> match(String path, String method, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface RootResource extends Resource {
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod {
        String getHttpMethod();

        UriTemplate getUriTemplate();

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
        Optional<ResourceMethod> method = rootResources.stream()
                .map(resource -> match(path, resource))
                .filter(Result::isMatched)
                .sorted().findFirst()
                .flatMap(result -> result.findResourceMethod(request, uriInfoBuilder));

        if (method.isEmpty()) {
            return (OutboundResponse) Response.status(Response.Status.NOT_FOUND).build();
        }

        return (OutboundResponse) method.map(m -> m.call(resourceContext, uriInfoBuilder))
                .map(entity -> Response.ok(entity).build())
                .orElseGet(() -> Response.noContent().build());
    }

    private Result match(String path, RootResource resource) {
        return new Result(resource.getUriTemplate().match(path), resource);
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) implements Comparable<Result> {

        private boolean isMatched() {
            return matched.isPresent();
        }

        @Override
        public int compareTo(Result o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo))
                    .orElse(0);
        }

        private Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, UriInfoBuilder uriInfoBuilder) {
            return matched.flatMap(result -> resource.match(result.getRemaining(), request.getMethod(),
                    Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uriInfoBuilder));
        }
    }


}


class RootResourceClass implements ResourceRouter.RootResource {

    private Class<?> resourceClass;
    private UriTemplate uriTemplate;

    private Map<String, List<ResourceRouter.ResourceMethod>> resourceMethods;

    public RootResourceClass(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        this.uriTemplate = new PathUriTemplate(resourceClass.getAnnotation(Path.class).value());

        this.resourceMethods = Arrays.stream(resourceClass.getMethods())
                .filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(m -> (ResourceRouter.ResourceMethod) new DefaultResourceMethod(m))
                .collect(Collectors.groupingBy(ResourceRouter.ResourceMethod::getHttpMethod));


    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(String path, String method, String[] mediaTypes,
                                                         UriInfoBuilder builder) {
        UriTemplate.MatchResult result = uriTemplate.match(path).get();
        String remaining = result.getRemaining();
        return resourceMethods.get(method).stream()
                .map(m -> match(remaining, m))
                .filter(Result::isMatched)
                .sorted().findFirst()
                .map(Result::resourceMethod);

    }

    @Override
    public UriTemplate getUriTemplate() {
        // 这部分功能： 将 RootResource 上的 @Path 信息，转化为 UriTemplate 对象
        // 可预见的，随着 RootResource 的具体 case 越来越多，这里写的实现逻辑也越来越复杂，
        // 可以确定的是，最终一定会通过重构的方式，将这部分逻辑分离出去。
        // 所以，可以单独把 UriTemplate 抽出来做测试
        return uriTemplate;
    }


    private Result match(String path, ResourceRouter.ResourceMethod method) {
        return new Result(method.getUriTemplate().match(path), method);
    }

    record Result(Optional<UriTemplate.MatchResult> matched,
                  ResourceRouter.ResourceMethod resourceMethod) implements Comparable<Result> {

        public boolean isMatched() {
            return matched.map(r -> r.getRemaining() == null).orElse(false);
        }

        @Override
        public int compareTo(Result o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }
    }


    static class DefaultResourceMethod implements ResourceRouter.ResourceMethod {

        private String httpMethod;
        private UriTemplate uriTemplate;
        private Method method;

        public DefaultResourceMethod(Method method) {
            this.method = method;
            this.uriTemplate = new PathUriTemplate(method.getAnnotation(Path.class).value());
            this.httpMethod = Arrays.stream(method.getAnnotations())
                    .filter(a -> a.annotationType().isAnnotationPresent(HttpMethod.class))
                    .findFirst()
                    .get().annotationType().getAnnotation(HttpMethod.class).value();
        }

        @Override
        public String getHttpMethod() {
            return httpMethod;
        }

        @Override
        public UriTemplate getUriTemplate() {
            return uriTemplate;
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
            return null;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
    }

}
