package com.kuan.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author: qxkk
 * @Date: 2022/9/22
 */
public class PathUriTemplateTest {

    @Test
    public void should_return_empty_if_path_not_matched() {
        PathUriTemplate template = new PathUriTemplate("/users");

        Optional<UriTemplate.MatchResult> result = template.match("/orders");

        assertTrue(result.isEmpty());
    }

    @Test
    public void should_return_match_result_if_path_matched() {
        PathUriTemplate template = new PathUriTemplate("/users");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users", result.getMatched());
        assertEquals("/1", result.getRemaining());
        assertTrue(result.getMatchedPathParameters().isEmpty());
    }

    @Test
    public void should_return_match_result_if_path_with_variable_matched() {
        PathUriTemplate template = new PathUriTemplate("/users/{id}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users/1", result.getMatched());
        assertNull(result.getRemaining());
        assertFalse(result.getMatchedPathParameters().isEmpty());
        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_return_empty_if_not_match_given_pattern() {
        PathUriTemplate template = new PathUriTemplate("/users/{id:[0-9]+}");

        assertTrue(template.match("/users/id").isEmpty());
    }

    @Test
    public void should_extract_variable_value_by_given_pattern() {
        PathUriTemplate template = new PathUriTemplate("/users/{id:[0-9]+}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    public void should_throw_illegal_argument_exception_if_variable_redefined() {
        assertThrows(IllegalArgumentException.class, () -> new PathUriTemplate("/users/{id:[0-9]+}/{id}"));
    }

    @Test
    public void should_compare_equal_match_result() {
        PathUriTemplate template = new PathUriTemplate("/users/{id}");
        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals(0, result.compareTo(result));
    }

    @ParameterizedTest
    @CsvSource({"/users/1234,/users/1234,/users/{id}",
            "/users/1234567890/order,/{resources}/1234567890/{action},/users/{id}/order",
            "/users/1,/users/{id:[0-9]+},/users/{id}"})
    public void first_pattern_should_be_smaller_than_second(String path, String smallerTemplate, String largerTemplate) {
        PathUriTemplate smaller = new PathUriTemplate(smallerTemplate);
        PathUriTemplate larger = new PathUriTemplate(largerTemplate);

        UriTemplate.MatchResult leftHeadSide = smaller.match(path).get();
        UriTemplate.MatchResult rightHeadSide = larger.match(path).get();

        assertTrue(leftHeadSide.compareTo(rightHeadSide) < 0);
        assertTrue(rightHeadSide.compareTo(leftHeadSide) > 0);
    }
}
