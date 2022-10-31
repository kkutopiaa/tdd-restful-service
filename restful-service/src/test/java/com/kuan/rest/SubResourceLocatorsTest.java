package com.kuan.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author: qinxuekuan
 * @Date: 2022/10/8
 */
public class SubResourceLocatorsTest {

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            /hello,                 hello,         fully matched with URI
            /topics/1234,           1234,          multiple matched choices
            /topics/1,              id,            matched with variable
            """)
    public void should_match_path_with_url(String path, String message, String context) {
        StubUriInfoBuilder builder = new StubUriInfoBuilder();

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        assertTrue(locators.findSubResourceMethods(path, "GET", new String[]{MediaType.TEXT_PLAIN},
                Mockito.mock(ResourceContext.class), builder).isPresent());

        assertEquals(message, ((Message) builder.getLastMatchedResource()).message);
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(textBlock = """
            /missing,               unmatched resource method
            /hello/content,         unmatched sub-resource method
            """)
    public void should_return_empty_if_not_match_url(String path, String context) {
        StubUriInfoBuilder builder = new StubUriInfoBuilder();

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        assertFalse(locators.findSubResourceMethods(path, "GET", new String[]{MediaType.TEXT_PLAIN},
                Mockito.mock(ResourceContext.class), builder).isPresent());
    }


    @Test
    public void should_call_locator_method_to_generate_sub_resource() {
        UriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        ResourceRouter.SubResourceLocator subResourceLocator = locators.findSubResource("/hello").get();

        UriTemplate.MatchResult result = Mockito.mock(UriTemplate.MatchResult.class);
        Mockito.when(result.getRemaining()).thenReturn(null);

        ResourceRouter.Resource subResource =
                subResourceLocator.getSubResource(Mockito.mock(ResourceContext.class), uriInfoBuilder);

        ResourceRouter.ResourceMethod method = subResource.match(result, "GET",
                new String[]{MediaType.TEXT_PLAIN}, null, uriInfoBuilder).get();

        assertEquals("Message.content", method.toString());
        assertEquals("hello", ((Message) uriInfoBuilder.getLastMatchedResource()).message);
    }


    @Path("/messages")
    static class Messages {

        @Path("/hello")
        public Message hello() {
            return new Message("hello");
        }

        @Path("/topics/{id}")
        public Message id() {
            return new Message("id");
        }

        @Path("/topics/1234")
        public Message message1234() {
            return new Message("1234");
        }

    }

    static class Message {

        private String message;

        public Message(String message) {
            this.message = message;
        }

        @GET
        public String content() {
            return "content";
        }

    }

}
