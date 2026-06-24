package com.flowforge.model.task;

import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link HttpRequestTask} against a real, in-process
 * {@link HttpServer} on the loopback interface. This exercises the full
 * request/response path (status codes, body capture, request bodies and
 * error handling) without depending on any external service.
 */
class HttpRequestTaskTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/ok", exchange -> respond(exchange, 200,
                "{\"message\":\"hello\"}"));
        server.createContext("/echo", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
            respond(exchange, 200, body);
        });
        server.createContext("/boom", exchange -> respond(exchange, 500, "server error"));

        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange,
                                int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void getStoresStatusAndBody() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        HttpRequestTask task = new HttpRequestTask("call", HttpRequestTask.Method.GET,
                baseUrl + "/ok", "", Map.of(), "resp");
        task.run(ctx);

        assertEquals("200", ctx.get("resp_status"));
        assertEquals("{\"message\":\"hello\"}", ctx.get("resp_body"));
        assertEquals(ctx.get("resp_body"), ctx.get("resp"));
    }

    @Test
    void postSendsBody() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        ctx.put("name", "Mahir");
        HttpRequestTask task = new HttpRequestTask("call", HttpRequestTask.Method.POST,
                baseUrl + "/echo", "{\"who\":\"${name}\"}", Map.of(), "resp");
        task.run(ctx);

        assertEquals("{\"who\":\"Mahir\"}", ctx.get("resp_body"));
    }

    @Test
    void nonSuccessStatusFailsButStillRecordsBody() {
        ExecutionContext ctx = new ExecutionContext();
        HttpRequestTask task = newTask(baseUrl + "/boom");
        assertThrows(TaskExecutionException.class, () -> task.run(ctx));
        assertEquals("500", ctx.get("resp_status"));
        assertTrue(ctx.get("resp_body").contains("server error"));
    }

    @Test
    void unreachableHostFails() {
        ExecutionContext ctx = new ExecutionContext();
        // Port 1 is reserved and not listening, so the connection must fail.
        HttpRequestTask task = newTask("http://127.0.0.1:1/nope");
        assertThrows(TaskExecutionException.class, () -> task.run(ctx));
    }

    private HttpRequestTask newTask(String url) {
        try {
            return new HttpRequestTask("call", HttpRequestTask.Method.GET, url,
                    "", Map.of(), "resp");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
