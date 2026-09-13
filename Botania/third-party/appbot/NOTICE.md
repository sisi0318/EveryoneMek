# Applied Botanics compatibility build

Upstream: https://github.com/ramidzkh/Applied-Botanics/tree/10201733d07f1e2a20ef69b816a67f633d9d6ffc
Release: https://github.com/ramidzkh/Applied-Botanics/releases/tag/1.6.0-alpha.3
Author: ramidzkh and the Applied Botanics contributors.

Upstream code is LGPL-3.0. Upstream assets are CC BY-NC-SA 3.0, as stated in the upstream README.
The compatibility JAR is a modified build for the Botania snapshot pinned by this project, not an unmodified official release.

The complete changes are in Botania/appbot-compat.json and Botania/tools/prepare_appbot.py:
two API class-name relocations, two renamed sound fields, one renamed recipe ingredient,
and a compatibility version suffix. Original bytecode instructions and textures are retained.
The same script fetches and verifies the original release, then reproduces the compatibility build.

This third-party JAR and its assets are not covered by EveryoneMek's MIT license.
