package org.chs.camlauncher.backend.metadata;

/** 旧式 Forge library 的 Maven 路径、下载地址和去重键推导。 */
public final class LibraryArtifact {
    public static final String DEFAULT_LIBRARY_URL = "https://libraries.minecraft.net/";

    private LibraryArtifact() {
    }

    public static String coordinateKey(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) {
            return name;
        }
        String classifier = parts.length >= 4 ? stripExtension(parts[3]) : "";
        return parts[0] + ":" + parts[1] + (classifier.isBlank() ? "" : ":" + classifier);
    }

    public static String relativePath(Library library) {
        Library.Downloads downloads = library.downloads();
        if (downloads != null) {
            if (downloads.artifact() == null) {
                return null;
            }
            String path = downloads.artifact().path();
            return path == null || path.isBlank() ? relativePath(library.name()) : path;
        }
        return relativePath(library.name());
    }

    public static String relativePath(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) {
            return null;
        }

        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length >= 4 ? parts[3] : null;
        String extension = "jar";

        int versionExtension = version.indexOf('@');
        if (versionExtension >= 0) {
            extension = version.substring(versionExtension + 1);
            version = version.substring(0, versionExtension);
        }
        if (classifier != null) {
            int classifierExtension = classifier.indexOf('@');
            if (classifierExtension >= 0) {
                extension = classifier.substring(classifierExtension + 1);
                classifier = classifier.substring(0, classifierExtension);
            }
        }

        String fileName = artifact + "-" + version;
        if (classifier != null && !classifier.isBlank()) {
            fileName += "-" + classifier;
        }
        return group + "/" + artifact + "/" + version + "/" + fileName + "." + extension;
    }

    public static String downloadUrl(Library library) {
        Library.Downloads downloads = library.downloads();
        if (downloads != null && downloads.artifact() != null) {
            String url = downloads.artifact().url();
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        String path = relativePath(library);
        if (path == null || path.isBlank()) {
            return null;
        }
        String repository = library.url() == null || library.url().isBlank()
                ? DEFAULT_LIBRARY_URL
                : library.url();
        return repository.endsWith("/") ? repository + path : repository + "/" + path;
    }

    public static String sha1(Library library) {
        Library.Downloads downloads = library.downloads();
        if (downloads != null && downloads.artifact() != null) {
            String sha1 = downloads.artifact().sha1();
            if (sha1 != null && !sha1.isBlank()) {
                return sha1;
            }
        }
        return library.checksums().stream()
                .filter(checksum -> checksum != null && !checksum.isBlank())
                .findFirst()
                .orElseGet(() -> library.sha1() == null || library.sha1().isBlank()
                        ? null
                        : library.sha1());
    }

    public static Long size(Library library) {
        return library.downloads() != null && library.downloads().artifact() != null
                ? library.downloads().artifact().size()
                : library.size();
    }

    private static String stripExtension(String value) {
        int extension = value.indexOf('@');
        return extension >= 0 ? value.substring(0, extension) : value;
    }
}
