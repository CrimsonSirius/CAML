package org.chs.camlauncher.backend.mod.source;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Mod API 的同步 JSON 客户端；不使用启动器镜像源。 */
final class ModApiHttpClient {
    private static final String USER_AGENT = "CAML/1.0";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 250L;
    private final HttpClient httpClient;

    ModApiHttpClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    JsonElement get(URI uri, Map<String, String> headers) throws IOException, InterruptedException {
        return send(request(uri, headers).GET().build());
    }

    JsonElement post(URI uri, String body, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = request(uri, headers)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return send(builder.build());
    }

    private HttpRequest.Builder request(URI uri, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json");
        headers.forEach(builder::header);
        return builder;
    }

    private JsonElement send(HttpRequest request) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                boolean retryableStatus = status == 429 || status >= 500;
                if (retryableStatus && attempt < MAX_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MILLIS * attempt);
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new BackendException(BackendError.of(
                            "network.httpError", status, request.uri()));
                }
                try {
                    return JsonParser.parseString(response.body());
                } catch (RuntimeException e) {
                    throw new BackendException(BackendError.of(
                            "network.responseInvalid", request.uri()), e);
                }
            } catch (BackendException e) {
                throw e;
            } catch (IOException e) {
                lastFailure = e;
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                Thread.sleep(RETRY_DELAY_MILLIS * attempt);
            }
        }
        throw lastFailure == null
                ? new IOException("Mod API request failed: " + request.uri())
                : lastFailure;
    }
}
