package com.kuan.rest;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @Author: qxkk
 * @Date: 2022/11/8
 */
class MethodInvoker {
    private static final ValueProvider pathParam = (parameter, uriInfo) ->
            Optional.ofNullable(parameter.getAnnotation(PathParam.class))
                    .map(annotation -> uriInfo.getPathParameters().get(annotation.value()));
    private static final ValueProvider queryParam = (parameter, uriInfo) ->
            Optional.ofNullable(parameter.getAnnotation(QueryParam.class))
                    .map(annotation -> uriInfo.getQueryParameters().get(annotation.value()));
    private static final List<ValueProvider> providers = List.of(pathParam, queryParam);

    static Object invoke(Method method, ResourceContext resourceContext, UriInfoBuilder builder) {
        try {
            UriInfo uriInfo = builder.createUriInfo();

            Object[] parameters = Arrays.stream(method.getParameters())
                    .map(parameter -> injectParameter(parameter, uriInfo)
                            .or(() -> injectContext(parameter, resourceContext, uriInfo))
                            .orElse(null)
                    ).toArray();

            return method.invoke(builder.getLastMatchedResource(), parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<Object> injectParameter(Parameter parameter, UriInfo uriInfo) {
        return providers.stream()
                .map(provider -> provider.provide(parameter, uriInfo))
                .filter(Optional::isPresent)
                .findFirst()
                .flatMap(values -> values.flatMap(v -> convert(parameter, v)));
    }

    private static Optional<Object> injectContext(Parameter parameter, ResourceContext resourceContext, UriInfo uriInfo) {
        if (parameter.getType().equals(ResourceContext.class)) {
            return Optional.of(resourceContext);
        }
        if (parameter.getType().equals(UriInfo.class)) {
            return Optional.of(uriInfo);
        }
        return Optional.of(resourceContext.getResource(parameter.getType()));
    }

    private static Optional<Object> convert(Parameter parameter, List<String> values) {
        return PrimitiveConverter.convert(parameter, values)
                .or(() -> ConstructorConverter.convert(parameter.getType(), values.get(0)))
                .or(() -> FactoryConverter.convert(parameter.getType(), values.get(0)));

    }

    interface ValueProvider {
        Optional<List<String>> provide(Parameter parameter, UriInfo uriInfo);
    }

    interface ValueConverter<T> {
        T fromString(List<String> values);

        static <T> ValueConverter<T> singleValue(Function<String, T> converter) {
            return values -> converter.apply(values.get(0));
        }
    }
}


class PrimitiveConverter {
    private static final Map<Type, MethodInvoker.ValueConverter<Object>> converters = Map.of(
            double.class, MethodInvoker.ValueConverter.singleValue(Double::parseDouble),
            float.class, MethodInvoker.ValueConverter.singleValue(Float::parseFloat),
            long.class, MethodInvoker.ValueConverter.singleValue(Long::parseLong),
            int.class, MethodInvoker.ValueConverter.singleValue(Integer::parseInt),
            short.class, MethodInvoker.ValueConverter.singleValue(Short::parseShort),
            byte.class, MethodInvoker.ValueConverter.singleValue(Byte::parseByte),
            boolean.class, MethodInvoker.ValueConverter.singleValue(Boolean::parseBoolean),
            String.class, MethodInvoker.ValueConverter.singleValue(s -> s)
    );

    static Optional<Object> convert(Parameter parameter, List<String> values) {
        return Optional.ofNullable(converters.get(parameter.getType()))
                .map(c -> c.fromString(values));
    }
}

class ConstructorConverter {
    public static Optional<Object> convert(Class<?> converter, String value) {
        try {
            return Optional.of(converter.getConstructor(String.class).newInstance(value));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}

class FactoryConverter {
    public static Optional<Object> convert(Class<?> converter, String value) {
        try {
            return Optional.of(converter.getMethod("valueOf", String.class).invoke(null, value));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}