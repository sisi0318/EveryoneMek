# Third-party notices

EveryoneMek's original code is available under the [MIT License](LICENSE).

- [Nature's Aura](https://github.com/Ellpeck/NaturesAura/blob/main/LICENSE): MIT, copyright (c) 2021 Ellpeck.
- [Mekanism](https://github.com/mekanism/Mekanism/blob/1.21.x/LICENSE): MIT, copyright (c) 2017–2025 Aidan C. Brady.
- [Ars Nouveau](https://github.com/baileyholl/Ars-Nouveau/blob/main/license.txt): LGPL-3.0 code; its assets are all rights reserved unless stated otherwise by the author.
- [Forbidden & Arcanus](https://github.com/stal111/Forbidden-Arcanus/tree/1.21.1): the 2.6.1 release declares All Rights Reserved.
- [Valhelsia Core](https://github.com/ValhelsiaTeam/Valhelsia-Core): the 1.1.4 release declares All Rights Reserved.
- [Gradle Wrapper](https://github.com/gradle/gradle/blob/master/LICENSE): Apache-2.0. Its upstream notices in the wrapper scripts and JAR remain applicable.

Nature's Aura, Ars Nouveau and Mekanism are separate runtime dependencies, and their JARs are not bundled into this repository or the addon JARs. The Nature's Mekanism module item icon refers to textures provided by the installed Nature's Aura and Mekanism packages. Those dependencies and assets retain their own licenses and notices.

Forbidden Mekanism uses Forbidden & Arcanus and Valhelsia Core as separate runtime dependencies. It calls their APIs and uses narrowly scoped state/completion hooks; it does not redistribute their implementations, JARs, models or textures. Its original controller and module art is generated with the built-in image_gen tool; prompts and references are recorded in `Forbidden-Arcanus/art/prompts.json`.

The new block artwork was generated with the built-in image_gen tool using original Mekanism machine textures as the style reference. The reference paths and final prompts are recorded in `NaturesAura/art/prompts.json`. Mekanism's full MIT notice is retained in `NaturesAura/src/main/resources/META-INF/licenses/Mekanism.txt` and packaged with the addon.

Ars Mekanism's industrial machines use generated artwork based on the repository's Nature's Mekanism and Ars Mekanism artwork as style references. Prompts are in `Ars-Nouveau/art/prompts.json` and `Ars-Nouveau/art/expansion-prompts.json`. The FE Sourcelink references the agronomic sourcelink model and texture from the installed Ars Nouveau dependency; it does not bundle those assets. The upstream Ars Nouveau license and Mekanism MIT notice are retained in `Ars-Nouveau/src/main/resources/META-INF/licenses/` and packaged with that addon.

The harvest calculation in `Ars-Nouveau/src/main/java/dev/everyonemek/ars/DrygmyHarvest.java` is adapted from Ars Nouveau's `DrygmyTile` and retains its LGPL-3.0 license. Changes add inventory-based captive jars, processing upgrades and persistent output batches. This file is an exception to the repository's MIT license for original code; its source and the upstream license are included in this repository, and the addon JAR includes the license and attribution notice.
