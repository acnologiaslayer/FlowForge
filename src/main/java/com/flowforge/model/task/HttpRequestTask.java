package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls an HTTP endpoint and stores the response for later steps, mirroring
 * the central role of n8n's HTTP Request node.
 * <p>
 * The URL, request body and header values all support {@code ${variable}}
 * interpolation. The outcome is written back into the {@link ExecutionContext}
 * under three derived variables, so downstream steps (e.g. a
 * {@link JsonExtractTask} or an {@link IfTask}) can react to it:
 * <ul>
 *   <li>{@code <var>_status} - the numeric HTTP status code;</li>
 *   <li>{@code <var>_body}   - the response body text;</li>
 *   <li>{@code <var>}        - alias of the body, for convenience.</li>
 * </ul>
 * A non-2xx status fails the step (fail-fast), just like a real request error.
 */
public class HttpRequestTask extends Task {

    /** The HTTP methods supported by the task. */
    public enum Method {
        GET, POST, PUT, DELETE, PATCH;

        public boolean allowsBody() {
            return this == POST || this == PUT || this == PATCH;
        }
    }

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final Method method;
    private final String url;
    private final String body;
    private final Map<String, String> headers;
    private final String resultVariable;

    public HttpRequestTask(String name, Method method, String url, String body,
                           Map<String, String> headers, String resultVariable)
            throws InvalidTaskConfigurationException {
        super(name);
        if (method == null) {
            throw new InvalidTaskConfigurationException("HTTP method must not be null.");
        }
        if (url == null || url.isBlank()) {
            throw new InvalidTaskConfigurationException("Request URL must not be blank.");
        }
        this.method = method;
        this.url = url.trim();
        this.body = body == null ? "" : body;
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
        this.resultVariable = (resultVariable == null || resultVariable.isBlank())
                ? "response" : resultVariable.trim();
        if (!this.resultVariable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new InvalidTaskConfigurationException(
                    "Invalid result variable '" + this.resultVariable + "'.");
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return new LinkedHashMap<>(headers);
    }

    public String getResultVariable() {
        return resultVariable;
    }

    @Override
    public TaskType getType() {
        return TaskType.HTTP_REQUEST;
    }

    @Override
    protected String execute(ExecutionContext context) throws TaskExecutionException {
        String resolvedUrl = context.interpolate(url);
        HttpRequest request = buildRequest(context, resolvedUrl);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String responseBody = response.body() == null ? "" : response.body();

            context.put(resultVariable + "_status", String.valueOf(status));
            context.put(resultVariable + "_body", responseBody);
            context.put(resultVariable, responseBody);

            if (status < 200 || status >= 300) {
                throw new TaskExecutionException(getName(),
                        "request to " + resolvedUrl + " returned HTTP " + status);
            }
            return method + " " + resolvedUrl + " -> " + status
                    + " (" + responseBody.length() + " bytes)";
        } catch (TaskExecutionException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new TaskExecutionException(getName(),
                    "could not reach " + resolvedUrl + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskExecutionException(getName(),
                    "request to " + resolvedUrl + " was interrupted", e);
        }
    }

    private HttpRequest buildRequest(ExecutionContext context, String resolvedUrl)
            throws TaskExecutionException {
        HttpRequest.Builder builder;
        try {
            builder = HttpRequest.newBuilder().uri(URI.create(resolvedUrl)).timeout(DEFAULT_TIMEOUT);
        } catch (IllegalArgumentException e) {
            throw new TaskExecutionException(getName(), "invalid URL '" + resolvedUrl + "'", e);
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), context.interpolate(header.getValue()));
        }

        HttpRequest.BodyPublisher publisher = method.allowsBody()
                ? HttpRequest.BodyPublishers.ofString(context.interpolate(body))
                : HttpRequest.BodyPublishers.noBody();
        return builder.method(method.name(), publisher).build();
    }

    @Override
    public String summary() {
        return "HTTP " + method + ": " + url;
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("method", method.name());
        fields.put("url", url);
        fields.put("body", body);
        fields.put("resultVariable", resultVariable);
        fields.put("headers", encodeHeaders(headers));
        return fields;
    }

    /** Encodes headers as {@code key:value} pairs separated by newlines. */
    static String encodeHeaders(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return sb.toString();
    }

    /** Parses the {@code key:value} per-line header encoding back into a map. */
    static Map<String, String> decodeHeaders(String encoded) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (encoded == null || encoded.isBlank()) {
            return headers;
        }
        for (String line : encoded.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            headers.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }
        return headers;
    }
}
