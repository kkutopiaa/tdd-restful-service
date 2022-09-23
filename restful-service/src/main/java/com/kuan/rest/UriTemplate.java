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
    private static final String NON_BRACKETS = "[^\\{}]+";
    private static final Pattern variable = Pattern.compile(LEFT_BRACKET + group(VARIABLE_NAME) +
            group(":" + group(NON_BRACKETS)) + "?" + RIGHT_BRACKET);
    private static final int variableNameGroup = 1;
    private static final int variablePatternGroup = 3;

    private static final String defaultVariablePattern = "([^/]+?)";


    private final Pattern pattern;
    private final List<String> variables = new ArrayList<>();
    private final int variableGroupStartFrom;


    private static String group(String pattern) {
        return "(" + pattern + ")";
    }

    public UriTemplateString(String template) {
        pattern = Pattern.compile(group(variable(template)) + "(/.*)?");
        variableGroupStartFrom = 2;
    }

    private String variable(String template) {
        return variable.matcher(template).replaceAll(result -> {
            variables.add(result.group(variableNameGroup));
            return result.group(variablePatternGroup) == null ? defaultVariablePattern
                    : group(result.group(variablePatternGroup));
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
            parameters.put(variables.get(i), matcher.group(variableGroupStartFrom + i));
        }

        return Optional.of(new MatchResult() {
            @Override
            public String getMatched() {
                return matcher.group(variableNameGroup);
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
