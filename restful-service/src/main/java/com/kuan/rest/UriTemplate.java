package com.kuan.rest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface UriTemplate {
    interface MatchResult extends Comparable<MatchResult> {
        String getMatched();

        String getRemaining();

        Map<String, String> getMatchedPathParameters();
    }

    Optional<MatchResult> match(String path);
}

class UriTemplateString implements UriTemplate {

    private static final String LEFT_BRACKET = "\\{";
    private static final String RIGHT_BRACKET = "}";
    private static final String VARIABLE_NAME = "\\w[\\w.-]*";
    private static final Pattern variable = Pattern.compile(LEFT_BRACKET + group(VARIABLE_NAME) + RIGHT_BRACKET);

    private final Pattern pattern;
    private final List<String> variables = new ArrayList<>();

    private static String group(String pattern) {
        return "(" + pattern + ")";
    }

    public UriTemplateString(String template) {
        pattern = Pattern.compile("(" + variable(template) + ")" + "(/.*)?");
    }

    private String variable(String template) {
        return variable.matcher(template).replaceAll(result -> {
            variables.add(result.group(1));
            return "([^/]+?)";
        });
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int count = matcher.groupCount();

        Map<String, String> parameters = new HashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            parameters.put(variables.get(i), matcher.group(2 + i));
        }

        return Optional.of(new MatchResult() {
            @Override
            public String getMatched() {
                return matcher.group(1);
            }

            @Override
            public String getRemaining() {
                return matcher.group(count);
            }

            @Override
            public Map<String, String> getMatchedPathParameters() {
                return parameters;
            }

            @Override
            public int compareTo(MatchResult o) {
                return 0;
            }
        });
    }
}
