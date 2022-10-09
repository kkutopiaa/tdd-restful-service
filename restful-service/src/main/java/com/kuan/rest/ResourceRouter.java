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
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ResourceRouter {

    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> match(UriTemplate.MatchResult result, String method, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface RootResource extends Resource, UriHandler {
    }

    interface ResourceMethod extends UriHandler {
        String getHttpMethod();

        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);
    }

    interface SubResourceLocator extends UriHandler {
    }

    interface UriHandler {
        UriTemplate getUriTemplate();
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
            return matched.flatMap(result -> resource.match(result, request.getMethod(),
                    Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uriInfoBuilder));
        }
    }


}


class ResourceMethods {
    private Map<String, List<ResourceRouter.ResourceMethod>> resourceMethods;

    public ResourceMethods(Method[] methods) {
        this.resourceMethods = getResourceMethods(methods);
    }

    private static Map<String, List<ResourceRouter.ResourceMethod>> getResourceMethods(Method[] methods) {
        return Arrays.stream(methods)
                .filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(m -> (ResourceRouter.ResourceMethod) new DefaultResourceMethod(m))
                .collect(Collectors.groupingBy(ResourceRouter.ResourceMethod::getHttpMethod));
    }

    public Optional<ResourceRouter.ResourceMethod> findResourceMethods(String path, String method) {
        return Optional.ofNullable(resourceMethods.get(method))
                .flatMap(methods -> match(path, methods));
    }

    private static Optional<ResourceRouter.ResourceMethod> match(String path,
                                                                 List<ResourceRouter.ResourceMethod> methods) {
        return methods.stream()
                .map(m -> ResourceMethods.match(path, m))
                .filter(Result::isMatched)
                .sorted().findFirst()
                .map(Result::handler);
    }

    static private Result<ResourceRouter.ResourceMethod> match(String path, ResourceRouter.ResourceMethod method) {
        return new Result<>(method.getUriTemplate().match(path), method, r -> r.getRemaining() == null);
    }

    static record Result<T extends ResourceRouter.UriHandler>
            (Optional<UriTemplate.MatchResult> matched, T handler,
             Function<UriTemplate.MatchResult, Boolean> matchFunction) implements Comparable<Result<T>> {

        public boolean isMatched() {
            return matched.map(matchFunction).orElse(false);
        }

        @Override
        public int compareTo(Result<T> o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }
    }
}


class RootResourceClass implements ResourceRouter.RootResource {

    private Class<?> resourceClass;
    private UriTemplate uriTemplate;

    private ResourceMethods resourceMethods;

    public RootResourceClass(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        this.uriTemplate = new PathUriTemplate(resourceClass.getAnnotation(Path.class).value());

        Method[] methods = resourceClass.getMethods();
        this.resourceMethods = new ResourceMethods(methods);
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String method,
                                                         String[] mediaTypes, UriInfoBuilder builder) {
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethods(remaining, method);
    }

    @Override
    public UriTemplate getUriTemplate() {
        // 这部分功能： 将 RootResource 上的 @Path 信息，转化为 UriTemplate 对象
        // 可预见的，随着 RootResource 的具体 case 越来越多，这里写的实现逻辑也越来越复杂，
        // 可以确定的是，最终一定会通过重构的方式，将这部分逻辑分离出去。
        // 所以，可以单独把 UriTemplate 抽出来做测试
        return uriTemplate;
    }

}

class SubResource implements ResourceRouter.Resource {

    private Object subResource;
    private ResourceMethods resourceMethods;

    public SubResource(Object subResource) {
        this.subResource = subResource;
        this.resourceMethods = new ResourceMethods(subResource.getClass().getMethods());
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String method,
                                                         String[] mediaTypes, UriInfoBuilder builder) {
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethods(remaining, method);
    }
}

class DefaultResourceMethod implements ResourceRouter.ResourceMethod {

    private String httpMethod;
    private UriTemplate uriTemplate;
    private Method method;

    public DefaultResourceMethod(Method method) {
        this.method = method;
        this.uriTemplate = new PathUriTemplate(
                Optional.ofNullable(method.getAnnotation(Path.class))
                        .map(Path::value)
                        .orElse("")
        );
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

class SubResourceLocators {

    private List<ResourceRouter.SubResourceLocator> subResourceLocators;

    public SubResourceLocators(Method[] methods) {
        subResourceLocators = Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(Path.class)
                        && Arrays.stream(m.getAnnotations())
                        .noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map((Function<Method, ResourceRouter.SubResourceLocator>) DefaultSubResourceLocator::new).toList();
    }

    public Optional<ResourceRouter.SubResourceLocator> findSubResource(String path) {
        return subResourceLocators.stream()
                .filter(l -> l.getUriTemplate().match(path).isPresent())
                .findFirst();
    }

    static class DefaultSubResourceLocator implements ResourceRouter.SubResourceLocator {

        private PathUriTemplate uriTemplate;
        private Method method;

        public DefaultSubResourceLocator(Method method) {
            this.method = method;
            this.uriTemplate = new PathUriTemplate(method.getAnnotation(Path.class).value());
        }

        @Override
        public UriTemplate getUriTemplate() {
            return this.uriTemplate;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + '.' + method.getName();
        }
    }
}