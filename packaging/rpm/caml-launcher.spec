%global debug_package %{nil}
%global __os_install_post %{nil}
%define _binary_payload w2.xzdio

Name:           caml-launcher
Version:        @VERSION@
Release:        @RELEASE@
Summary:        CAML Minecraft Launcher
License:        GPL-3.0-only
URL:            https://github.com/CrimsonSirius/CAML
Vendor:         CAML Contributors
Packager:       CrimsonSirius <227993016@qq.com>
BuildArch:      x86_64
AutoReqProv:    no
Group:          Amusements/Games

Requires:       glibc
Requires:       gtk3
Requires:       libX11
Requires:       libXext
Requires:       libXrender
Requires:       libXtst
Requires:       libXi
Requires:       libXxf86vm
Requires:       mesa-libGL
Requires:       fontconfig
Requires:       freetype
Requires:       alsa-lib

%description
CAML is a JavaFX-based Minecraft launcher. The package contains its own
Java runtime and does not require a system Java installation.

%install
mkdir -p %{buildroot}
cp -a %{_sourcedir}/root/. %{buildroot}/

%post
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database -q /usr/share/applications || :
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -q -t /usr/share/icons/hicolor || :
fi

%postun
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database -q /usr/share/applications || :
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -q -t /usr/share/icons/hicolor || :
fi

%files
/opt/caml-launcher
%attr(0755,root,root) /usr/bin/caml
/usr/share/applications/caml.desktop
/usr/share/icons/hicolor/256x256/apps/caml.png
/usr/share/icons/hicolor/512x512/apps/caml.png
/usr/share/doc/caml-launcher
/usr/share/licenses/caml-launcher

%changelog
* Mon Nov 16 2026 CrimsonSirius <227993016@qq.com> - @VERSION@-@RELEASE@
- Initial CAML Linux package.
