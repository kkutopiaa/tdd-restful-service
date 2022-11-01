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

    interface Resource extends UriHandler {
        Optional<ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes,
                                       ResourceContext resourceContext, UriInfoBuilder builder);
    }

    interface ResourceMethod extends UriHandler {
        String getHttpMethod();

        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);
    }

}


class DefaultResourceRoot implements ResourceRouter {

    private Runtime runtime;
    private List<Resource> rootResources;

    public DefaultResourceRoot(Runtime runtime, List<Resource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();
        UriInfoBuilder uriInfoBuilder = runtime.createUriInfoBuilder(request);
        List<Resource> rootResources = this.rootResources;
        Optional<ResourceMethod> method = UriHandlers.mapMatched(path, rootResources,
                (result, resource) -> getResourceMethod(request, resourceContext, uriInfoBuilder, result, resource));

        if (method.isEmpty()) {
            return (OutboundResponse) Response.status(Response.Status.NOT_FOUND).build();
        }

        return (OutboundResponse) method.map(m -> m.call(resourceContext, uriInfoBuilder))
                .map(entity -> Response.ok(entity).build())
                .orElseGet(() -> Response.noContent().build());
    }

    private static Optional<ResourceMethod>
    getResourceMethod(HttpServletRequest request, ResourceContext resourceContext, UriInfoBuilder uriInfoBuilder,
                      Optional<UriTemplate.MatchResult> matched, Resource handler) {
        return handler.match(matched.get(), request.getMethod(),
                Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new),
                resourceContext, uriInfoBuilder);
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

    public Optional<ResourceRouter.ResourceMethod> findResourceMethods(String path, String httpMethod) {
        return findMethod(path, httpMethod)
                .or(() -> findAlternative(path, httpMethod));
    }

    private Optional<ResourceRouter.ResourceMethod> findAlternative(String path, String httpMethod) {
        if (HttpMethod.HEAD.equals(httpMethod)) {
            return findMethod(path, HttpMethod.GET).map(HeadResourceMethod::new);
        }
        if (HttpMethod.OPTIONS.equals(httpMethod)) {
            return Optional.of(new OptionResourceMethod());
        }
        return Optional.empty();
    }
    private Optional<ResourceRouter.ResourceMethod> findMethod(String path, String httpMethod) {
        return Optional.ofNullable(resourceMethods.get(httpMethod))
                .flatMap(methods -> UriHandlers.match(path, methods, r -> r.getRemaining() == null));
    }

    class OptionResourceMethod implements ResourceRouter.ResourceMethod {
        @Override
        public String getHttpMethod() {
            return null;
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
            return null;
        }

        @Override
        public UriTemplate getUriTemplate() {
            return null;
        }
    }


}

class HeadResourceMethod implements ResourceRouter.ResourceMethod {

    ResourceRouter.ResourceMethod resourceMethod;

    public HeadResourceMethod(ResourceRouter.ResourceMethod resourceMethod) {
        this.resourceMethod = resourceMethod;
    }

    @Override
    public String getHttpMethod() {
        return resourceMethod.getHttpMethod();
    }

    @Override
    public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
        return resourceMethod.call(resourceContext, builder);
    }

    @Override
    public UriTemplate getUriTemplate() {
        return resourceMethod.getUriTemplate();
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

    private List<ResourceRouter.Resource> subResourceLocators;

    public SubResourceLocators(Method[] methods) {
        subResourceLocators = Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(Path.class)
                        && Arrays.stream(m.getAnnotations())
                        .noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map((Function<Method, ResourceRouter.Resource>) SubResourceLocator::new).toList();
    }

    public Optional<ResourceRouter.ResourceMethod>
    findSubResourceMethods(String path, String method, String[] mediaTypes,
                           ResourceContext resourceContext, UriInfoBuilder uriInfoBuilder) {
        return UriHandlers.mapMatched(path, subResourceLocators, (result, locator) ->
                locator.match(result.get(), method, mediaTypes, resourceContext, uriInfoBuilder)
        );
    }

    static class SubResourceLocator implements ResourceRouter.Resource {

        private PathUriTemplate uriTemplate;
        private Method method;

        public SubResourceLocator(Method method) {
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

        @Override
        public Optional<ResourceRouter.ResourceMethod>
        match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes,
              ResourceContext resourceContext, UriInfoBuilder builder) {
            Object resource = builder.getLastMatchedResource();

            try {
                Object subResource = method.invoke(resource);
                return new ResourceHandler(subResource, uriTemplate)
                        .match(result, httpMethod, mediaTypes, resourceContext, builder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}

class ResourceHandler implements ResourceRouter.Resource {

    private UriTemplate uriTemplate;

    private ResourceMethods resourceMethods;

    private SubResourceLocators subResourceLocators;
    private Function<ResourceContext, Object> resource;

    public ResourceHandler(Class<?> resourceClass) {
        this(resourceClass, new PathUriTemplate(getTemplate(resourceClass)), rc -> rc.getResource(resourceClass));
    }

    private static String getTemplate(Class<?> resourceClass) {
        if (!resourceClass.isAnnotationPresent(Path.class)) {
            throw new IllegalArgumentException();
        }
        return resourceClass.getAnnotation(Path.class).value();
    }

    public ResourceHandler(Object resource, UriTemplate uriTemplate) {
        this(resource.getClass(), uriTemplate, rc -> resource);
    }

    private ResourceHandler(Class<?> resourceClass, UriTemplate uriTemplate,
                            Function<ResourceContext, Object> resource) {
        this.uriTemplate = uriTemplate;
        this.resourceMethods = new ResourceMethods(resourceClass.getMethods());
        this.subResourceLocators = new SubResourceLocators(resourceClass.getMethods());
        this.resource = resource;
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod>
    match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes,
          ResourceContext resourceContext, UriInfoBuilder builder) {
        builder.addMatchedResource(resource.apply(resourceContext));

        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethods(remaining, httpMethod)
                .or(() -> subResourceLocators.findSubResourceMethods(remaining, httpMethod, mediaTypes,
                        resourceContext, builder));
    }

    private Optional<ResourceRouter.ResourceMethod> alternative(String remaining, String httpMethod) {
        if ("HEAD".equals(httpMethod)) {
            return resourceMethods.findResourceMethods(remaining, "GET");
        }
        return Optional.empty();
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