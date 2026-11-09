package org.chs.camlauncher.backend.download.source;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/** 将 Mojang/加载器地址重写到镜像根地址。 */
public final class MirrorDownloadSource implements DownloadSource {
    public static final String BMCLAPI_ROOT = "https://bmclapi2.bangbang93.com";

    private final String root;
    private final List<Rule> rules;

    public MirrorDownloadSource(String root) {
        String value = Objects.requireNonNull(root, "root").trim();
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("镜像根地址不能为空");
        }
        this.root = value;
        this.rules = List.of(
                new Rule("https://bmclapi2.bangbang93.com", this.root),
                new Rule("https://launchermeta.mojang.com", this.root),
                new Rule("https://piston-meta.mojang.com", this.root),
                new Rule("https://piston-data.mojang.com", this.root),
                new Rule("https://launcher.mojang.com", this.root),
                new Rule("https://libraries.minecraft.net", this.root + "/libraries"),
                new Rule("http://files.minecraftforge.net/maven", this.root + "/maven"),
                new Rule("https://files.minecraftforge.net/maven", this.root + "/maven"),
                new Rule("https://maven.minecraftforge.net", this.root + "/maven"),
                new Rule("https://maven.neoforged.net/releases", this.root + "/maven"),
                new Rule("https://maven.neoforged.net", this.root + "/maven"),
                new Rule("https://meta.fabricmc.net", this.root + "/fabric-meta"),
                new Rule("https://resources.download.minecraft.net", this.root + "/assets")
        );
    }

    @Override
    public String name() {
        return "mirror";
    }

    @Override
    public List<URI> candidates(URI original) {
        String value = original.toString();
        for (Rule rule : rules) {
            if (value.startsWith(rule.prefix())) {
                return List.of(URI.create(rule.replacement()
                        + value.substring(rule.prefix().length())));
            }
        }
        return List.of();
    }

    private record Rule(String prefix, String replacement) {
    }
}
