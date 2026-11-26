package org.chs.camlauncher.backend.download;

import com.google.gson.Gson;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** 分片下载的检查点、分段进度和续传元数据。 */
final class SegmentResumeState {
    static final long MIN_SEGMENT_SIZE = 4L * 1024 * 1024;
    static final int MAX_SEGMENTS = 4;
    private static final long CHECKPOINT_BYTES = 1024 * 1024;
    private static final long CHECKPOINT_NANOS = 1_000_000_000L;
    private static final Gson GSON = new Gson();

    private long total;
    private String sha1;
    private String etag;
    private String lastModified;
    private List<Segment> segments;

    private SegmentResumeState() {
    }

    static SegmentResumeState create(long total, String sha1, String etag, String lastModified) {
        SegmentResumeState meta = new SegmentResumeState();
        meta.total = total;
        meta.sha1 = sha1;
        meta.etag = etag;
        meta.lastModified = lastModified;
        meta.segments = createSegments(total);
        return meta;
    }

    static SegmentResumeState load(
            DownloadItem item,
            Path part,
            Path metaPath,
            DownloadAttempt.RangeProbe probe
    ) throws IOException {
        if (!Files.isRegularFile(part) || !Files.isRegularFile(metaPath)) {
            return null;
        }
        try {
            SegmentResumeState meta = GSON.fromJson(
                    Files.readString(metaPath), SegmentResumeState.class);
            if (meta == null || meta.total() != probe.totalSize() || meta.segments() == null) {
                return null;
            }
            if (item.sha1() != null && !item.sha1().isBlank()
                    && !item.sha1().equalsIgnoreCase(meta.sha1())) {
                return null;
            }
            if (!validatorsMatch(meta, probe)
                    || Files.size(part) != probe.totalSize()
                    || !meta.matchesPlan()) {
                return null;
            }
            meta.updateValidators(probe.etag(), probe.lastModified());
            meta.markPersisted(System.nanoTime());
            return meta;
        } catch (RuntimeException e) {
            return null;
        }
    }

    long total() {
        return total;
    }

    String sha1() {
        return sha1;
    }

    String etag() {
        return etag;
    }

    String lastModified() {
        return lastModified;
    }

    String ifRange() {
        return etag != null ? etag : lastModified;
    }

    void updateValidators(String etag, String lastModified) {
        this.etag = etag;
        this.lastModified = lastModified;
    }

    List<Segment> segments() {
        return segments;
    }

    long downloadedBytes() {
        return segments.stream().mapToLong(Segment::downloaded).sum();
    }

    boolean checkpointDue(Segment segment, long now) {
        return now - segment.lastPersistNanos() >= CHECKPOINT_NANOS
                || segment.downloaded() - segment.lastPersistBytes() >= CHECKPOINT_BYTES;
    }

    void persist(Path metaPath) throws IOException {
        synchronized (this) {
            Path parent = metaPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = Files.createTempFile(parent, "caml-resume-", ".tmp");
            try {
                Files.writeString(temporary, GSON.toJson(this));
                try {
                    Files.move(temporary, metaPath,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, metaPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    void preallocate(Path part) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(part.toFile(), "rw")) {
            if (file.length() != total) {
                file.setLength(total);
            }
        }
    }

    private boolean matchesPlan() {
        List<Segment> expected = createSegments(total);
        if (segments.size() != expected.size()) {
            return false;
        }
        for (int index = 0; index < segments.size(); index++) {
            Segment actual = segments.get(index);
            Segment wanted = expected.get(index);
            if (actual.start() != wanted.start()
                    || actual.end() != wanted.end()
                    || actual.downloaded() < 0
                    || actual.downloaded() > actual.length()) {
                return false;
            }
        }
        return true;
    }

    void markPersisted(long now) {
        for (Segment segment : segments) {
            segment.markPersisted(now);
        }
    }

    private static boolean validatorsMatch(
            SegmentResumeState meta,
            DownloadAttempt.RangeProbe probe
    ) {
        if (probe.etag() != null) {
            return probe.etag().equals(meta.etag());
        }
        return probe.lastModified() != null
                && probe.lastModified().equals(meta.lastModified());
    }

    private static List<Segment> createSegments(long total) {
        int count = (int) Math.min(MAX_SEGMENTS,
                Math.max(2, (total + MIN_SEGMENT_SIZE - 1) / MIN_SEGMENT_SIZE));
        long length = (total + count - 1) / count;
        List<Segment> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long start = index * length;
            if (start >= total) {
                break;
            }
            long end = Math.min(total - 1, start + length - 1);
            result.add(new Segment(start, end));
        }
        return result;
    }

    static final class Segment {
        private long start;
        private long end;
        private long downloaded;
        private transient long lastPersistNanos;
        private transient long lastPersistBytes;

        private Segment() {
        }

        Segment(long start, long end) {
            this.start = start;
            this.end = end;
            markPersisted(System.nanoTime());
        }

        long start() {
            return start;
        }

        long end() {
            return end;
        }

        long downloaded() {
            return downloaded;
        }

        long length() {
            return end - start + 1;
        }

        long lastPersistNanos() {
            return lastPersistNanos;
        }

        long lastPersistBytes() {
            return lastPersistBytes;
        }

        void addDownloaded(long bytes) {
            downloaded += bytes;
        }

        void complete() {
            downloaded = length();
        }

        void markPersisted(long now) {
            lastPersistNanos = now;
            lastPersistBytes = downloaded;
        }
    }
}
