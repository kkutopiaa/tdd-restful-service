package com.kuan.rest;

import java.util.Map;
import java.util.Optional;

interface UriTemplate {
    interface MatchResult extends Comparable<MatchResult> {
        String getMatched();

        String getRemaining();

        Map<String, String> getMatchedPathParameters();
    }

    Optional<MatchResult> match(String path);
}

class UriTemplateString implements UriTemplate {

    public UriTemplateString(String template) {

    }

    @Override
    public Optional<MatchResult> match(String path) {
        return null;
    }
}
