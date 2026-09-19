# Third-party notices

- Original Mek Factory code is distributed under the repository MIT license.
- Original factory texture atlases were created with the built-in ImageGen tool, then mechanically cropped/resized to 16×16. Sources and prompts are preserved in `art/`; no Mekanism texture pixels are copied into the released factory textures.
- Mekanism is an MIT-licensed runtime dependency, copyright (c) 2017–2025 Aidan C. Brady. Its extension APIs and existing texture/model resources are referenced at runtime. Its original texture files and dependency JAR are not bundled in releases. The development-only `art/reference/mekanism-textures.png` sheet enlarges its original induction textures as visual references under the same MIT license. The notice is included at `META-INF/licenses/Mekanism.txt`.
- JEI is optional. Its binaries and sources are not included.
- Modern Industrialization's shared multiblock inventory/change tracking, and the historical GTNH Processing Array's recipe-map/machine selection, were consulted as design references. No MI or GTNH implementation code/assets are copied or bundled. Exact source links are recorded in DESIGN.md.
- The Gradle Wrapper retains its Apache-2.0 notices.
- Temporary reference files and GameTest worlds remain in ignored build directories and are not part of releases.
