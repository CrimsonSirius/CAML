package org.chs.camlauncher.backend.language;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.language.contract.LanguageEvent;
import org.chs.camlauncher.language.contract.LanguageEventListener;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** 语言事件监听器注册与安全分发。 */
final class LanguageEventPublisher {
    private static final Logger LOGGER = LogManager.getLogger(LanguageEventPublisher.class);
    private final List<LanguageEventListener> listeners = new CopyOnWriteArrayList<>();

    void add(LanguageEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    void remove(LanguageEventListener listener) {
        listeners.remove(listener);
    }

    void publish(LanguageEvent event) {
        for (LanguageEventListener listener : listeners) {
            try {
                listener.onLanguageEvent(event);
            } catch (RuntimeException e) {
                LOGGER.warn("语言事件监听器失败: {}", event, e);
            }
        }
    }
}
