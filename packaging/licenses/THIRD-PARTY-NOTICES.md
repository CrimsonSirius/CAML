# CAML Third-Party Notices

CAML itself is licensed under GPL-3.0-only. The release packages also contain
the following third-party components:

| Component | Version | License |
|---|---|---|
| OpenJDK runtime (Eclipse Temurin on Linux, Azul Zulu on Windows) | 25 | GPL-2.0 with Classpath Exception |
| OpenJFX (JavaFX) | 25.0.3 | GPL-2.0 with Classpath Exception |
| Gson | 2.15.0 | Apache-2.0 |
| Log4j API and Log4j Core | 2.26.1 | Apache-2.0 |
| TwelveMonkeys ImageIO (WebP and dependencies) | 3.15.3 | BSD-3-Clause |
| Error Prone annotations | 2.50.0 | Apache-2.0 |

Both JDK runtime builds are OpenJDK derivatives under the same
GPL-2.0-with-Classpath-Exception license; only the vendor differs
(Eclipse Adoptium for Linux packages, Azul Systems for Windows packages).

The packaged JDK runtime contains additional module notices in the `JDK/`
directory (`runtime/legal/` in the Windows build). The full Apache-2.0 and
BSD-3-Clause texts are installed alongside this notice. Log4j's NOTICE text is
installed as `Log4j-NOTICE.txt`. The OpenJFX license is installed as
`OpenJFX-GPL-2.0-CE.txt`.

Project source: https://github.com/CrimsonSirius/CAML