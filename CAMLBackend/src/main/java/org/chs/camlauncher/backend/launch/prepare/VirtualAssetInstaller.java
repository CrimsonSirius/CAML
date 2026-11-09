package org.chs.camlauncher.backend.launch.prepare;

import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.metadata.model.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.model.AssetObject;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.io.SafePaths;

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
        Path virtualRoot = SafePaths.resolveChild(assetsRoot.resolve("virtual"), assetId, "asset id");
        for (Map.Entry<String, AssetObject> entry : index.objects().entrySet()) {
            AssetObject object = entry.getValue();
            if (object.hash() == null || object.hash().length() < 2) {
                continue;
            }

            Path source = SafePaths.resolveUnder(
                    assetsRoot.resolve("objects"),
                    object.hash().substring(0, 2) + "/" + object.hash(),
                    "asset object");
            copy(source, SafePaths.resolveUnder(virtualRoot, entry.getKey(), "virtual asset"));
            if (index.mapToResources()) {
                copy(source, SafePaths.resolveUnder(
                        plan.gameDirectory().resolve("resources"), entry.getKey(), "legacy asset"));
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
