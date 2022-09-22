package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }


}
