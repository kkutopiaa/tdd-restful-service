package com.kuan;


import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASpike {

    Server server;


    @BeforeEach
    public void start() throws Exception {
        server = new Server(6666);

        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");

        handler.addServlet(new ServletHolder(new ResourceServlet(new TestApplication())), "/");


        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    public void stop() throws Exception {
        server.stop();
    }

    @Test
    public void test() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:6666/")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response);
        System.out.println(response.body());

        assertEquals("qxk test in resource", response.body());
    }


    @Path("/test")
    static class TestResource {
        public TestResource() {
        }

        @GET
        public String get() {
            return "qxk test in resource";
        }
    }


    static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class);
        }
    }

    static class ResourceServlet extends HttpServlet {
        private Application application;

        public ResourceServlet(Application application) {
            this.application = application;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws  IOException {
            Stream<Class<?>> rootResources = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));


            Object result = dispatch(req, rootResources);


            // 换成 dispatch
//            String result = new TestResource().get();

            // 换成 MessageBodyWriter
            resp.getWriter().write(result.toString());
            resp.getWriter().flush();
        }

        Object dispatch(HttpServletRequest req, Stream<Class<?>> rootResources) {

            try {
                Class<?> rootClass = rootResources.findFirst().get();
                // >>>>>  用 di 去构造一个 component 出来。

                Object rootResource = rootClass.getConstructor().newInstance();
                Method method = Arrays.stream(rootClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                return method.invoke(rootResource);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

}