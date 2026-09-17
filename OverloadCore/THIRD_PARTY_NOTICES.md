# Third-party notices

Original Overload Core code and generated artwork are distributed under the repository MIT license.

- Mekanism and Mekanism Generators are separate MIT-licensed runtime dependencies, copyright (c) 2017–2025 Aidan C. Brady. The audited default recipe definitions in `overloadcore-bonus-recipes.json` are derived from Mekanism 10.7.19.85 recipe data. The full MIT notice is included at `META-INF/licenses/Mekanism.txt` in the addon JAR.
- Curios is a separate LGPL-3.0 runtime dependency. The addon calls its public accessory APIs and references its existing empty-slot icon at runtime; it does not bundle Curios code or artwork.
- The pendant art is original built-in ImageGen output. Final prompt and mechanical export details are in `art/README.md` and `art/prompt-v2.txt`.
- The Thunder Ward energy-shield sprite is original built-in ImageGen output; its RGBA source and full prompt are kept in `art/source/thunder_ward-energy-shield-v3.png` and `art/thunder-ward-energy-shield-prompt.txt`. Its generated transparency is preserved during nearest-neighbor export. The previous bracelet and stamp artwork remain as design history; the user-approved stamp background-removal script is not used for the current shield. Curios supplies its existing empty-bracelet slot icon at runtime.
- The Gradle Wrapper retains the upstream Apache-2.0 notices in its scripts and JAR.
- The progressive tooltip is independently implemented using Minecraft/NeoForge's tooltip APIs. Its typewriter, scan-highlight and italic red/blue chromatic-echo appearance references **huige233**'s `com.huige233.autism_and_insomnia.client.DreamJournalClientTooltipComponent`, including `styleGlitchRGB`, supplied by the user; that source file is not redistributed and its mod is not a dependency. Author attribution is retained beside both renderer implementations.

No upstream mod JAR, game world, decompiled source tree or large source image is bundled with the mod.
