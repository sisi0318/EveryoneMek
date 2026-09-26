# 五台场域扩展机器

`tools/generate_module_resources.py` 维护五台机器、三种物品、配方与语言资源。全部复用本模组原创 `orb_inner`、`orb_steel`、`shell_panel` 和 `sun_collector` 16×16 PNG，没有新位图或外部素材。各设备用立体轮廓区分：捕获支架、上下锻压头、调谐控制台、物流环及独立观测屏。

模型使用与日冕舱相同的实体并集导出，后覆盖材质拥有重叠区域；不叠加完整共面盒。`ModuleShapes.java` 从同一源实体生成四方向选择／碰撞形状。`tools/preview_modules.cjs` 对真实JSON和像素材质做离线深度预览，不是游戏截图。

`ModuleField` 固定场几何预算：捕获／锻造／物流使用128或48个四边形，调谐／观测屏各1面。`orbital_module.vsh/.fsh` 以顶点属性传机器类型、运行相位、进度／储能比例和亮度，不逐设备修改全局uniform。加法混合、深度测试、禁止透明深度写入；无纹理采样、噪声循环、屏幕扭曲或粒子实体。视效遵循既有FULL／REDUCED／OFF、距离和shader回退配置。

`tools/VerifyModuleShader.java` 读取实际shader及ModuleField，在隐藏OpenGL窗口检查5种模式的链接、运行／停止／移动像素、实体遮挡与深度不写，输出 `build/module-shader-check/`。它不会启动Minecraft客户端。

## alpha.29 频率物流节点

节点由原开放环架改为白灰封闭工业机壳、五面接口识别区与正面内凹投影窗。仍复用原16×16贴图，模型及选择框继续由`generate_module_resources.py`同一份实体盒生成。`orbital-modules-preview.png`是实际静态网格的深度预览，不包含工作投影。

`NodeSnapshot`只收集真实无线搬运事件中的注册ID，不保存另一份货物或物品私有NBT；同时活跃的四类资源每20tick轮换，40tick无记录后过期。`NodeSnapshotShader`使用原物品／流体／化学品图集和原颜色，能量用程序化闪电轮廓，流体用水滴轮廓，化学品用六边轮廓区分；物品为模型材质快照，方块类物品显示表面材质。停止后沿用CoreMotion平滑淡出。

`node_snapshot.vsh/.fsh`每个节点只有1quad，最多1次纹理采样；NEW_ENTITY的UV0为图集坐标、UV1为局部平面坐标、UV2携带位置相位偏移，Normal携带动态相位／资源类型，Color携带资源颜色和强度。没有逐节点uniform修改，不写透明深度，保留遮挡／雾及原特效距离开关；关闭shader时显示原图集的发光材质回退。

`tools/VerifyNodeSnapshotShader.java`在隐藏OpenGL窗口验证实际GLSL150、NEW_ENTITY顶点布局、四类着色、流动、淡出和实体遮挡；输出留在`build/node-snapshot-check`，不进入运行资源。离屏测试以Minecraft铁锭纹理配合分类着色作采样夹具，不能当作完整游戏画面。未启动用户的游戏客户端。

alpha.25：耀斑晶核由FlareCellRenderer绘制，builtin/entity物品共用独立flare_cell.vsh/fsh与现有球体网格，GUI384面／其他96面。原16px金属框由generate_module_resources.py生成flare_cell_frame，flare_cell_fallback保留完整实体。仅两次固定值噪声、0贴图采样，显式按ANIMATE_ITEMS/SHADERS开关处理；VerifySolarShader.java . flare验证实际着色器，不修改现有粒子材质。

## alpha.28 开放式耀斑晶核

用户指出原框架像厚重方盒：上下各10×10像素的盖板和四根立柱遮住了能量球。`tools/flare_cell_mesh.py`改为倾斜的细八边约束环与两个相对磁极，原球面shader、旋转、物品显示变换保持一致。

- 环带径向厚0.6模型像素、轴向厚0.8像素，配两处小型金属磁极及琥珀指示灯。保留白灰／石墨工业色，去掉完整顶盖和底盖。
- 静态框架46个四边形；shader外壳仍用原16×16的`orb_steel`、`orb_inner`和`sun_collector_active`，无新增PNG或逐帧网格构造。GUI合计430面，其余物品场景142面。
- 内环弦中点到中心大于4.8像素，避开旋转能量球的最大半径；磁极也位于球面之外。OBJ坐标、朝外绕序及UV在生成器中检查。
- shader关闭／不可用时，使用同框架与96面球形回退，复用已有`stellar_surface_particle`，共142面；不再退回立方体芯块。

总生成器仍从`generate_module_resources.py`调用新网格生成器，同时维护两个JSON、两个OBJ和一份MTL。`tools/preview_flare_cell.cjs`用真实OBJ、原16px材质和深度缓冲检查正反侧面，输出`flare-cell-restraint-preview.png`；可传入旧框架三角网格JSON作对照。该图展示静态回退与几何，不是游戏截图或运行shader效果。没有新图像生成素材，也没有更改运行贴图。


## alpha.30 整体打磨

五种模型统一16格工业机壳、内凹工作面和接口，继续使用原16×16 PNG；不同功能用内部结构与工作显示区分。捕获／锻造特效缩至0.8倍并移到腔体内，调谐／观测屏位于前面板后方的独立深度位置。所有静态／工作模型均检查共面重叠。

Node投影噪点来自对插值float类别做精确相等比较。倾斜透视下会在闪电和图集采样分支之间随机跳变，原正交测试漏掉了此问题。现改为flat int resourceType；VerifyNodeSnapshotShader用两张不同颜色图集检查12个透视视角下能量分支的输出不受图集影响，旧版首视角32292颜色通道不同，新版为0。

`tools/stellar_materials.py`同时导出StellarMaterialMesh静态顶点、五种物品的OBJ回退和模型。材质种类／表面区域使用flat整数，顶点携带局部坐标、相位和燃料余量，避免同批物品共用错误uniform。金属面保持稳定，压缩物质的深紫纹理、燃料的琥珀流动和合金锭的狭窄能量槽使用独立着色。0纹理采样、固定少量sin计算，无全屏效果或FBO。11/30/132面，GROUND或REDUCED将圆柱降为4段，最高72面。

VerifyStellarMaterials使用真实生成网格和GLSL检查五种形态、动态、遮挡、低面数及残余胶囊变暗。`stellar-materials-preview.png`由实际隐藏GL渲染输出组成，运行时贴图没有修改；不是游戏截图。GUI、双手、展示框、掉落物共用StellarMaterialRenderer，动画和shader遵循原客户端开关。
