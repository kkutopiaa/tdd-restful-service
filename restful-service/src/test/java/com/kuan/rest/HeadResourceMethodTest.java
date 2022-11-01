package com.kuan.rest;

import jakarta.ws.rs.container.ResourceContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 * @Author: qxkk
 * @Date: 2022/11/1
 */
public class HeadResourceMethodTest {

    @Test
    public void should_call_method_and_ignore_return_value() {
        ResourceRouter.ResourceMethod method = mock(ResourceRouter.ResourceMethod.class);
        ResourceContext context = mock(ResourceContext.class);
        UriInfoBuilder builder = mock(UriInfoBuilder.class);

        HeadResourceMethod headResourceMethod = new HeadResourceMethod(method);

        // 状态验证
        assertNull(headResourceMethod.call(context, builder));

        // 行为验证
        Mockito.verify(method).call(same(context), same(builder));
    }


    @Test
    public void should_delegate_to_method_for_uri_template() {
        ResourceRouter.ResourceMethod method = mock(ResourceRouter.ResourceMethod.class);

        UriTemplate uriTemplate = mock(UriTemplate.class);
        when(method.getUriTemplate()).thenReturn(uriTemplate);

        HeadResourceMethod headResourceMethod = new HeadResourceMethod(method);

        assertEquals(uriTemplate, headResourceMethod.getUriTemplate());
    }


    @Test
    public void should_delegate_to_method_for_http_method() {
        ResourceRouter.ResourceMethod method = mock(ResourceRouter.ResourceMethod.class);

        when(method.getHttpMethod()).thenReturn("GET");

        HeadResourceMethod headResourceMethod = new HeadResourceMethod(method);

        // 行为验证，只能验证调用了 getHttpMethod 方法，不能验证方法做了什么工作
        headResourceMethod.getHttpMethod();
        verify(method).getHttpMethod();

        // 状态验证
        assertEquals("GET", headResourceMethod.getHttpMethod());
    }

}
