package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.metadata.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.AssetObject;
import org.chs.camlauncher.backend.metadata.MetadataService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public final class VirtualAssetInstaller {
    private final MetadataService metadata;

    public VirtualAssetInstaller() {
        this(new MetadataService());
    }

    public VirtualAssetInstaller(MetadataService metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public LaunchPlan prepare(LaunchPlan plan) throws IOException {
        Path indexFile = plan.instance().assetIndexFile();
        if (indexFile == null || !Files.isRegularFile(indexFile)) {
            return plan;
        }

        AssetIndexFile index = metadata.readAssetIndex(indexFile);
        if (!index.isVirtual()) {
            return plan;
        }

        String assetId = plan.manifest().assetIndex() != null
                ? plan.manifest().assetIndex().id()
                : plan.manifest().assets();
        if (assetId == null || assetId.isBlank()) {
            assetId = "legacy";
        }

        Path assetsRoot = plan.instance().assetsDirectory();
        Path virtualRoot = assetsRoot.resolve("virtual").resolve(assetId);
        for (Map.Entry<String, AssetObject> entry : index.objects().entrySet()) {
            AssetObject object = entry.getValue();
            if (object.hash() == null || object.hash().length() < 2) {
                continue;
            }

            Path source = assetsRoot
                    .resolve("objects")
                    .resolve(object.hash().substring(0, 2))
                    .resolve(object.hash());
            copy(source, virtualRoot.resolve(entry.getKey()));
            if (index.mapToResources()) {
                copy(source, plan.gameDirectory().resolve("resources").resolve(entry.getKey()));
            }
        }
        return plan.withAssetsDirectory(virtualRoot);
    }

    private static void copy(Path source, Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
