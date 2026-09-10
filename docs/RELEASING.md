# 手动发布指定模组

在 GitHub 网页进入 **Actions → Release selected mod → Run workflow**：

1. 选择源码分支，通常使用 `main`。工作流会固定本次触发的提交来编译。
2. 在 `module` 里选择要发布的模组。
3. 通常不勾选 `draft`、`prerelease`。需要先检查附件时勾选 `draft`，测试版才勾选 `prerelease`。
4. 点击绿色 **Run workflow**。
5. 等待 Build 和 Publish 完成，从运行摘要打开 Release；下载其中的 `.jar` 即可。

工作流只对选中的模组运行 `clean assemble`，不执行完整 GameTest。它直接编译本次选择的源码，不从其他运行中猜测“最新”文件。
上传范围仅为该模组的发布 JAR 和 `SHA256SUMS`；构建任务使用只读权限，发布任务才使用 GitHub 的临时仓库令牌创建 Release，无需手动添加个人令牌。

## 版本与标签

版本来自所选模组 `gradle.properties` 的 `mod_version`，标签格式为 `<模组目录>-v<版本>`。
同一个仓库的多个模组使用不同标签前缀，不会共用一个版本号。已经存在的版本标签不会被覆盖；发布新版本前先更新对应模组的版本并提交。

Release 的源码标签与本次编译提交一致。说明来自该模组 CHANGELOG 中对应版本的内容，并附源码提交与使用说明链接。
GitHub 的 `Latest` 是整个仓库共用的标记，本工作流不自动设置它；查找某个模组时按标签前缀或 Release 标题区分。

勾选 `draft` 的运行会创建草稿。检查满意后，可在 Releases 中编辑该草稿并点击 **Publish release**；不要为公开同一个草稿重复运行同版本构建。

## 新增可发布模组

同时更新以下三处即可接入：

- `.github/workflows/release.yml` 中 `module.options` 的选项。
- `.github/workflows/build.yml` 中 `module.options` 的选项。
- `.github/scripts/prepare_release.py` 中 `MODULES` 的目录、显示名、归档名前缀、mod ID 和 Java 版本。

目前支持的项目使用独立 Gradle Wrapper、`mod_version` 属性和 NeoForge 模组元数据。接入结构不同的项目时，应同步调整打包元数据校验。
