package com.kuan.rest;


import jakarta.servlet.Servlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class ServletTest {

    private Server server;

    private final int port = 6666;

    @BeforeEach
    public void start() throws Exception {
        server = new Server(port);

        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        handler.addServlet(new ServletHolder(getServlet()), "/");

        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    public void stop() throws Exception {
        server.stop();
    }

    protected abstract Servlet getServlet();

    protected URI path(String path) throws Exception {
        String base = "http://localhost:" + port + "/";
        return new URL(new URL(base), path).toURI();
    }

    public HttpResponse<String> get(String path)  {
        try {
            HttpClient client = HttpClient.newHttpClient();
            URI uri = path(path);
            System.out.println("in get , uri : " + uri);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}