# 五台场域扩展机器

`tools/generate_module_resources.py` 维护五台机器、三种物品、配方与语言资源。全部复用本模组原创 `orb_inner`、`orb_steel`、`shell_panel` 和 `sun_collector` 16×16 PNG，没有新位图或外部素材。各设备用立体轮廓区分：捕获支架、上下锻压头、调谐控制台、物流环及独立观测屏。

模型使用与日冕舱相同的实体并集导出，后覆盖材质拥有重叠区域；不叠加完整共面盒。`ModuleShapes.java` 从同一源实体生成四方向选择／碰撞形状。`tools/preview_modules.cjs` 对真实JSON和像素材质做离线深度预览，不是游戏截图。

`ModuleField` 固定场几何预算：捕获／锻造／物流使用128或48个四边形，调谐／观测屏各1面。`orbital_module.vsh/.fsh` 以顶点属性传机器类型、运行相位、进度／储能比例和亮度，不逐设备修改全局uniform。加法混合、深度测试、禁止透明深度写入；无纹理采样、噪声循环、屏幕扭曲或粒子实体。视效遵循既有FULL／REDUCED／OFF、距离和shader回退配置。

`tools/VerifyModuleShader.java` 读取实际shader及ModuleField，在隐藏OpenGL窗口检查5种模式的链接、运行／停止／移动像素、实体遮挡与深度不写，输出 `build/module-shader-check/`。它不会启动Minecraft客户端。

alpha.25：耀斑晶核由FlareCellRenderer绘制，builtin/entity物品共用独立flare_cell.vsh/fsh与现有球体网格，GUI384面／其他96面。原16px金属框由generate_module_resources.py生成flare_cell_frame，flare_cell_fallback保留完整实体。仅两次固定值噪声、0贴图采样，显式按ANIMATE_ITEMS/SHADERS开关处理；VerifySolarShader.java . flare验证实际着色器，不修改现有粒子材质。

## alpha.28 开放式耀斑晶核

用户指出原框架像厚重方盒：上下各10×10像素的盖板和四根立柱遮住了能量球。`tools/flare_cell_mesh.py`改为倾斜的细八边约束环与两个相对磁极，原球面shader、旋转、物品显示变换保持一致。

- 环带径向厚0.6模型像素、轴向厚0.8像素，配两处小型金属磁极及琥珀指示灯。保留白灰／石墨工业色，去掉完整顶盖和底盖。
- 静态框架46个四边形；shader外壳仍用原16×16的`orb_steel`、`orb_inner`和`sun_collector_active`，无新增PNG或逐帧网格构造。GUI合计430面，其余物品场景142面。
- 内环弦中点到中心大于4.8像素，避开旋转能量球的最大半径；磁极也位于球面之外。OBJ坐标、朝外绕序及UV在生成器中检查。
- shader关闭／不可用时，使用同框架与96面球形回退，复用已有`stellar_surface_particle`，共142面；不再退回立方体芯块。

总生成器仍从`generate_module_resources.py`调用新网格生成器，同时维护两个JSON、两个OBJ和一份MTL。`tools/preview_flare_cell.cjs`用真实OBJ、原16px材质和深度缓冲检查正反侧面，输出`flare-cell-restraint-preview.png`；可传入旧框架三角网格JSON作对照。该图展示静态回退与几何，不是游戏截图或运行shader效果。没有新图像生成素材，也没有更改运行贴图。
