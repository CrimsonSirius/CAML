package org.chs.camlauncher.front.task;

import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.front.text.UiText;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/** 跨页面重建保留的下载任务句柄。 */
public final class DownloadTask {
    private final String titleKey;
    private final Object[] titleArgs;
    private final String subject;
    private final DownloadTelemetry telemetry;
    private final DownloadCancellationToken token;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile BooleanSupplier cancelAction;

    public DownloadTask(String titleKey, Object[] titleArgs, String subject,
                        DownloadTelemetry telemetry, DownloadCancellationToken token) {
        this.titleKey = Objects.requireNonNull(titleKey, "titleKey");
        this.titleArgs = titleArgs == null ? new Object[0] : titleArgs.clone();
        this.subject = subject == null ? "" : subject;
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.token = Objects.requireNonNull(token, "token");
    }

    public String title() {
        return UiText.tr(titleKey, titleArgs);
    }

    public String subject() {
        return subject;
    }

    public DownloadTelemetry telemetry() {
        return telemetry;
    }

    public DownloadCancellationToken token() {
        return token;
    }

    public DownloadTelemetry.Snapshot snapshot() {
        return telemetry.snapshot();
    }

    public boolean isRunning() {
        DownloadTelemetry.Snapshot snapshot = snapshot();
        return !snapshot.finished() && !snapshot.failed() && !snapshot.cancelled();
    }

    public void attach(BooleanSupplier cancelAction) {
        this.cancelAction = Objects.requireNonNull(cancelAction, "cancelAction");
    }

    public boolean cancel() {
        BooleanSupplier current = cancelAction;
        boolean cancelled = current != null && current.getAsBoolean();
        token.cancel();
        if (cancelled) {
            telemetry.markCancelled();
            notifyListeners();
        }
        return cancelled;
    }

    public void complete() {
        telemetry.markCompleted();
        notifyListeners();
    }

    public void fail(String message) {
        telemetry.markFailed(message);
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
