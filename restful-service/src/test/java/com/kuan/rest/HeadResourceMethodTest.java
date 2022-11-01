package com.kuan.rest;

import jakarta.ws.rs.container.ResourceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;

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


}
