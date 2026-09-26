# MekGravity 接手入口

先读根AGENTS.md，当前行为以README与代码为准。禁止runClient，客户端验收由用户完成。

## 基线与当前要求

- 0.1.0-alpha.27，ID mekgravity，包dev.everyonemek.gravity；MC1.21.1、NeoForge21.1.241、Mek/Mek Generators10.7.19.85、Java21。原7格引力堆与新增9格微缩太阳共存。
- 固定7×7×7。主控在(3,1,0)，核心(3,3,3)，六线圈沿轴距核心2格，中间留空。最低线圈等级决定发电。
- alpha.2用户明确取消强制钠冷却，要求连接玻璃、修复UI、提高向外输电、成型专用材质、核心特效与更高发电效率。不能再把冷却口作为成型条件。
- 默认毛功率2/4/8/16 GFE/t，alpha.6单丸能值24 TJ（alpha.5的120倍），默认100%稳态续航240/120/60/30秒；单口16 GFE/t，默认四输出口合计64 GFE/t。未用燃料丸按新配方，已付费反应余量保留J值。数字按默认FE/J和20TPS。
- 2026-09-23用户说“UI没变”实际指机器外观；只读检查测试目录和启动日志确认仍加载alpha.4。alpha.5才接入整机几何；此类反馈先核对实际JAR/日志，不擅改操作界面或重复重做模型。更新需退出游戏替换旧JAR并重启，不能仅F3+T。
- 白灰Mek工业机壳为本模组已确认风格。alpha.4用户指出条纹重复、核心像小机器，确认悬浮深紫能量核与少量金属环；框架和外壳用shell-v2图稿的简洁白灰面，成型仅紫灰角标。不能恢复黑紫长条纹或方形核心外壳。
- 燃料丸保留alpha.3的独立透明item/generated图标，不借用机器纹理。新核心/外壳原稿在art/source/orb.png及shell-v2.png，提示词见art/orb-and-shell-prompts.json。

## 实现入口

- alpha.27范围设置：ModuleConfig.MIN_RANGE/MAX_RANGE为16/512，原linkRange默认128不迁移。面板个人range与全服linkRange分开同步；Action追加value，8改扫描，9改连接，registrar协议2。个人范围存Player.PERSISTED_NBT_TAG内mekgravity_panel_range，保留其他持久标签，首次缺省跟随linkRange。
- 修改扫描只改变eligible及画布范围，不改source/peer；每20tick扫描限流不因设置或刷新请求重置，缩小时快照先过滤、扩张最多1秒发现。范围变化递增revision，拒绝旧拖线。连接设置每次核对hasPermissions(2)或isSingleplayerOwner，RANGE.set后RANGE.save触发原配置保存/重载；客户端只显示Snapshot确认值，普通用户无权修改全服值。FieldLink.inRange仍是绑定和实际物流的共同范围依据。
- RangeSettings使用原GuiWindow/GuiTextField回车与勾号，确认值与输入草稿分开，设置错误在窗口内显示。窗口打开时画布鼠标、滚轮和Delete让回父类，避免操作窗口时拖线或删链接。
- 当前NeoForge/FML的SERVER配置默认位于实例config/，已有存档serverconfig/同名文件才覆盖；RANGE.save保存实际加载的文件，不假设始终在存档目录。已核对ServerLifecycleHooks与ConfigTracker.resolveBasePath。alpha.27共49项GameTest通过；范围测试使用真实ServerOpListEntry临时授予2级OP，finally还原配置/OP，覆盖200格实际绑定、降低/恢复距离、扫描独立及保存、畸形数值和权限收回。未运行客户端。

- alpha.26 场域面板：链接器普通空气右键、ModuleMenu动作7打开LinkPanelMenu，客户端LinkPanelScreen复用GuiMekanism/GuiTextField/MekanismButton，无物品槽。金线core→module、蓝线node→node；拖线与两次点击共用Action，底部四通道与解绑改变同一OrbitalModule数据。
- LinkPanelMenu以开面板位置或host为中心，每20tick遍历ServerChunkCache.getChunkNow的已加载BE表，仅Controller/SolarController/OrbitalModule且Mek可访问；最多最近192台，不扫描方块体积、不加区块票。身份列表变化才递增revision，普通状态刷新不打断拖线。工具需继续持有且在anchor的8格内；host需同BE且安全许可。
- LinkPanelNetwork使用两个有界StreamCodec数据包，客户端Consumer仅在SolarClient注册，不在服务端引用Screen。Action核对当前菜单、随机session、revision、BE身份和权限，每面板每tick最多4次；解绑选线附旧端点避免错删。FieldConnections复用工具和面板的pair-range/type/player及模块owner权限检查，跨当前linkRange的两个可见端点仍不能连接。
- LinkPanelGeometry提供原生画布布局/曲线命中数学；wire四边形顶点绕序须匹配GuiGraphics.fill，避免RenderType.gui剔除连线。背景绘制用绝对屏幕坐标/scissor，标题与按钮用原Mek局部坐标；初始化先禁用无选择的资源按钮。

- alpha.25 NODE通过懒创建NodeStorage扩展原生energy/fluids/chemicals；字段不能初始化覆盖父构造getInitial方法创建的数据。收发分离：J各2.56PJ，流体4+4×16MmB，化学4+4×64M，侧/背/上下入、正面出；4通道mask保存默认15，旧18物品槽和绑定不改。held item必须同时注册对应容器creators，loot复制mekanism:energy/fluids/chemicals，不能只保存orbital_module。
- 化学罐与物品creators用ChemicalAttributeValidator.ALWAYS_ALLOW。原TileEntityMekanism.collectChemicalTanks会在shouldDumpRadiation为true时过滤放射性货物；仅NODE覆写为false保持密封，真实核废料掉落/重放回归已过。其余机器不改默认辐射行为。
- NodeStorage.transfer直接从输入到对方输出，先匹配容量与能耗再原子提交；流体按组件匹配，化学品按原类型，能量用long。发送端按gameTime%4轮换资源优先级。传输电费来自绑定源，能量货物另存；默认物品1MJ/个、流体/化学品1kJ/单位、能量每次1MJ。自动eject优先原生long，FE回退使用ForgeEnergyIntegration模拟对齐且64次有界循环，外部容量/速度仍生效。原FE桥canReceive/canExtract总返回true，方向测试要调用实际insert/extract，不能仅检查这两个声明。
- FieldLinker正常点击能源→发送→接收，工具分别保存power与sender；远处配对只要求玩家在接收端附近，但每次重新检查两端权限/同维度/128格/已加载。旧flat dimension/pos/node标签和潜行接收端优先流程继续支持；空气潜行右键仅清选择。receiverConfigured保存配对记录，无源无目标的节点默认显示接收就绪；sourceBound/peerBound菜单布尔值消除未绑定时显示0,0,0。动作5/6分别解除目标/能源，20～23切换通道。
- ModuleScreen资源窗用原GuiFluidGauge/GuiChemicalGauge，tanks supplier必须给getFluidTanks(null)/getChemicalTanks(null)完整8罐，不能传4罐子集，否则出侧GaugeDropper索引会误指入侧。GaugeType.STANDARD按18×60布局，原生父类同步真实容器；传输开关只控制无线通道，取出已有输出仍可进行。
- FlareCellRenderer通过RegisterClientExtensionsEvent注册FLARE，item模型builtin/entity；FRAME/FALLBACK在ModelEvent注册，资源重载后从ModelManager获取。SolarShader追加独立flare_cell材质，复用SolarSphereMesh，GUI384quads、其他96；金属框复用16px原素材。VerifySolarShader新增flare参数，仅输出build预览，不能覆盖原恒星/引力粒子贴图。

- alpha.24用户要求GUI不出现“本批已付”等内部记账措辞：显示加工耗能／传输耗能，太阳汇总为模块耗能（每tick），持久化paid键不改。
- alpha.24用户要求压缩恒星物质只用耀斑晶核：orbital/stellar_matter仍为5GJ/40基础tick，但输入改为单个flare_cell，不改原工作台配方。OrbitalModule.start按输入组数降序、recipe ID确定顺序；完整复杂配方即使受能量/空间/等级限制也不回退到组数更少的配方，避免新单输入路线抢走合金用晶核。旧预付快照不重算。

- alpha.23 expansion包新增CAPTOR/FORGE/TUNER/NODE/OBSERVATORY五种独立ModuleBlock/OrbitalModule，ModuleContent用同菜单、按种类独立BE注册；kind从已注册BlockState读取，父构造getInitialInventory不依赖子字段。cargo9进9出复用CoronalInventorySlot；TUNER仅1个原生flare输入槽；OBS无槽且移除Attributes.AttributeRedstone，只提供正面红石输出与比较器。
- FieldLinker监听RightClickBlock并调用物品useOn，避免Mek方块先开GUI吞掉工具操作；潜行记录源或接收节点，普通右键模块绑定。FieldLink保存维度/位置，同维度默认128格，检查hasChunkAt和源/目标权限；Node接收端由发送端付款，无需自己连源。绑定不复制库存或能量，不强加载。父类原生GUI/安全/红石和掉落mekanism:items分别保留。
- OrbitalRecipe使用orbital_processing，machine仅flare_captor/gravity_forge，1～4输入/结果≤64/ticks≤72000/energy≤100TJ/tier0～3。记录已付输出和进度进orbital_module组件，库存仅mekanism:items。Node本机输入到远端输出，每tick4096，1MJ/物品，双端仅真实slot事务；拒绝未加载/暂停/无权限/满库存。
- CoreTuning在两种主控中单份保存profile/burst/cooldown，平衡默认0不改旧行为；聚焦80%功率、加工×2，超频125%功率/15%燃料附加，耀斑150%/30%附加，400tick持续与1600tick复用计时仅加载tick推进。gross为电能，cost(gross)实际扣燃料；affordable先按剩余燃料反算gross，末端不足支付1gross时耗尽为等量selfUse避免卡1J。主控DATA/solar_data掉落同样保存调谐，菜单同步3字段，太阳续航按当前调谐成本估算。
- generate_module_resources.py由总资源生成器调用；outer_surface复用去共面面，ModuleShapes由同实体盒生成。ModuleField/ModuleShader/ModuleRenderer与orbital_module.vsh/fsh不改逐实例uniform，顶点编码类型/相位/进度；调谐和观测屏1quad，其他128/48quads，0纹理采样。VerifyModuleShader用真实几何与shader检查5种模式；preview_modules为静态原生模型预览，非游戏截图。

- alpha.22日冕白侧板与壳体共面导致视角移动时斜纹闪烁。generate_coronal_resources.outer_surface调用runtime_geometry.exposed_faces对实体求并集，材质由后加入盒子覆盖，按原面方向裁切UV，不能再把完整六面白轨条直接叠到壳体上。原solid盒仍用于CoronalShapes，轮廓/碰撞不变；灯光额外数据复制到新表面。两模型102→86quads，原57组共面重叠归零，tools/verify_coronal_geometry.py同时检查模块内部和9块翼片接口；旧模型明确失败，新模型通过。纯几何修改仅检查资源、离线预览和构建，不重复35项服务端测试。

- alpha.21日冕吞吐默认512批、额外acceleration5、普通40基础ticks→四级8/4/2/1tick。太阳react自动调载必须把lastProcessing与lastOutput一起响应，否则突发加工用电会因从零缓慢爬升而不能及时补回缓存。SolarConfig.Loading以coronalPerformanceRevision一次迁移原32批/5GJ默认值，保留自定义；旧coronal_work的duration/progress/paid不重算，批量/余量/paid上限分别扩至512/32768/512TJ。
- CoronalInventorySlot用BasicInventorySlot的4096上限、原生oversized NBT及AttachedItems组件；世界与物品附件保持18槽原顺序并同上限。普通64堆叠存4096、16堆叠存1024、非堆叠仍1。MANUAL/EXTERNAL沿用原extractItem的物品堆叠上限，INTERNAL弹出覆盖该限制以批量转移；否则产物合并到单槽后自动出料会卡64/t。空ItemStack查询返回4096。菜单原生提取/合并/shift及真实掉落ItemStack序列化重放已验证。
- 配方maxBatch二分最大流，先按可支付能量和产物空间限界，不按512逐项循环；onContentsChanged清nextAttempt响应补货，未改变库存的空闲仍5tick重试。完成后保留最近batch/progress/paid显示；特效仅启停与4tick周期更新，不逐批force发包。
- CoronalBlock/生成CoronalShapes匹配薄型翼片接合舱；同一generate_coronal_resources.py生成模型/碰撞，背部接合板延伸至本地z24的真实翼片背面。复用orb_inner为主体、窄shell_panel轨条和sun_collector金色灯，Renderer中心local z=.63，物品GUI缩放和破坏粒子同步。

- alpha.20日冕加工舱位于solar布局(-1,4,4)/(9,4,4)/(4,4,-1)/(4,4,9)，背面对collector的segment4且朝内相同；不加入旧220块结构。CoronalMachine原生Mek9进9出，只存一套mekanism:items，coronal_work仅保存已付费产物/进度/设置。SolarController在SolarPorts.eject后派发四舱，每舱每tick最多推进一次，spendForCorona直接扣共享储电且保留reserve，不计入外部exported；lastProcessing为上一tick批次总扣能。
- CoronalRecipe的coronal_processing支持1～4输入、结果1～64、基础ticks≤72000、energy≤10^12 J、tier0～3；最大流分配处理标签重叠和跨槽原料。配方匹配空闲最多每5tick，已付批次不重新读取配方。finish先合并后占空槽，堵塞保留remaining；掉落loot必须同时复制mekanism:items及mekgravity:coronal_work。原生ItemSlotsBuilder能力与服务端slots数量一致。
- 原生BlockTile菜单必须用MENUS.register(id,CoronalMachine.class,CoronalMenu::new)创建MekanismContainerType；纯registerMenu+IMenuTypeExtension不能被Mek.getProvider识别，会右键无GUI。已由真实空手右键测试验证。当前Mek富集精炼黑曜石物品ID为enriched_refined_obsidian，不是enriched_obsidian。
- 日冕资源由generate_coronal_resources.py维护并由总生成器调用，复用原16px材料。CoronalRenderer/CoronalField/CoronalShader及coronal_processing.vsh/fsh为悬浮物料和双热环；固定128/48quads，以顶点属性携带各舱相位/进度，无全屏效果、不写透明深度。新增JEI分类只放专用高温配方，普通配方添加原SMELTING/BLASTING催化机器。

- alpha.19 Construction/SolarConstruction使用持久化buildTier(0～3，默认0)，菜单20循环等级、21整组升级。搭建缺料跳过可继续补齐，现有高级同类不降级。升级仅替换低级线圈／太阳tiered部件，需要备齐新部件；AssemblyBuild用BreakEvent及EventHooks.onBlockPlace保护，BlockSnapshot恢复拒绝的位置；旧BE NBT迁移、新件支付、旧件退回在恢复真实主手后执行，避免退料合入临时手持栈。满背包退料掉在玩家旁；已完成部件不回滚，下次可续做。
- 自动出电用outputCursor轮换输出能量口的优先级，不承诺同tick严格等分，不影响外部主动抽能顺序。Part/SolarPart的上一tick累计值仅用于端口读数，2tick无活动归零，不进NBT。两菜单30/31翻页查看端口，选择索引服务器限幅；capability额度仍由原Energy实现掌管。
- 引力堆Structure新增独立完整343格WATCHERS，与遇首错终止的OWNERS区分，onLoad/区块load/unload维护；移除20tick轮询。末端按net=gross-ceil(gross/50)、gross=net+ceil(net/49)计算，使用分段整数避免溢出；不清理旧能量或燃料。
- VisualConfig为CLIENT、mekgravity-client.toml。FULL／REDUCED／OFF只改变显示，animateItems独立，shaderMaterials=false回退原模型；GravityClient注册原ConfigurationScreen。SolarField emit新增reduced参数，旧重载仍FULL；430/76段，Shader路径按标量计算相机朝向四边形，复用一个Vector3f，不再为每段创建Vec3数组。
- ReactorMenu.stillValid与SolarMenu统一按实际点击部件距离/安全权限检查，移除Mek父类额外按主控距离关闭远端框架GUI的限制。新版AssemblyUpgradeTests顺带覆盖最远角打开后保持菜单。
- FuelStock只读统计仓内配方份数和剩余能量，乘加饱和为Long.MAX_VALUE时UI带≥，不把摘要回写库存。SolarMenu同步coreHot，面板把full且热态显示高温待机，停机显示已熄火；未成型备用份数=-1。保留原单份剩余燃料条，太阳结构页显示约束/采能瓶颈。

- 2026-09-25用户要求逐项优化直到完成：第1项燃料保存已于alpha.18完成；其余五项于alpha.19完成，下列为当前契约。
- FuelInventorySlot用于FuelMenu及SolarFuelMenu：原SlotItemHandler.setChanged继承Slot，只通知其emptyInventory，原生moveItemStackTo合并/部分提取直接修改ItemStack时不会进入ItemStackHandler.onContentsChanged。覆写槽位setChanged后在服务端调用实际BlockEntity.setChanged，不只在quickMoveStack末尾补救；普通右键/合并也经过同一保存通知。保持原库存、槽位索引、过滤和NBT格式。

- alpha.17 GravityCoreItemRenderer通过原RegisterClientExtensionsEvent注册core，模型builtin/entity；CoreRenderer.drawCore由世界和物品共用，接收BlockState而不创建虚拟Part。GUI距离参数6经drawGravity除.3后使用384面球体，加双环192面，所有显示场景保留双环反向转动，物品不画六向世界光束。静态OBJ仍供shader失败回退。
- core及其所有静态子模型particle统一指向gravity_surface_particle.png，生成器generate_resources.py同时维护物品display和粒子引用。VerifySolarShader.java带gravity参数会直接用gravity_surface.fsh烘焙16×16粒子图；原stellar路径仍正常。总运行PNG41张，没有改已有40张。实际世界发电和库存逻辑未变。

- 资源总生成器generate_resources.py也必须写入client=[SolarWarningMixin]，不能只修改已生成mixins.json；否则下一次美术资源生成会撤掉用户的色散警告。
- alpha.16双引力环由GravityRingShader使用gravity_ring.vsh/.fsh绘制，GravityRingMesh.java由core_mesh.py从与原OBJ相同的顶点/UV提取生成，不手改；9float布局pos3/normal3/u/v/inner。两环96+96quads，UV0携带u和各自flow相位，Color携带负载/内表面/v；默认原模型仍供物品及shader失败回退。
- SolarFieldShader用POSITION_TEX_COLOR与stellar_field.vsh/.fsh，一段光带一个quad，横坐标±1或±3做柔边，纵向dot(position)-phase驱动明暗；同原轨迹430quads（旧双层640），加法混合、LEQUAL、COLOR_WRITE、不写深度，避免透明外晕遮掉后面的亮线。注册沿用SolarClient的客户端shader事件。
- SolarSeedItemRenderer通过net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent绑定solar_seed，模型parent=builtin/entity，手持/GUI/展示框/掉落物走同SolarShader，不复制虚拟BE或游戏资源。先撤销原ItemRenderer的-.5中心偏移再缩放.34，GUI使用中等384面，其余近距864面；资源重载后fallback模型重新从ModelManager获取。
- solar_seed命中/破坏粒子由sun_idle/active模型particle指向stellar_surface_particle.png；VerifySolarShader在16×16视口用真实stellar_surface shader渲染正面区块直接导出，无背景/后续重绘。JSON引用由generate_solar_resources.py维护，原39张贴图保留，总40张。修改球面shader色彩时按art/field-shaders.md重新烘焙。

- alpha.15追加太阳最内核CORE_RADIUS=.45，zone5默认CORE_CONTACT_DAMAGE=1000000（独立配置stellarCoreContactDamage，仍乘全局灼伤倍率），外层zone4半径1.25保持8点。新增heat_core翻译与色散匹配。seed碰撞为空、选择框保留，避免隐形实体立方挡住玩家进入中心；伤害仍走原hurt与is_fire等标签，禁止直接setHealth/discard，正常抗火与保命机制可拦截。每5tick扫描，中心检测≤0.25s。

- alpha.15用户明确要求使用.vsh/.fsh替换方格表面并优化性能；世界胚核现由SolarShader+SolarSphereMesh+shaders/core/stellar_surface.*绘制，原sun.obj仅为物品和shader加载失败回退。shader源手写，JSON描述由generate_solar_resources.py维护，无新位图。球半径仍1.25。
- SolarClient的RegisterShadersEvent使用ResourceLocation构造ShaderInstance，只在成功回调保存实例。RenderType用POSITION_TEX_COLOR_NORMAL，单次opaque、CULL、LEQUAL、写颜色/深度。RGB编码原单位球方向、A编码热态，UV0编码cos/sin相位，Normal为Pose变换后的径向法线；不能直接设置逐核心uniform后延迟批绘，否则多核心状态串用。Java每draw复用两个Vector3f，网格12/8/4细分在类初始化生成（864/384/96面）。16/40格LOD切换，40～48格淡出场线。
- GLSL150程序纹理最多4次固定3D值噪声、0纹理采样，远处或子像素细节省略第4次；原生fog.glsl与ColorModulator正常使用。gravity_surface.fsh复用core_convection.glsl和stellar_surface.vsh，CoreRenderer的能量体使用drawGravity按0.3缩放至原半径0.375，LOD距离除0.3按屏幕尺寸降档，外金属环继续原模型。引力环光效24～32格淡出。不引入全屏后处理或raymarch，不自行绑定额外FBO。真实OpenGL预览工具仅在隐藏窗口验证，不启动用户游戏。

- alpha.14恒星警告字体复用同仓库OverloadCore独立ChromaticTooltipText风格，参考作者huige233。client/ChromaticWarningText保留署名、斜体/红蓝叠影/轻微抖动，警告不做逐字等待。必须用FormattedCharSequence覆盖叠影Style色，且Font将alpha<4视为不透明，淡出小于4的叠影应跳过。
- SolarWarningMixin仅放mixins配置client，WrapOperation包裹Gui.renderOverlayMessage内唯一GuiGraphics.drawStringWithBackdrop(Font,Component,int,int,int,int):int，只匹配heat_warning/heat_burning/heat_contact三个翻译键。复用原HUD变换与alpha、按斜体宽度重新居中，窄屏缩放，原文字背景选项保留；其他消息original.call，无新网络包或独立HUD计时。THIRD_PARTY_NOTICES随JAR打包，不新增OverloadCore依赖。

- alpha.13球面不再把一张16px图包住整圈经度；solar_surface.py将六个16×16网格球化，贴图密度均匀且两极无汇聚点。photosphere-v2.png为内置image_gen原创双格图集，atlas-layout.json保存1774×887及两格裁剪范围。export_textures.cjs按names.length处理图集格数，39张运行PNG仍是真16×16，检查旧37张哈希未变。
- 太阳预览改用tools/raster_preview.cjs对原生模型做逐像素最近邻采样与深度测试，避免SVG画序把隐藏三角形叠在球面上；归一化正交轴避免球看成椭圆。新旧球面比较工具preview_solar_surface.cjs只在预览中重建旧32×16经纬网格，不参与游戏。未引入shader或修改客户端渲染流程。

- alpha.12 SolarController.isCoreHot将历史启动付费ignited与实际热态区分：enabled、ignited、formed且gross>0/剩余预算/允许续料的可用燃料满足时热；满缓存/红石暂停保持，停机/耗尽/拆坏停止。SolarHeat每5tick局部查询LivingEntity，身体AABB到核心中心距离4/3/2/1.25分区，伤害2/4/8分别20/10/5tick，预警私发actionbar+pling且同级40tick限频。UUID暴露计时仅内存，不持久化，不保存实体引用或加载区块。
- 新stellar_heat数据伤害在generate_solar_resources.py生成，is_fire/bypasses_cooldown/bypasses_shield/no_knockback标签使周期热伤不被10tick受击无敌吞掉，保持护甲/抗火/事件/不死保护流程，hurt成功才点火。创造仅预警、旁观忽略，server config含开关和倍率；不直接改血、不处理掉落物。
- SolarPart热态hot与visual只在seed更新包同步，不能写入STOCK或调用loadAdditional清空库存。SolarRenderer分别平滑热态体积与实际负载运动；满缓存热态球半径仍1.25，场线调用SolarField.emit额外size参数以免藏进球内，熄火后正常收束。

- alpha.11 SolarField为无世界访问的纯Java轨迹生成器，SolarRenderer消费其带宽度/颜色/alpha/halo的线段；外层三环半径1.65/1.81/1.97随核心缩放，六条弯曲极向场线、两束聚束光与四向采能流。光带偏移取view×direction并处理平行退化；使用当前依赖原RenderType.lightning的POSITION_COLOR、SRC_ALPHA/ONE混合。实际最大640 quads，包围盒沿用seed.inflate(4)，不改通信、资源或服务器tick。
- tools/ExportSolarField.java直接运行已编译的SolarField/CoreMotion，检查1200个phase/strength组合、边界/预算与平滑启停，输出build/solar-field-frames.json；tools/preview_solar_field.cjs投影真实轨迹和原生太阳/聚束器模型。产物art/solar-field-preview.png为静态离线三帧，不可宣称游戏截图。

- alpha.9太阳WATCHERS与OWNERS分离：完整621格监听独立于遇到首个缺件即终止的所有权扫描。onLoad/区块load/unload登记或失效，detach清理监听；无20tick轮询。placement与refreshLayout使用同一布局派生朝向/segment，刷新匹配部件不覆盖其他存活主控的部件，不修改OUTPUT/库存。SolarConstruction材料数与坐标分隔符写入UTF-8语言生成器；Python读取源码须显式encoding，默认GBK会把“×”“·”变成“脳”“路”。
- alpha.9 tools/solar_rings.py沿原24个周界节点生成6类连续斜接梁，冠架增加交叉、直梁、T接头；SolarLayout的segment/facing、OBJ与art/solar-beam-states.json来自同一生成逻辑。环和冠架方块状态覆盖保存中的全部segment/方向。energy保留runtime_geometry原红蓝lamp，不能再被太阳贴图覆盖；内凹＋／－同时作为形状标识，SolarClient的物品output getter仍读STOCK。

- 2026-09-24用户要求开始实现微缩太阳并逐步补全。alpha.8已接入基础版，行为以SOLAR_README.md为准；SOLAR_DESIGN.md/SOLAR_EXPANSIONS.md包含未完成候选，不能全部宣称可用。实际默认256/512/1024/2048 GFE/t、16/8/4/2小时、单份737.28 PJ，缓存1.024 PFE、单口1.024 TFE/t额度。长效成品燃料供料，氘氚只用于制备。
- 微缩太阳核对经验：Generators化学品ID使用mekanismgenerators命名空间；加压反应室MAX_FLUID/MAX_GAS均10,000。现有FuelRecipe上限1 PJ，太阳737.28 PJ需独立stellar_fuel与单配方10^18 J上限、long安全乘加；64份总预算会溢出，保持物品库存与单份预算分离。原Ports的64次int FE分批理论上限约137.44 GFE/t/口，不能冒称满足1.024 TFE/t；优先原生long并有界FE回退。现有7格Structure/Controller/Ports也不能直接套9格结构。
- alpha.8已做自动调载、残余胶囊回收、基础供能明细、耗尽比较器与太阳动效。CC和自定义调载阈值仍未实现；耀斑与观测告警已于alpha.23通过独立模块实现；分级整组升级已于alpha.19完成，日冕加工已于alpha.20完成。不要加入未完成功能占位按钮。胶囊与主控预算原子转移、不带点火资格，满背包不回收。
- solar/包是独立装配/存储域：SolarStructure按生成的SolarLayout扫描八角足迹内9格空间，缓存校验、跟踪失效位置、禁止重叠控制器和强加载。SolarLayout由generate_solar_resources.py从art/solar-design/layout.json生成，220个实块、219块加主控；角落足迹外不要求空气。每组9块采能翼同级，四组可混级；环/聚束器最低级决定约束上限。装饰FACING/SEGMENT更新不得触发递归校验，聚束器真实转向必须触发；alpha.9采能翼朝向属于布局派生状态，旧片也自动纠正。
- SolarController只保存储电、当前单份燃料余量/总量、点火和设置；fuel仓是SolarPart自己的18格ItemStackHandler。SolarFuelRecipe独立上限10^18 J，不批量将64份相乘。STOCK/DATA/FUEL_DATA三种组件分别处理仓、主控、残余胶囊。SolarMenu按实际点击部件距离校验而非只按主控，远端柱子也可打开。SolarPorts保留共享+单口tick额度、模拟无资源变化、停止后取料、真实自动弹出。
- SolarConfig独立mekgravity-solar-server.toml（J）。alpha.10取消原50%目标与80%提前停机：两种模式均向实际容量补满；automatic额外按上一tick真实exported净能量快速升载，关闭时使用常规升载。净可容纳量与毛燃料不能直接取min：net=fuel-ceil(fuel/20)，需反算fuel=net+ceil(net/19)，只对小于当tick产量的净余量反算，禁止乘20导致溢出。满缓存不收费、不取下一份燃料；最后单份残余仍保持预算守恒。SolarScreen续航用菜单同步power×load/100，标明设定负载，不再除瞬时gross。比较器只告警全部燃料耗尽，统一在反应事务结束后刷新。
- 太阳资源入口generate_solar_resources.py由generate_resources.py统一调用，生成模型/配方/语言/布局Java。alpha.13的sun.obj由tools/solar_surface.py生成六面球化表面，每面16×16细分共1536个四边形，径向法线、半径1.25不变。旧solar图集仍供采能翼/灯光/燃料；新photosphere-v2为两格横向图集，photosphere_idle/active独立用于球面，均16×16。原稿及提示词在art。SolarRenderer只绘制seed，使用CoreMotion，日珥/日冕/向外光点固定网格预算，不spawn实体。聚束器使用共享ModelShapes鼻部选择框。
- 原PRC制备：燃料坯+8000mB D-T化学品+1000mB水，1200tick/200额外J；现发布件升级组件入口为getComponent()，不是getUpgradeComponent()。测试用速度8以覆盖实际完整制备，时长由原Mek倍率计算，不假设每级2倍。
- 用户在2026-09-22确认art/models/graybox-v1灰模，alpha.5已接入。tools/runtime_geometry.py从批准的assembly.json提取几何，移除内部/覆盖面并合并共面矩形，再使用现有原创16×16贴图分配UV；不改灰模比例、不用满面机器纹理遮盖几何。generate_resources.py生成运行JSON与ModelShapes.java，不能只改单个生成文件。
- AssemblyAppearance复用已有FACING：成型框架UP为立柱、EAST为世界X横梁、NORTH为世界Z横梁、DOWN为对称接头；面板朝内、玻璃和接口朝外。COIL的FACING是实际瞄准，不能被外观代码改写。Shape只为外伸线圈新增，来自同一源几何；六方向实际level.clip(OUTLINE/COLLIDER)回归覆盖0.25格鼻部。
- 灰模文件和ZIP保留可编辑模型，不直接进入JAR。generate_graybox.py输出OBJ/MTL/assembly.json/scene.js；保留93个逻辑玻璃锚点。游戏仍逐块保存/掉落，不能把合并的预览玻璃当成库存实体。
- 灰模预览使用原生WebGL2，不依赖CDN/第三方3D库；支持完整/剖切/内腔、爆炸、灰模/分色、线框、旋转平移缩放与射线选择。点击默认穿过玻璃，Alt点击可选窗面。OBJ单位为方块，不是像素，八接头/60段框架/六发射器/核心仍基于7×7×7结构。
- Content注册主控、7种部件和4级线圈；旧COOLANT保留ID与合成供回收，只从创造列表隐藏。MachineBuilder默认有升级属性；移除AttributeUpgradeSupport禁用升级，不能调用空withSupportedUpgrades。
- Controller只持有真实能量、启动状态、反应余量以及旧冷/热钠缓存；燃料仓各有18格原物品，不能复制保存在主控里。冷却库存不再影响发电，也不接收新钠。
- FuelRecipe是matter_fuel数据配方，energy单位J。反应余量按投入时预算保存，配方变更不重算已付费存量。2%自耗从毛发电扣除；不再另扣冷却热量。
- Config.performanceRevision一次性迁移旧默认功率与端口速率，保留非默认值及后续手动调整。
- Ports在每次操作校验实体、加载状态、外侧面和完整结构。Part的单口in/out额度与Controller的全堆received/exported均按世界tick共用；新建handler、模拟调用不能增加额度。总额度来自对应方向的实际能量口数量，不取线圈功率。
- Ports.emit对long接口和FE桥都允许最多64次分批提交，零接收立即退出。FE路径先模拟接收，再由ForgeEnergyIntegration向下对齐可支付整数FE，防止奇数接收量造成半J取整差。原储能与保护备用电仍共用一份。
- Structure登记全部343格，结构变化/卸载即失效。FORMED是纯外观方块状态；validate完成/失败、invalidate和重载均刷新已关联的加载部件，不能强加载或改另一个结构的部件。
- StructureChangeMixin忽略active/formed及非线圈Part的装饰朝向变化，避免重组循环；线圈朝向、主控方向、端口OUTPUT及真实方块变化仍会失效。失效时同时清理旧claimed位置，避免转动主控后旧范围残留成型皮肤；失败校验和重载也清理已关联核心/线圈ACTIVE。
- GlassConnections读取26邻格，仅按方块类型连接，忽略active/facing/formed；每面4边+4角，对角缺格保留内角。ConnectedGlassModel按ModelData选择不可变预烘焙quad列表，物品模型保留完整外框，无CTM依赖。
- 玻璃未成型沿用48个glass边角子模型；成型用54个window子模型（6方向×8边角+1窗面），方向与GlassConnections的面内坐标一致。窗框为solid，窗面为translucent，BakedModel覆写getRenderTypes并缓存256种组合，不在逐帧拼接列表；窗面颜色用21.1.241 neoforge_data/ExtraFaceData，引用原版white_concrete而未复制位图。MC LevelRenderer.blockChanged重建±1格，覆盖对角变化。
- tools/core_mesh.py生成原生neoforge:obj核心：128个球面面片与两环各96面片，MTL用#energy/#steel/#inner引用已有材质。完整core.obj保留物品图标；core_energy/core_ring_0/core_ring_1三个OBJ严格复制相应分组，球体另有active材质变体。ModelEvent.RegisterAdditional注册4个模型，资源重载后通过ModelManager取新烘焙模型。
- alpha.7核心世界RenderShape为ENTITYBLOCK_ANIMATED，CoreRenderer每次绘制球体与双环（停机也显示），不再叠加静态方块核心。球体自转/浮动/呼吸，双环反向转动；1道细光环+2段追逐弧、6束流、18个光点，固定预算且不spawn实体。现有渲染包围盒覆盖运动与束流，远处走原生BER距离裁剪。
- CoreMotion仅为客户端持有的平滑状态（弱引用表），以世界tick+partial积分，解析指数平滑消除帧率差异，暂停不动，停机衰减为零。不循环截断相位，避免周期跳变；传给float角度前做360度取余。
- Part.visualLoad由Structure.activity按gross/额定功率取0～100推导。只发一字节visual_load更新标签，连续变化每10tick最多一次、零/非零切换立即发送；getUpdateTag覆盖新客户端进入区块。handleUpdateTag和onDataPacket显式只读取此键，不走loadAdditional，避免清空库存/master；不写入NBT、DATA或STOCK。非核心不发效果包，客户端不扫世界查询主控。
- ReactorMenu/Screen为230×244，玩家槽35,159，库存标签34,146；主信息区8,28,194,60，余量条y106，按钮y122。主页面无钠仪表。启动前显示已充/所需量，能量窗显示实际吞吐与上限。
- 截图中的仪表重叠源于MEDIUM实际34×60而非16宽；STANDARD实际18×60，需计入overlay外框2像素。未来任何仪表排布按控件真实尺寸，不靠名称猜测。
- 缓存回收窗口只在旧cold/hot非空时显示；停机后可从输入模式口抽冷钠，输出模式口取热钠。主控与燃料仓NBT、物品DATA/STOCK均保留资源。框架/线圈不附加无用STOCK。
- Construction.plan为224个部件加主控，默认93玻璃、不含冷却口。生存用真实ItemStack.useOn，保护取消后保留已放部分和材料；默认蓝图不会替换已有不同种部件。
- 根docs/gravity-reactor为结构与视觉资料；代码生成JSON只改tools/generate_resources.py。运行贴图必须16×16，原稿/图集/提示词放art且不进JAR。

## 验证

- alpha.26共48项GameTest通过。LinkPanelTests三项覆盖实际空气use／ModuleMenu按钮、Snapshot与Action编解码、自动发现+私有／范围过滤、两类绑定+真实物品交付、通道切换与断线保留资源；过期revision、menu/session、pair距离、错源类型、自连、权限收回、同位置替换BE、持有物／host／交互距离失效均拒绝。测试世界地表约Y=-60，竖直距离夹具用世界高度中点，不能把低端放出建造高度。未启动客户端，GUI观感/鼠标操作留用户验收。
- alpha.25共45项GameTest通过。NodeResourceTests覆盖正常三步远端配对与清选择、6000物品＋100GJ级能量＋两种流体（带组件）＋氢/红石化学品同一tick交付及准确耗能；保存/原生ItemStack组件/实际掉落重放保留双侧缓存及核废料；四类自动eject到原PRC、独立通道暂停与满接收端守恒。PRC基础储能仅2000J，测试核对节点余量；用1份生物燃料（原反应需2份）验证插入而不启动反应。flare实际GLSL、相位/冷热/遮挡/多实例及384/96LOD验证通过，未运行客户端。

- alpha.24仍41项GameTest通过：扩展原Forge实际拆放回归，在16个恒星合金完成后仅送4个flare_cell（无燃料丸），再产4个compressed_stellar_matter，累计20GJ/基础40tick正确；复杂配方与单输入路线并存。中英文运行资源及JAR检查去除了旧记账措辞，界面未做客户端验收。

- alpha.23共41项GameTest通过：OrbitalTests六项覆盖真实工具绑定→配方加工→箱子，Forge付费物实际拆放继续，Node满端不扣资源及10000物品真实储物箱守恒，私有源/维度/距离拒绝，三模式能量/燃料与1～160J末端、主控掉落保存冷却，观测站真实灯和坏结构告警；满缓存升载不消耗晶核，信号只发正面且OBS无红石启停。所有旧35项继续通过；五类shader隐藏GL动态/停机/遮挡和几何去重检查通过。没有客户端游戏验收。

- alpha.21共35项GameTest通过：四个真实舱经capability补给，原生ULTIMATE_BIN接收，四个等级分别预热20tick后测40tick准确出量并核对实际发电足以补回扣能；扩容4096模拟/容量拒绝、正常64手动提取、合并/shift守恒、BE与掉落物双序列化和真实重放；512已付批保存/堵塞/熄火交付、旧32批40tick/160GJ记录不重算。模型和碰撞来源相同，已检查组合深度预览及JAR资源；没有客户端视觉验收。

- alpha.20共32项GameTest通过。CoronalTests覆盖真实漏斗供料66原铁→66铁锭并自动出箱、模拟无副作用、输出拒绝外部插入；付费32批暂停/BlockEntity.loadStatic/真实掉落重放、恢复不重复收费、满输出保留32份及熄火交付；分散与重叠输入、真实合金配方、tier门槛、原生右键菜单暂停。VerifyCoronalShader隐藏GL验证实际GLSL、流动/停机像素、深度遮挡与不写透明深度，128面；静态模型深度预览复用16px原素材。未启动客户端。

- alpha.19共29项GameTest通过：AssemblyUpgradeTests两台真实菜单20/21/3、缺料补建、取消放置/拆除恢复、精确退料和旧BE数据；PortFairnessTests两台四个原生能量立方的真实tick轮换、守恒、读数过期归零；GravityCompletionTests 1～100 J末端及2tick补齐外观；RuntimeSummaryTests128份太阳燃料能量饱和、胶囊实际余量、热态和建造等级保存。ExportSolarField检查430/76段与原运动；没有客户端视觉验收。

- alpha.18先加入FuelPersistenceTests，两台燃料仓的真实菜单QUICK_MOVE合并都在原实现失败（数量变但chunk.isUnsaved=false）；换FuelInventorySlot后23项服务端测试通过。测试清除已保存区块标记再点击，覆盖非空堆叠合并、只剩8格容量的部分取出、右键分半/单个合并、满背包阻止操作；每步用原ChunkSerializer.write并BlockEntity.loadStatic重载核对数量，额外核对掉落STOCK，finally恢复区块脏标记并清理模拟玩家。

- alpha.17构建、gravity shader真实OpenGL编译/多核心/冷热/遮挡验证及紫色粒子烘焙通过；新粒子16×16/不透明/233色，物品七种显示context、CoreRenderer共用入口及JAR引用检查通过。纯客户端变更不重复21项服务端测试，没有启动游戏客户端。

- alpha.16构建与VerifyFieldShaders隐藏GL验证通过：两shader实际编译/链接、运行/停机/移动像素差、实体深度遮挡、透明场深度不写。GravityRingMesh逐顶点与core_ring_0/1.obj一致、闭合边、96面/环；粒子16×16/不透明/正常颜色，物品builtin/entity及扩展注册契约检查。本次纯渲染，不重复21项服务器测试，游戏视觉由用户验收。

- alpha.15构建及tools/VerifySolarShader.java隐藏OpenGL工具通过。测试读取真实.vsh/.fsh并展开当前MC fog.glsl，在GLSL150编译/链接/重链接、同一buffer两星独立热态/相位、冷热及流动像素、深度遮挡、3档闭合面/法线/半径。GPU计时为640×640离屏特定显卡结果，不等于游戏帧率；运行需编译main classes、现有LWJGL3.3.3及JOML1.10.5和平台natives classpath，输出build/shader-check。增加中心伤害后已运行21项服务端GameTest通过，无客户端游戏验收。

- alpha.14构建、javap核对当前1.21.1发布件的renderOverlayMessage调用及WrapOperation描述符、client配置与JAR归属检查通过；新增字体不改变服务器，无需重复20项GameTest，实际观感由用户验收。

- alpha.13构建与资源/几何检查通过：1536个面朝外、各焊接边恰好两面共用、顶点半径1.25、UV和径向法线有效、39张16×16PNG、旧37张像素哈希一致。已看近景前后对比、整机与三个动态时刻预览。本次仅模型/资源修改，不重复20项服务端测试，客户端由用户验收。

- alpha.12共20项服务端测试通过。SolarHeatTests通过真实ServerPlayer数据包观察预警/限频/创造无伤与热态重载、关机/耗尽/拆坏；真实Cow验证距离、周期灼伤、着火和抗火保护，GameTestListener在通过/失败时都清理实体。ReactorTests.player新增包观察者重载，原调用保留。特效导出再检查240组全尺寸低负载热态待机。

- alpha.11构建、特效轨迹检查与离线预览通过，无服务端行为改动，不重复跑18项GameTest。使用JDK21执行 `java --class-path build/classes/java/main tools/ExportSolarField.java build/solar-field-frames.json`，再用现有Node/Sharp执行 `tools/preview_solar_field.cjs`；build目录快照不提交。

- alpha.10构建与18项服务端测试通过。solarFillsPastHalfCapacityAndResumesWithoutWastingFuel复现50%可用缓存无负载升载、50/80/99%额定功率，枚举自动开/关最后1～40 J取整与自耗守恒，再通过真实端口抽能、控制器tick补满/待机。无需重复长时燃料测试；现有144000步预算回归继续覆盖总预算。

- alpha.9构建与17项服务端测试通过。新增真实BlockItem.useOn在四种主控朝向和±89°俯仰放置全部翼片；不调用validate的真实tick验证未成型远端片刷新与补齐/拆坏外观；原Mek配置器切换真实handler，再掉落/重放验证OUTPUT组件和方向。模型资源状态穷举与细梁共享端点检查通过；没有客户端验收。

- alpha.8共15项服务端测试通过，SolarTests新增4项：两小时144000次稳态预算计算/64份库存不溢出、胶囊原子回收和重载、四翼等级/满缓存暂停恢复/旧能力失效、保护施工扣料/远端菜单/原能量立方，以及原加压反应室真实tick制备。未运行客户端；无界面测试与离线预览不替代模型/UI验收。
- alpha.7构建与11项GameTest通过，CoreAnimationTests验证真实结构负载/启停/拆坏、效果标签不持久化且不覆盖库存，以及20/60/144 FPS相位一致/暂停/停止。三个拆分OBJ与完整核心的坐标、面序、UV逐项相同；运动径向间隔检查通过。art/models/core-motion-v1为可离线操作的Canvas简化着色预览，非游戏画面。
- alpha.6构建与10项服务端测试通过。既有fuelStartupAndReloadWorkWithoutCoolant追加600次终极稳态反应的单丸耗尽/下一丸衔接/能量守恒检查，模拟测试接收器收集缓冲增加量，不宣称这些循环是600个真实服务端tick；旧短余量保存校验不做倍率迁移。
- alpha.5构建与10项服务端测试通过，新增四种主控朝向下的角色分配/重载/损坏修复/真实线圈转向及主控转向残留清理，以及六向鼻部OUTLINE/COLLIDER世界射线。原8项发电/输电/物流/保存/玻璃回归继续通过。
- 资源检查覆盖所有保存状态组合恰好命中一个模型、33张16×16贴图、54个成型窗模型及非重叠外表面。model_preview.cjs投影原生JSON/OBJ，preview_runtime_parts.cjs和preview_reactor.cjs检查部件与整机；不能当作客户端视觉验收。
- 默认配置缓存下build runGameTestServer通过。NeoForge加载失败可能返回0，因此任务检查新日志的测试完成标记及本模组配方解析错误。
- mekanismgenerators:reactor_glass属于Generators。14个配方在游戏内注册由测试检查。
- 没有启动客户端；模型/材质/UI检查与服务端测试不能代替游戏内视觉验收。JEI和Extras专属满载行为仍由实际整合包测试。
