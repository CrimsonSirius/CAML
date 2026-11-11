package org.chs.camlauncher.backend.language;

import org.chs.camlauncher.language.contract.LanguageConstants;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 将内置语言资源同步到数据目录。 */
final class BuiltinLanguageInstaller {
    private final Path languageDirectory;

    BuiltinLanguageInstaller(Path languageDirectory) {
        this.languageDirectory = languageDirectory;
    }

    List<Repair> install() throws IOException {
        List<Repair> repairs = new ArrayList<>();
        for (String locale : LanguageConstants.BUNDLED_LOCALES) {
            byte[] bundled = BundledLanguageResources.read(locale);
            Map<String, String> bundledCatalog = LanguageCatalogParser.parse(
                    bundled, locale + ".json");
            Path installed = languageDirectory.resolve(locale + ".json");
            Repair repair = repairReason(locale, installed, bundledCatalog);
            if (repair == null) {
                continue;
            }
            writeAtomically(installed, bundled);
            repairs.add(repair);
        }
        return List.copyOf(repairs);
    }

    private Repair repairReason(String locale, Path installed, Map<String, String> bundledCatalog) {
        if (!Files.isRegularFile(installed)) {
            return new Repair(locale, installed.getFileName().toString(), "missing", null);
        }
        try {
            Map<String, String> installedCatalog = LanguageCatalogParser.parse(
                    Files.readAllBytes(installed), installed.getFileName().toString());
            return installedCatalog.equals(bundledCatalog)
                    ? null
                    : new Repair(locale, installed.getFileName().toString(), "content-differs", null);
        } catch (IOException | RuntimeException e) {
            return new Repair(locale, installed.getFileName().toString(), "invalid-json", e);
        }
    }

    private void writeAtomically(Path target, byte[] content) throws IOException {
        Path temporary = Files.createTempFile(languageDirectory, "caml-lang-", ".tmp");
        try {
            Files.write(temporary, content);
            LanguageCatalogParser.parse(Files.readAllBytes(temporary), temporary.getFileName().toString());
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    record Repair(String locale, String fileName, String reason, Throwable cause) {
    }
}
