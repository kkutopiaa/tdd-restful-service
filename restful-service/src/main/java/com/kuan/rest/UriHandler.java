package com.kuan.rest;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @Author: qxkk
 * @Date: 2022/10/9
 */
public interface UriHandler {
    UriTemplate getUriTemplate();
}

class UriHandlers {

    // 真正想重用的方法
    public static <T extends UriHandler, R> Optional<R> match(String path, List<T> handlers,
                                                              Function<UriTemplate.MatchResult, Boolean> matchFunction,
                                                              Function<Optional<Result<T>>, Optional<R>> mapper) {
        return mapper.apply(matched(path, handlers, matchFunction));
    }

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers,
                                                           Function<UriTemplate.MatchResult, Boolean> matchFunction) {
        return matched(path, handlers, matchFunction).map(Result::handler);
    }

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers) {
        return match(path, handlers, r -> true);
    }

    private static <T extends UriHandler> Optional<Result<T>>
    matched(String path, List<T> handlers, Function<UriTemplate.MatchResult, Boolean> matchFunction) {
        return handlers.stream()
                .map(m -> new Result<>(m.getUriTemplate().match(path), m, matchFunction))
                .filter(Result::isMatched)
                .sorted().findFirst();
    }

    static record Result<T extends UriHandler>
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