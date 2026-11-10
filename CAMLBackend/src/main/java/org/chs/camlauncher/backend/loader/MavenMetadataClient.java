package org.chs.camlauncher.backend.loader;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.SourceAwareHttpClient;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/** 读取 Maven maven-metadata.xml 中的版本列表。 */
public final class MavenMetadataClient {
    private final SourceAwareHttpClient httpClient;

    public MavenMetadataClient() {
        this(defaultClient());
    }

    public MavenMetadataClient(HttpClient httpClient) {
        this.httpClient = new SourceAwareHttpClient(Objects.requireNonNull(httpClient, "httpClient"));
    }

    public void setSources(List<DownloadSource> sources) {
        httpClient.setSources(sources);
    }

    public List<String> fetchVersions(URI uri) throws IOException, InterruptedException {
        return fetchVersions(uri, versions -> !versions.isEmpty());
    }

    /** 只有满足 usable 的版本列表才会被采用，否则继续尝试下一个候选源。 */
    public List<String> fetchVersions(URI uri, Predicate<List<String>> usable)
            throws IOException, InterruptedException {
        Objects.requireNonNull(usable, "usable");
        return httpClient.getParsedBytes(
                uri,
                Duration.ofSeconds(30),
                "application/xml,text/xml,*/*",
                body -> {
                    List<String> versions = parseVersions(uri, body);
                    return usable.test(versions) ? Optional.of(versions) : Optional.empty();
                }
        ).orElse(List.of());
    }

    private static List<String> parseVersions(URI uri, byte[] body) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(body)
            );
            NodeList nodes = document.getElementsByTagName("version");
            List<String> versions = new ArrayList<>(nodes.getLength());
            for (int index = 0; index < nodes.getLength(); index++) {
                String value = nodes.item(index).getTextContent();
                if (value != null && !value.isBlank()) {
                    versions.add(value.trim());
                }
            }
            return List.copyOf(versions);
        } catch (Exception e) {
            throw new IOException("无法解析 Maven 元数据: " + uri, e);
        }
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
}
