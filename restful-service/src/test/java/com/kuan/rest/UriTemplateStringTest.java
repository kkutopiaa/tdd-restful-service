package com.kuan.rest;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author: qxkk
 * @Date: 2022/9/22
 */
public class UriTemplateStringTest {

    @Test
    public void should_return_empty_if_path_not_matched() {
        UriTemplateString template = new UriTemplateString("/users");

        Optional<UriTemplate.MatchResult> result = template.match("/orders");

        assertTrue(result.isEmpty());
    }

    @Test
    public void should_return_match_result_if_path_matched() {
        UriTemplateString template = new UriTemplateString("/users");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users", result.getMatched());
        assertEquals("/1", result.getRemaining());
        assertTrue(result.getMatchedPathParameters().isEmpty());
    }

    @Test
    public void should_return_match_result_if_path_with_variable_matched() {
        UriTemplateString template = new UriTemplateString("/users/{id}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users/1", result.getMatched());
        assertNull(result.getRemaining());
        assertFalse(result.getMatchedPathParameters().isEmpty());
        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_return_empty_if_not_match_given_pattern() {
        UriTemplateString template = new UriTemplateString("/users/{id:[0-9]+}");

        assertTrue(template.match("/users/id").isEmpty());
    }

    @Test
    public void should_extract_variable_value_by_given_pattern() {
        UriTemplateString template = new UriTemplateString("/users/{id:[0-9]+}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_throw_illegal_argument_exception_if_variable_redefined() {
        assertThrows(IllegalArgumentException.class, () -> new UriTemplateString("/users/{id:[0-9]+}/{id}"));
    }

    @Test
    public void should_compare_for_match_literal() {
        assertChosen("/users/1234", "/users/1234", "/users/{id}");
    }

    @Test
    public void should_compare_match_variables_if_matched_literal_equally() {
        assertChosen("/users/1234567890/order",
                "/{resources}/1234567890/{action}", "/users/{id}/order");

    }

    @Test
    public void should_compare_specific_variable_if_matched_literal_variables_same() {
        assertChosen("/users/1", "/users/{id:[0-9]+}", "/users/{id}");
    }

    private static void assertChosen(String path, String smallerTemplate, String largerTemplate) {
        UriTemplateString smaller = new UriTemplateString(smallerTemplate);
        UriTemplateString larger = new UriTemplateString(largerTemplate);

        UriTemplate.MatchResult leftHeadSide = smaller.match(path).get();
        UriTemplate.MatchResult rightHeadSide = larger.match(path).get();

        assertTrue(leftHeadSide.compareTo(rightHeadSide) < 0);
    }
}
