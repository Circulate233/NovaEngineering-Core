# Nova Engineering 客户端启动时间与内存优化清单

旧 `F:\nova\startup-*` 目录已清理。开发客户端后续逐轮记录与可复算的动画帧内存结果集中在[直接实机对照](startup-direct-results.md)。

最初六轮基线使用 mcmpu 启动 NovaEngineering-World-V2，核心包版本为 `1.23.9+522b95e6`，Cleanroom 0.6.13-alpha，Zulu 25、G1、8 GiB 初始堆和上限。前三轮比较时间与内存，第 4 轮用 Flare 定位线程调用，第 5 轮用 JFR 追 GC 根，第 6 轮在主菜单 Full GC 后转储完整堆。后续独立对照使用开发客户端当前的 5 GiB 堆，另计字符串去重、LibNine 模型读取和 Actinium 直接缓冲的实际效果。时间端点统一到 VintageFix 首次菜单。[实施决策与九轮对照](startup-implementation.md#第一批实机对照)

[启动墙钟](#启动墙钟与阶段覆盖) · [类加载](#类加载转换与注册) · [模型与纹理](#资源模型纹理) · [配方与模组](#配方脚本模组初始化) · [菜单内存](#菜单存活堆峰值和进程内存) · [分配覆盖率](#启动分配核对) · [后续实验顺序](startup-implementation.md#后续实验顺序) · [独立对照原始结果](../../../nova/startup-resource-ab-20261125/results.md)

## 优先级与修改范围

| 要改善的指标 | 第一批值得独立对照的操作 | 后续结构改造 |
|---|---|---|
| 首次主菜单时间 | R1 同一模型重复探测与开流、R3 CTM 临时烘焙、R5 普通 PNG 重复读取与格式探测。三项在同一个 14–15 秒资源窗口，按每项对窗口、全程的实际影响排序。 | R2/R4 普通模型和变体直接生成最终表示；B1/B2 脚本多阶段处理、HEI 插件和索引；B6 加载画面首次资源刷新；B8/M7 冻结注册表和重建统计展示。 |
| 启动分配、峰值与 GC | 先查资源阶段的构建副本，以及后期两个 GC 之间同时活跃的配方、脚本、UU 和机器任务。 | B3–B5 消除匹配器、结构旋转和 UU 的短命对象。只有主线程等到这些任务或端到端实测改善时才记启动时间收益。 |
| 菜单常驻与进程提交 | R6 先改 Actinium 的透明像素写回，再改初次图集上传；R7 区分无候选命名空间和真正逐文件失败。M1 已测得菜单存活堆少 93.98 MiB；V2 已测得早期直接缓冲峰值少 611.77 MiB。 | M2 的脚本调用方、M3–M10 按所有权评估模型、字体、资源、注册表、脚本与搜索。V1 单独测较低 Xms 对提交、扩堆和首次进世界的影响。 |

类加载器及转换实现由已有 PR 负责；L1–L5、保护域、类补丁表与启动器耗时只作基线归因。L6 和 M2 只保留模组订阅、脚本查询等业务侧候选。启动器用 mcmpu，业务资源读取可以在星核处理，Actinium 动画访问需要独立分支。逐项改法、前置证据和验收详见[实施决策表](startup-implementation.md)。

**读表方式。** “墙钟”是玩家实际经过的时间。“主采样秒”“池采样秒”是第 4 轮 Flare 在主线程或并行工作线程上采到的权重。分配是三轮启动 JFR 事件的加权估算。全程分配表沿用采集器检出菜单日志的时间；14 段对齐表截在 VintageFix 首次菜单事件。采集器较菜单事件分别晚 0.565 / 0.447 / 0.062 秒。“保留”是菜单对象的校准支配大小。数组载荷与浅大小按物理地址去重，非数组对象本体按实机类直方图核算。父调用包括子调用，支配者包括其下级，跨表数字有重合。下表用“对照”标记可以直接做单项原型的候选，用“核验”标记应先记录命中、所有权或可达入口的候选。

## 启动墙钟与阶段覆盖

| 指标 | 第 1 轮 | 第 2 轮 | 第 3 轮 |
|---|---:|---:|---:|
| mcmpu 到 Java 进程创建（秒） | 92.179 | 6.244 | 6.313 |
| JVM 到首次打开菜单，VintageFix（秒） | **125.522** | **105.836** | **101.816** |
| mcmpu 到脚本检出菜单日志（秒） | 218.378 | 112.647 | 108.301 |
| 启动累计分配（GiB） | 36.51 | 36.36 | 36.06 |
| 已观测用堆峰值（GiB） | 6.72 | 6.29 | 6.28 |
| 菜单静置后诊断 Full GC 存活堆（GiB） | 1.697 | 1.690 | 1.696 |
| 菜单前 GC 停顿合计（秒） | 2.03 | 1.67 | 1.64 |

以下 14 个区间首尾相接。前三列单位为秒，分配列为各轮区间内所有线程的 JFR 样本权重（GiB）。日志按整秒取中点，邻接日志之差约有 1 秒误差。表显示到小数点后一位，原始值逐轮闭合至 VintageFix 计时。能取得组件内部毫秒计时的地方另列。AA 是 Actually Additions，其日志仅用作时间标志。

| 起止标志 | 第 1 轮 | 第 2 轮 | 第 3 轮 | 三轮分配（GiB） | 这段要查什么 |
|---|---:|---:|---:|---|---|
| JVM → 初次发现开始 | 2.0 | 2.3 | 1.4 | 0.41 / 0.56 / 0.13 | 引导基线，超早期不可触达部分按约定跳过 |
| 初次发现开始 → 完成 | 8 | 6 | 7 | 2.40 / 2.05 / 2.61 | Cleanroom 自报 7.310 / 6.780 / 6.473，270 个 mod ID。这次扫描早于 Nova 注册 |
| 初次发现完成 → 游戏 Main | 6 | 5 | 5 | 1.21 / 1.35 / 1.29 | Mixin 准备、VintageFix 缓存反序列化等，L4/L5 |
| 游戏 Main → 首个 Construction | 10 | 10 | 9 | 2.14 / 2.37 / 2.14 | 后续候选探索、资源包遍历，L5/R8 |
| Construction 全部 276 次派发 | 21 | 15 | 15 | 4.92 / 4.89 / 4.67 | 自报 20.531 / 14.925 / 15.060。加载画面、Forge 订阅者、CraftTweaker、Forgelin，L6/B1/B6 |
| Construction 结束 → PreInit 开始 | 1 | 0 | 1 | 0.25 / 0.00 / 0.48 | 边界接近日志精度 |
| PreInit 开始 → 最后完整可见派发 | 26 | 19 | 18 | 5.98 / 5.97 / 5.99 | 245 项合计 25.617 / 18.589 / 17.581。IC2、CraftTweaker、NC、Ender IO、AE2 与相互嵌套的加载工作，L1–L6/B1/B5 |
| 最后可见派发 → 主模型资源缓存开启 | 8 | 8 | 7 | 2.07 / 2.30 / 2.03 | 仍有模组初始化、方块/物品/矿辞注册、Kirino ECS。B7 |
| 模型资源缓存开启 → 关闭 | 14 | 15 | 14 | 8.89 / 8.81 / 8.97 | 模型、图集和烘焙事件，R1–R9、R11。方块模型加载至 bake 完成约 11.8 秒 |
| 模型资源缓存关闭 → AA Init | 5 | 4 | 4 | 1.17 / 1.14 / 1.04 | CraftTweaker 的配方事件、ZenUtils 脚本和 NuclearCraft 配方 |
| AA Init → AA PostInit | 6 | 5 | 6 | 1.41 / 1.22 / 1.55 | 含 Ender IO IMC/XML 配方，B3 |
| AA PostInit → HEI 开始 | 8 | 7 | 6 | 2.24 / 2.38 / 2.20 | 脚本修改配方、机器与模组收尾、RTG 和 Astral Sorcery 等，B2/B4/B5 |
| HEI 开始 → HEI 结束 | 8 | 7 | 6 | 2.04 / 1.83 / 1.80 | 自报 7.295 / 7.019 / 6.156，插件注册和配方构建约各两秒，B2/M4 |
| HEI 结束 → 首次打开菜单 | 2.6 | 2.6 | 2.4 | 1.19 / 1.32 / 1.13 | GameData.freezeData 的尾段样本中可见 ForgeRegistry.sync、fireRemapEvent 与 StatList.reinit；B8/M7 共用这段时间 |

逐轮以未舍入数相加，14 段墙钟分别闭合至 125.522 / 105.836 / 101.816 秒，同端点内分配权重合计 36.306 / 36.192 / 36.037 GiB。前表全程分配与这里的差额落在菜单事件到采集器检出之间。

首轮比第三轮晚打开菜单 23.706 秒。仅 Construction 和有逐项计时的 PreInit 就多约 13.5 秒。首轮 PreInit 的主线程经 `ActualClassLoader.readFully` 触发 1,002 次被记录的读取，涉及 141 个文件，共 2.070 秒。三轮主线程阻塞和 I/O 事件去重后为 7.048 / 3.461 / 3.590 秒，覆盖首轮额外等待中的约 3.5 秒。文件读取已包含在这个并集内。读阈值与节流会遗漏短事件，物理磁盘及操作系统页缓存贡献尚无直接计时。

Forge Construction 之前的图形初始化也有可定位的时间。Actinium 建窗路径三轮 35/44/50 个主线程 native 样本，其中 GLFW.glfwCreateWindow 占 33/40/45 个；该方法首末样本跨度约 0.67/0.82/0.92 秒。第二轮日志记录首选 OpenGL 4.6 创建成功。当前证据支持先分开计一次成功建窗与真实发生的版本协商重试，调整回退顺序的收益尚无实测依据。

**启动器。** 首轮启动 Java 前比后两轮多约 86 秒。三轮均复用运行时安装、检查 233 个项目文件，首轮存在元数据刷新的线索。mcmpu 的日志没有给出各网络请求、校验和覆盖层复制的分段时间。因此将 launcher→Java 单列为整合包端到端基线。

## 类加载、转换与注册

L1–L5 记录类加载器 PR 所在范围的实测量，供解释业务阶段中的嵌套调用。

| 项目 | 实测 | 归因范围 |
|---|---|---|
| **L1 类字节** | 父类加载器 resourceCache 保留 447.39 MiB。44,714 对点号与斜杠路径对应的不同数组逐字节相同，重复载荷 183.53 MiB。 | 类型定义、加载器缓存。 |
| **L2 EnderCore** | SimpleMixinPatcher 每轮约 1.10 GiB 分配，1.00 主采样秒，其中 readCode 占 0.66。 | EnderCore 接口转换器。 |
| **L3 Forge 父类映射** | 重映射 5.66–5.85 GiB/轮。父映射查询 7.24 主采样秒，内含 getClassBytes 6.26，内含类路径定位 3.40。 | 层级查询、重映射、类字节读取。 |
| **L4 SideTransformer 和 Mixin** | SideOnly 描述符查找中的字符串构造 212/223/196 MiB。Mixin 转换 1.91–1.94 GiB/轮，transformClass 约 6.74 主采样秒。 | 转换器实现；业务方法的普通 Mixin 可用于其他项目。 |
| **L5 模组发现** | 发现家族 2.86–2.89 GiB/轮，另一次 exploreModCandidates 有 92/84/78 个主线程样本，VintageFix 发现缓存链约 216–259 MiB/轮。 | 发现器、超早期扫描和缓存。 |
| **L6 Forge 订阅方法和模组过早加载（核验）** | `DeclaredMethodTable.publicMethods` 2.38 主采样秒，其中 2.16 进入 findClass。IC2 PreInit 的 1.56 主采样秒中 1.32 经类加载，NC 的 1.12 中为 0.80。AE2 的 ApiDefinitions、Ender IO 的注册枚举也提前拉入依赖。可基于已知注解索引预筛，并把纯客户端附属数据移至首次消费。 | 已有 ClassValue。保留继承、桥接、订阅者顺序、首次调用和注册对象身份。比较首次进世界是否承接了移走的延迟。普通反射派发包含业务本体，其总样本不能算反射优化额度。 |

## 资源、模型、纹理

| 项目 | 证据与可改位置 | 实施或核验边界 |
|---|---|---|
| **R1 JSON 模型探测（对照）** | CTM/LibNine/UVL 等探测合计约 1.41–1.69 GiB/轮。`ModelJsonMetadataIndex.lookup` 在 cache hit 前定位资源和开流。用同一重载代次、最终资源包来源与模型位置缓存最少元数据，先查命中，再按需开流。 | 比较同资源实际开流、解码次数；动态包或覆盖顺序不明确时走实时路径。方块加载 2.74–2.83 秒，其调用栈的主线程 park 为 0.910/1.058/0.958 秒。 |
| **R2 模型生产与最终紧凑表示（结构改造）** | 先生成 vanilla 模型和 quad，再遍历并存入紧凑页。核心几何三轮约 400 个 Java 样本，44.48 池采样秒。FaceBakery 分配 1.11–1.19 GiB，变换/视角 0.69–0.83 GiB，blockstate 变体合并 0.86–0.96 GiB。让受控 builder 直接交付页记录，复用不可变变换、变体模板及顶点布局。 | 保留 tint、法线、面序、材质、透视、哈希碰撞核对和自定义动态模型。达阈值的 writer 锁等待跨轮波动，先测遍历和复制。单独复测模型注册表的菜单保留量。 |
| **R3 CTM 的两次重复工作（对照）** | `wrap` 触发 bake 建立纹理映射和图层，丢弃返回几何，再包装原 baked 模型。丢弃路径的终端压缩有 17.14 池采样秒及 1.06 主采样秒。优先跳过只属于临时几何的这一步。另有 `shouldWrap` 依赖图决策约 154–249 MiB/轮，可复用纹理元数据和已遍历的共享子图。 | 保留回调、图层、override 和 bake 缓存可见性。实装 StellarCore 已按根模型身份缓存决策。新候选是同一代次多个根共享子图或纹理判定，同时保留循环检测。实装的旧 OptimizeCtmBakeReplay 配置无对应实现。 |
| **R4 模型快照与派生缓存（核验）** | 已有 vanilla model/blockstate 磁盘快照，模型快照相关仍分配约 182–296 MiB/轮。按命中、恢复对象和未命中回退分开统计，验证能否用只读表示直接供下游消费。菜单模型注册和 retexture 缓存见 M3。 | 指纹包含包来源、版本、模型变换与重载代次，覆盖 F3+T 和动态模型。 |
| **R5 PNG 与纹理供给（对照）** | 物品模型完成至主图集 stitch 完成约 4–5 秒，其中 stitch 自身约 0.1–0.2 秒。普通精灵先开资源读 PngSizeInfo，再开资源读元数据和像素。格式枚举有 5.98 池采样秒。确认 PNG 后一次供给尺寸、像素和对应元数据，用有界任务解码。 | 当前已复用解码结果。保留 custom loader，对索引色、alpha、动画、mip 各层做逐像素比较，同时测最高并发缓冲和峰值堆。 |
| **R6 动画帧的访问协议（对照）** | Mip.raw 57.33 MiB，其中带 published 标记 54.73 MiB，达到现压缩门槛的已发布序列占 47.75 MiB。TConstruct 首帧读后复制、HEI ColorGetter 读后复制，可短期借用；Actinium 的 mipmap 前处理会修改透明像素，可给它有界可写视图。 | 真正对外可变的数组保留身份；写入要传给 mipmap，嵌套或跨回调持有时维持有效期。未公开原像素只有 2.60 MiB。连解码窗口和编码载荷去重后共 61.88 MiB，释放量取决于每个发布方与编码结果。 |
| **R7 动态包绑定与失败键（对照）** | StellarCore unclaimedResources 独占 10.92 MiB。无候选包与逐位置探测失败目前都逐路径存键。按绑定快照/命名空间记“无候选”，仅为实际逐位置探测记负结果。 | 保留增量注册及资源重载失效。这一结果表示找不到补绑的可变包，不能作为通用文件缺失标志。 |
| **R8 资源路径与归档索引（核验）** | ClasspathAssetIndex 扫描链约 61–62 MiB/轮。另有资源包 I/O、目录快照以及菜单 ZipEntryIndex、ZIP 中央目录（M7）。按 JAR 指纹统计有哪些索引重复保存路径和 namespace，可按归档一次枚举、分区查询。 | classpath、资源包覆盖和动态目录的搜索顺序各异。比较实际磁盘打开次数、miss 延迟、后台扫描竞争以及换包后的正确性。 |
| **R9 匠魂生成材质精灵（核验）** | CustomTextureCreator 路径约 101/234/161 MiB，另有 1.76 个非主线程采样秒。栈中出现材质可用性测试、部件集合复制、纹理存在性查询。可按材质注册代次复用可渲染部件及源纹理集合。 | 保留自定义部件的查询回调、可变贴图生成、颜色叠加及重载结果。这项用于减少生成期工作，R6 另管存活像素。 |

### 模型阶段的关键路径

三轮方块加载 **2.826 / 2.793 / 2.741 秒**，物品加载 **1.182 / 1.067 / 1.143 秒**，烘焙 **2.114 / 2.116 / 2.046 秒**。图集约 4–5 秒和模型事件也在同一资源窗口。优化可沿以下调用链验证。

- **从请求模型到真实源文件（R1/R4/R7/R8）。** 类型探测有 12.38 池采样秒，其中 8.70 落到资源定位、5.34 含 ZIP/解压。LibNine 调用占 6.48，AE2 探测占 5.90，这些子路径有交叉。Nova 的元数据命中时仍可能付开流代价；星核已给 LibNine 缓存过按位置的结果。进一步共享的内容是同一最终来源 JSON 的 UVL 标志和 LibNine 的 `9s`，CTM 的纹理 `.mcmeta` 单独按纹理和重载代次处理。方块加载时主线程有约一秒在等池中的工作，应以最长未完成模型及其读取次数判断墙钟影响。
- **从 CTM 包装到紧凑几何（R2/R3/M8）。** 第四轮 CTM 临时烘焙进入 Nova 最终几何转换的池与主线程合计 18.20 采样秒。按是否经过 Forge MultipartModel 拆开，15.68 在 multipart 路径（池 14.74、主线程 0.94），2.48 在其余 weighted 路径，0.04 为直接路径。仅给最外层 VanillaModelWrapper 特判会漏掉绝大多数。需要把 CTM 创建的取纹理函数跟到实际叶模型，保住图层/贴图回调；还要核查 Forge 的烘焙缓存是否能把临时产物交给其他消费者。独立的 R2 针对最后真正交付给游戏的几何。将 18.20 与 R2 的 44.48 池采样秒相加会重复计算。
- **从精灵预取到 PNG 与动画帧（R5/R6/R9）。** 图集准备占 27.30 池采样秒，其中 22.06 含 ZIP/解压，5.98 含 ImageIO 识别格式。TextureUtil 路径每轮分配 0.83–0.87 GiB 的 byte[]；第二轮一个格式探测栈约 184 MiB，一个实际 PNG 解码缓冲栈约 159 MiB。普通 PNG 可复用同次开流的尺寸、像素和元数据；自定义 loader 或无法确认 PNG reader 时保留原行为。R6 的长期成本发生在后续读取已经生成的帧时：TConstruct 的 AbstractColoredTexture 立即复制基础层，另一处 TextureColoredTexture 会保存引用，HEI 同步 setRGB，Actinium 会写回透明像素。四者的所有权不同，需逐个验收。

### 按实装分支复核 R1、R3、R5、R6

| 来源 | 三轮观测 | 对实施顺序的影响 |
|---|---|---|
| R1 · LibNine | isOfType 的分配权重为 761.6 / 468.4 / 864.3 MiB，采到的这些分配栈全部由 isOfType 直接调用 readModelType。星核字节码只有缓存为空才走这条边；缓存启用后经过命名空间筛选和 computeIfAbsent。注册代码构造两个 loader，星核的监听器却挂在 L9Models 的实例构造器上。 | 先验证 LibNine 缓存是否随资源加载真正启用，再评价 Nova 的公共元数据索引。Parameterized 和 Sum 两个 loader 均消耗显著分配。在当前静态文件清点的 17,722 个模型 ID 中只找到 10 份顶层 `9s`，但动态来源可以扩充这个集合，不能将磁盘清单直接作为白名单。 |
| R1 · CTM 与 AE2 UVL | CTM 的模型接受路径 577.3 / 431.5 / 399.8 MiB；UVL 内 Nova lookup 为 267.7 / 395.4 / 181.9 MiB，其中 parse 103.0 / 169.0 / 91.1 MiB。第一轮同一工作线程在 41 ms 内先后由 CTM 和 UVL 读取同一份 ContentTweaker 动态目录模型。CTM 的 `ctm_version` 来自模型 JSON，R3 的图层来自另一份纹理元数据。 | 分开记录 LibNine、UVL、CTM 的真实定位和解析。静态归档的先查后开只覆盖其中一部分。存在多来源的 227 个静态模型 ID、已经观测到的动态目录重读都应加入覆盖优先级用例。JFR 异常节流后仍有可对照的资源 ID，三种探测均失败的相同 ID 分别有 7/24/18 个。 |
| R3 · 丢弃几何的范围 | CTM.wrap 的分配约 994 / 1,020 / 1,157 MiB。其中终端紧凑转换仅 139 / 73 / 75 MiB；分开嵌套 CTM 的新取纹理函数后，能匹配外层函数的终端转换为 134 / 71 / 75 MiB、17.94 合计采样秒。临时 bakeNormal 去掉终端转换仍约 749 / 820 / 1,016 MiB，其中 FaceBakery 526 / 608 / 723 MiB。 | 可先比较只跳过终端转换，再评估对可证明纯 vanilla 子树只重放有序材质回调。后者需要验证父模型还读取的粒子、AO、GUI、视角和 override。Forge 缓存键含 getter，实装上限 50 项、写后 100 ms；只有实际已进入局部烘焙路径才允许抑制紧凑化。 |
| R5 · 普通 PNG 内部 | 按原始 JFR 的字节码位置，第一次资源定位 36 / 60 / 55 MiB、第二次 56 / 23 / 44 MiB；格式探测 203 / 229 / 198 MiB，实际 PNG 读取 606 / 566 / 584 MiB，动画元数据 42 / 69 / 37 MiB。能识别的自定义精灵 load 另有 68 / 60 / 41 MiB。 | 格式识别中有 192 / 205 / 192 MiB 发生在第一次填充输入缓存。首个 SPI 会随 JVM 改变，换 reader 后同一读取可能移到 PNG 头。外层 ImageInputStream、PNG 内层 IDAT 子流各自仍有 8 KiB 工作数组；比较实际避免的缓冲和小 PNG 分布，再决定单开流和小图解码的作用。 |
| R6 · 首次公开与后续访问 | 三轮只有 2/2/3 个执行样本包含 Frame.publish。5 个来自 TConstruct 直接复制首帧，1 个来自会保存引用的额外纹理，1 个是 Nova 对已公开帧重新登记。Actinium.processSprite 的 6/6/7 个执行样本与 publish 交集为零；三轮均未采到 publish 内的分配事件。 | 5 次采样证明调用路径存在；首次公开的帧数和字节数仍需按状态转变计数。目前不能把菜单中 54.73 MiB 直接判给匠魂、HEI 或 Actinium。两次采到的私有解码都来自 ItemContour 的 read window。 |

PNG 前缀大小另按三轮相同的 274 层加载顺序处理覆盖。在方块、物品常用路径的 28,798 份静态可见 PNG 中，小于 1/2/4/8 KiB 的分别有 22,802 / 24,885 / 27,174 / 27,928 份。2 KiB 覆盖 86.41%，增至 4 KiB 多容纳 2,289 份；每条并发读取同时多占 2 KiB 前缀缓冲。普通精灵的真实命中率仍需按实际启动请求计数，因为清点包括自定义精灵和未被注册的文件。[PNG 大小与覆盖层清点](../../../nova/startup-baseline-20261124/analysis/r5-static-png.md)

分配样本权重按相应栈计算，同行的子项通常嵌在父项内。[R1 资源 ID、精确栈和磁盘清点](../../../nova/startup-baseline-20261124/analysis/r1-probe-baseline.md) · [R3/R5 按字节码位置的三轮明细](../../../nova/startup-baseline-20261124/analysis/r3-r5-readout.md) · [R6 逐样本访问者](../../../nova/startup-baseline-20261124/analysis/r6-frame-publication.md)

**R1 已验证同次选择内的重复读取。** 在实装客户端，星核全局缓存启闭各 3 次，LibNine 所有者均为 0 次。定点对照中两个 loader 各自仍接到 120,221 次类型探测；改后 LibNine 的启闭各 3 次，Sum 先查，Parameterized 复用 120,214 个结果。真实进入 JSON reader 从 30,014 降至 15,007。三轮单项对照的 LibNine 栈分配中位数由 662.44 降至 307.91 MiB，但方块模型加载中位数仅从 3.197 变为 3.167 秒，JVM 到菜单的同组差值为 −0.511 / +6.585 / −1.651 秒。剩余 15,007 次真实输入、CTM 与 UVL 仍要按来源及具体字段判断能否共享。[方法分支与次数](../../../nova/startup-resource-ab-20261125/analysis/r1-trace-comparison.csv) · [九轮阶段与分配](../../../nova/startup-resource-ab-20261125/analysis/comparison.csv)

R6 的稀疏 CPU 样本漏掉了两个大调用方。定点记录的 89,608 次 Frame.publish 中，Actinium.processSprite 有 **35,281** 次，Cleanroom.TextureMap.finishLoading 有 **22,063** 次，TConstruct 的首帧读取后复制有 **17,411** 次。Cleanroom 将读取初始帧的位置从 Nova 钩子指定的 loadTextureAtlas 挪到了 finishLoading；后者读第 0 帧后立即交给 uploadTextureMipmap。另有 8,591 次是重新登记公开帧。CTM 和 FallbackResourceManager 的两个目标方法在这轮被 JFR 跳过。[定点记录与缺测方法](../../../nova/startup-m1-20261125/diagnostic/results.md)

**R6 按实际留到菜单的像素排序。** 另用 JFR 记录某帧第一次变为公开、后续公开读者和每轮打包。菜单 Full GC 后的十个完整快照均有 **17,008** 个物理 `int[]`、**54.1501 MiB** 已公开像素。将同一数组的全部已见公开读者合在一起，再筛出曾到达现有压缩门槛的数组，得到以下互斥组。

| 这份物理数组的已见公开读者 | 菜单 raw（MiB） | 其中曾达到压缩门槛（MiB） |
|---|---:|---:|
| 只有 Actinium mipmap 前的像素处理 | 49.4569 | **44.0830** |
| 只有 Cleanroom 初次图集上传 | 0.9837 | 0.6656 |
| 仅由上述两方共读 | 3.1477 | 2.1299 |
| 至少还有一个其他读者 | 0.5618 | 0.2609 |
| 合计 | **54.1501** | **47.1394** |

先在 Actinium 对 level 0 做受控写回，保证 mipmap 和已有外部引用读到相同像素；随后适配 Cleanroom 同步上传的借用期，两处合计可触达 **46.8785 MiB** 达到门槛的原始载荷。编码结果、解码窗口和潜在下游引用都要重新测量，净节省尚未实测。匠魂及其扩展所接触的菜单数组并集只有 9 个、4,032 字节，均未见于达到门槛的打包；其临时复制应按启动分配另评。旧 HPROF 的 54.73 MiB 属于另一轮，这里按同一轮的物理数组作分母。[按持有者的完整核对](../../../nova/startup-resource-ab-20261125/r6-diagnostic/results.md)

## 配方、脚本、模组初始化

| 项目 | 证据与可改位置 | 验证条件 |
|---|---|---|
| **B1 CraftTweaker / ZenUtils（核验）** | 三个不同入口分别做 Zen 类型注册、配方事件加载脚本、PostInit 应用脚本操作。PreInit 回调 1.40 主采样秒中 0.78 用于注册类。后续配方事件的脚本加载约 1.42 主采样秒。可共享已验证的反射签名，复用纯编译结果，并对移除无序配方构建候选索引。 | 脚本内容、导入、扩展和注册输入改变后失效；副作用仍按脚本顺序执行。通配符、OreDict、继承、错误行号及脚本重载均需一致。 |
| **B2 HEI 插件、索引和搜索（对照）** | 插件注册 2.398/2.232/1.894 秒，随后配方注册表构建 2.356/2.608/2.330 秒。一次配方需多次展开原料、取 UID/子类型、计算哈希。可在一次构建内让纯原料辅助器复用派生值，分别核对插件输入和索引写入。另有提示与可搜索名称约 0.28–0.35 GiB/轮。 | tooltipSearchMode 已启用。整栈包含 ItemStackRenderer.getTooltip 的三轮约 310/325/338 MiB，第二轮来自搜索词构建；可缓存纯 tooltip 的可搜索文本，保留动态 tooltip 和语言代次。现有小列表、UID 缓存和索引压实已生效。保留自定义 subtype、NBT、ForgeCaps、查询顺序和增量注册。分别验收两段墙钟，菜单保留见 M4。 |
| **B3 Ender IO、匠魂与其他配方（核验）** | Ender IO IMC→addRecipes 在三轮主线程有 54/43/45 个样本，已有 StAX 的 RecipeXmlReader 占 24/21/18。可按输入与配置缓存只读 XML 解析产物，再按当前注册表解析引用。另一条合金兼容链 `TicRegistration.getFluidForItems → TinkerRegistry.getMelting → MeltingRecipe.matches → ListUtil.getListFrom` 分配 159–162 MiB/轮，适合复用同一输入的匹配候选、减少重复集合展开。 | 现有核心的 MeltingRecipeIndex 按**输出流体**分桶，这里查询的是输入匹配。覆盖 OreDict、可变注册列表、通配/NBT、配方顺序和插件自定义 matches。 |
| **B4 Modular Machinery（结构改造）** | MM 定义解析/注册约 0.30–0.41 GiB，旋转结构及复制约 0.07–0.24 GiB，另有外部几何/动画、燃料索引。可保留一份不可变结构配以方向变换，按需生成热点方向，合并相同几何资源解析。燃料索引按输入和注册版本建立一次。外部模型/动画先探测资源、再交给解码器读取，优先比较每份资源的打开次数；已有共享 mapper，应取初始化与解码的实际完成时刻。菜单的结构和几何对象见 M5。 | 记录每方向使用次数、首次构建和首次组装延迟。覆盖旋转后的 Tile/NBT、坐标、动态结构末端和外部模型切换；机器注册写入按原顺序。 |
| **B5 IC2、NC、AE2 与注册长尾（核验）** | 可见 PreInit 中位数 IC2 1.552 秒、NuclearCraft 1.264 秒、Ender IO 0.937 秒、AE2 0.797 秒。区分 L6 的类型初始化与实际方块状态、工具模型、配方构造。另有 IC2 UU 图约 0.31–0.35 GiB/轮。第 2 轮经 getAll→getInternal 复制 LeanItemStack 约 130 MiB，singleton 及其迭代器各约 71、67 MiB。精确元数据分支可直接把节点交给只读消费者，保留通配分支、NBT 和数量归一化。 | UU 图有后台任务，先确认菜单是否等待。RTG BiomeInit 30/21/19、AstralSorcery.postInit 27/25/25 个主线程样本属于次级入口，须先定位注册数据内的重复工作。 |
| **B6 加载画面、Forgelin、语言与配置（核验）** | Custom Loading Screen 构造 2.270/1.719/1.910 秒，采样中约 1.04 秒进入首次全局资源刷新，第二遍刷新已有优化。可使加载画面只请求所需字体/背景并保留必要监听器。Forgelin 的 Kotlin 单例反射约 0.58 主采样秒，验证已知 Kotlin object 的 INSTANCE 快速路径。Forge 语言加载约 0.53–0.55 GiB，配置解析约 0.20–0.22 GiB。 | 按包、locale、资源代次复用语言片段，按文件记录配置读写及二次解析。保留覆盖优先级、文件编辑、启动画面和 fallback 构造。测首次实际使用，确认没有把启动延迟转嫁给第一帧。 |
| **B7 Kirino 与 ClassGraph（核验）** | Kirino ECS 自报 1.127/0.949/0.933 秒，主线程等 ClassGraph 的 park 为 0.749/0.617/0.621 秒。ClassGraph/ZenUtils 扫描家族约 0.58–0.61 GiB/轮。按请求包前缀、注解、可见 ClassLoader 和 classpath 指纹核对重叠扫描。 | 实装已有扫描缓存。先数缓存未覆盖的输入，只有结果和可见性等价的请求共享扫描结果；测调用方等待与并发 I/O。 |
| **B8 StatList 与统计展示（核验）** | 统计项重建的完整栈约 209/212/211 MiB，0.56 主采样秒。路径含 Universal Tweaks 稀有度、ItemStack 文本与 NBT 字符串。核心目前在构造 StatCrafting 时才捕获已生成的展示组件，可把受控展示模板推迟至实际读取。 | 保留 hover/NBT、语言切换、mod 自定义名字和修改组件副本的行为。动态物品限定至可证明等价的范围。 |
| **B9 低权重但可复现的分配（核验）** | 匠魂额外特性 `RecipeMatch` 组合匹配约 58–60 MiB/轮；OnlyOneItem 配方签名约 40–55 MiB；BASE 语言修正约 18 MiB；Techguns 特效定义约 22 MiB；VLib 指南 JSON 约 18–20 MiB。可分别缩小候选组合、复用签名、按输入版本读取语言和定义、按页物化指南。 | 逐项验证输入变化、结果顺序、动态配方与第一打开界面的延迟。这些是完整路径累计分配，主菜单墙钟贡献较小或尚未单独计时。 |

### 分散在多个阶段的业务

| 来源 | 多轮或交叉证据 | 优化结论与第一项验证 |
|---|---|---|
| B1 ContentTweaker 与后续脚本事件 | ContentTweaker 在 PreInit 主动加载脚本，三轮 46/36/36 个主线程 Java 样本均含 loadScriptInternal，整栈 224/222/221 MiB。Forge 配方事件中另有 64/63/49 个该方法样本，PostInit 再执行移除配方的脚本操作。 | 按脚本 loader、阶段、源文件和编译环境区分解析与编译。分别计编译次数和 refresh 次数；保留每个阶段原有的执行顺序和副作用。 |
| B2 HEI 插件与搜索 | 第四轮 registerPlugins 的直接子调用中，Ender IO Machines 0.40、Vanilla 0.30、JER 0.28、IC2 0.26 主采样秒。另有 0.82 主采样秒含 findClass，已嵌在插件内。 | 先用插件入参与 UID/tooltip 调用次数识别重复展开。tooltip 索引另在工作线程耗时 2.271/1.651/1.563 秒，其末端样本与主线程冻结阶段重叠。三轮菜单前主线程均未采到等待该搜索服务结束的栈；同时测首个搜索和进入世界时的等待。 |
| B8/M7 最后一次注册表冻结 | 三轮 freezeData 整栈 387/392/391 MiB；第 4 轮 1.08 主采样秒。三轮其首末 Java 样本跨度 约 1.50/1.06/0.99 秒，末样本距实际开菜单 0.411/0.320/0.312 秒；88/76/68 个样本中，sync 有 21/21/18，fireRemapEvent 有 63/51/47，其中 StatList.reinit 有 43/33/32。 | 一路比较冻结表与活动表的复制，另一路将展示文本的构造延至实际访问。保留重映射事件的监听器和顺序。两项分别报同一菜单尾段的 A/B 差值。 |
| R10 JourneyMap 资源归档 | PostInit 的 collectTexturesFromArchive 每轮 107/103/106 MiB，9/8/6 个主线程样本；第二轮 49.4 MiB ZipEntry、45.3 MiB byte[]。现有 Mixin 已简化文件名比较。 | 验证能否按归档指纹、条目前缀复用资源名，避免对每个条目反复建 ZipEntry。比较路径集、大小写、覆盖顺序和动态目录修改。与 R8 共用指纹和失效实验。 |
| R11 Wizardry 书架变体 | getBakedBookModels 三轮约 60/57/64 MiB，第四轮池 1.84、主线程 0.28 采样秒。现有 get→构建→putIfAbsent 在并发时可能重复构建。 | 计同代、变体、格式、取纹理函数的重复及并发次数；有重复时再合并同键构建，覆盖递归和资源重载。 |
| B10 Patchouli 与资源语言 | PostInit 的 DMLRelearned 和 LoadComplete 的 Patchouli 各触发一次书籍重载；三轮各自两批均有 114 条语言回退，批内 101 个不同路径，两批多重集合相同。BookRegistry.reload 整栈 70/70/68 MiB。Konkrete 的 FancyMenu 语言解析在原分配余项里另有约 15–16 MiB/轮。 | 对比两批资源与动态注册代次，复用相同书籍、语言、模板的不可变解析结果。有中途消费者时分别发布；同步检查首次打开、翻页、换语言与 F3+T。 |
| R12 两套 OBJ 解码器 | 原分配余项中 Advanced Rocketry 的 RendererWarpCore→WavefrontObject 40–42 MiB/轮，Draconic Additions 使用的 CodeChickenLib OBJParser 30–34 MiB/轮。 | 各自统计同源 OBJ 的复读次数与单次拆词分配；测免正则数值解析或按来源复用不可变顶点模板。保留坐标、绕序、UV、组名和运行期模型姿态。 |

上述尾段的主线程样本、嵌套分配、Patchouli 资源多重集合和 CTM 子路径均可从本地采集复算。[业务路径逐轮数字](../../../nova/startup-baseline-20261124/analysis/document-business-paths.csv) · [复算脚本](../../../nova/startup-baseline-20261124/analysis/document-business-paths.py)

## 菜单存活堆、峰值和进程内存

第六轮对 **29,541,628** 个对象做支配分析。234 个选定目标用原口径重建的保留字节数与库结果逐字节一致，再按本机紧凑对象头、4 字节引用和 8 字节对齐校准。菜单 Java 堆约 1.69 GiB。全堆 3,847,374 个不同的 byte[] 浅大小合计 **722.79 MiB**。按身份覆盖这批数组的各项分别为类缓存 437.85、String.value 205.26、ZIP 中央目录 27.08、字体 25.77、其他 26.83 MiB。以下支配大小还会包含其他对象类型。

| 项目 | 在快照中量到的东西 | 优化候选与验收 |
|---|---|---|
| **M1 相同字符串的物理数组（对照）** | 3,671,623 个 String.value 数组，按 coder 与逐字节相等分出 1,558,651 组。额外 2,112,972 个数组占 72.19 MiB 载荷、103.63 MiB 浅大小。 | 单改 `-XX:+UseStringDeduplication`，使用同版 G1，记录去重日志、后台 CPU、首次菜单和静置后 Full GC。代码端的 WeakStringTable 在资源 ID、模型、HEI 路径反复出现，三轮 247/240/214 个 Java 样本；定位每个调用者的命中率和临时字符串创建，再决定调整规范化时机。 |
| **M2 注解、保护域与类加载器其他持有者（核验）** | LaunchClassLoader 共保留 655.21 MiB，含 L1 的类缓存和 **29.73 MiB pdcache**。45,663 个保护域对应不同 protocol/file，其中 45,615 个带 JAR entry。另有 ASMDataTable.globalAnnotationData **31.63 MiB**（containerAnnotationData 3.72、packageMap 1.53 MiB 单列），11,486 个键、272,429 个集合成员，经 ZenUtils 静态根持有。 | 实装 GenericEventManagerImpl 会按接口名动态查实现。优先试不可变倒排索引、共享注解描述及按需物化，保留可查询的全部注解并验证脚本读者。保护域整体留给类加载器 PR。业务范围内记录 ZenUtils 查询的类型、次数、是否长期持有原表，以及最后一个读者。 |
| **M3 模型注册、派生与方块状态（核验）** | 烘焙模型注册表保留 32.57 MiB。另有 StellarCore 未烘焙模型 cache 10.42 MiB、4,763 项，其中单个 wrapper 的 retextureCache 占 5.60 MiB；aliases 7,883 项。BlockModelShapes 的身份映射约 4.00 MiB。 | 对模型变体、retexture 键的身份/内容与菜单后命中按代次计数。可缩短构建缓存寿命或共享不可变变体，频繁查询可用紧凑索引。跨引用的共享几何不都由注册表独占，验收同时复查物理数组与资源重载后的堆。 |
| **M4 HEI、CraftTweaker 与拼音查询（核验）** | HEI RecipeRegistry 保留 23.96 MiB，其中输入 8.19、输出 4.35、recipeIds 3.00。CraftTweaker 的 MCItemUtils.ITEMLIST 有 35,524 项、9.55 MiB；另一个 ItemStackHelper 的 nova$hashes 约 7.71 MiB。JECharacters 的两棵拼音 TreeSearcher 各约 8.80、5.48 MiB。 | 根据输入/输出索引的实际桶分布考察连续存储；脚本物品表可共享不可变底层，按访问懒建包装。该 hash 缓存由 CraftTweaker 的 JEIAddonPlugin.ingredientHelper 持有，需查 cleanup 后何时重建。HEI 的 wrapperMaps 1.81 MiB、recipeWrappersByCategory 2.70 MiB 已含在 23.96 MiB 内；独立支配的 IngredientRegistry 另有 3.91 MiB。拼音树考虑共享前缀和压实节点，验证简拼、多音字、模糊搜索及首次检索。 |
| **M5 MM 结构与外部渲染资源（核验）** | BlockArrayCache.BLOCK_ARRAY_CACHE_MAP 有 206 个顶层键，保留 12.94 MiB。GeoModelExternalLoader 保留 10.37 MiB，其中 geoModels 为 8.84 MiB。 | 与 B4 合并设计。比较不可变方块组成、坐标方向视图和共享几何，统计组装/渲染/重载后的重建与命中，保留有修改权的调用方。 |
| **M6 字体原始文件（核验）** | CosmicTextRenderer 保留 25.78 MiB，其中 5 个字体 byte[] 有 25.77 MiB。后续 EngineSettings.create 仍会读取。 | 验证按来源和指纹重开、异步引擎创建期间的短期持有以及用后释放。核对字体回退、首屏延迟、再次建引擎和资源包热重载。 |
| **M7 ZIP、注册表及大数组（核验）** | 396 个 ZipFile.Source.cen 数组占 27.08 MiB；另有 StellarCore ZipEntryIndex.INDEXES 保留 8.87 MiB，FileResourcePack.containedPathsByFile 保留 4.79 MiB。RegistryManager.FROZEN 保留 10.75 MiB。注册表活跃/冻结身份映射在旧对象样本中有两对 key/value 数组，共 8.00 MiB。最大单个 TextureAtlasSprite 保留 17.10 MiB，几乎全属 R6。 | 按实际 JAR 和句柄生命周期验证索引重叠，评估轻量路径表。冻结映射只在相等且更新可隔离时共享只读底层，覆盖 remap、快照恢复与身份比较。JDK 中央目录本身和全体纹理需要结合共享关系解释。 |
| **V1 堆提交与启动峰值（配置对照）** | Xms=Xmx=8 GiB，菜单 Full GC 后用堆约 1.69 GiB，堆仍提交 8 GiB。启动已观测用堆峰值 6.28–6.72 GiB。菜单前 RSS 高水位约 7.98–8.27 GiB，Mem Reduct 已启用阈值清理，清理记录接近部分工作集跌落时段，逐次触发者未记录。 | 单独降低 Xms 并保留现有 Xmx，比较扩堆、暂停、启动耗时、峰值和静置后提交。R2/R5 等减少临时缓冲或限制并行度的方案，用同一时间线检查峰值是否发生转移。 |
| **V2 原生内存（直接缓冲已定位）** | 以第 2 轮菜单 NMT 为例，总提交约 8.92 GiB，含 8 GiB Java 堆。Metaspace + Class 约 359 MiB、GC 约 226 MiB、Code 约 167 MiB、Symbol 约 52 MiB。Other 菜单约 20 MiB、历史峰约 630 MiB；JFR/NMT 本身也有成本。 | Other 早期峰值已经定位到 Actinium 内嵌 GTNHLib 的三个临时 direct buffer，下一节给出字节与创建者的核对。其余原生类别分别复测。工作集谷值受系统修剪影响，GPU 驱动和显存需单独观测。 |

### G1 字符串去重的实机结果

另用当前开发客户端完成一次预热和六轮对照。六轮实际加载的 280 个 mod JAR 与原基线哈希相同，本次系统使用 5 GiB 初始堆和上限；只在同组条件内比较。以下为每组三轮的中位数。

| 指标 | 去重关闭 | 去重开启 |
|---|---:|---:|
| JVM 至首次菜单 | 105.958 秒 | 109.314 秒 |
| 菜单诊断 Full GC 后存活堆 | 1,732.95 MiB | 1,638.97 MiB |
| 启动已观测用堆峰 | 4.513 GiB | 4.495 GiB |
| 菜单去重表原生提交 | 约 0 MiB | 41.24 MiB |
| 菜单 NMT 总提交 | 6,003.90 MiB | 6,052.93 MiB |
| 启动进程 RSS 高水位 | 6.365 GiB | 6.458 GiB |

三对相邻控制/实验轮的菜单存活堆分别减少 95.65 / 96.58 / 92.20 MiB；启动时间差分别为 −5.302 / +3.617 / +6.102 秒。由此确认 M1 可缩减此客户端的菜单存活堆，启动提速和进程内存下降均未获本组支持。当前默认值保留关闭。[完整逐轮对照及采集口径](../../../nova/startup-m1-20261125/results.md) · [原始数字](../../../nova/startup-m1-20261125/comparison.csv)

### 峰值发生在哪一段

三轮全程最高用堆都出现在一次 G1 年轻代疏散前。时间零点取 JFR 的 JVM 启动时间。为追查谁在峰值前持续分配，将上一次 GC 结束至本次峰值前的分配按最内层匹配的业务栈归类；只使用该区间的样本。GC 前后堆值按同一个 gcId 配对。

| 轮次 | JVM 启动后及阶段 | 当次 GC 前 → 后（GiB） | 停顿 | 上次 GC 后至峰值的间隔与主要分配 |
|---|---|---|---:|---|
| 1 | 123.174 秒，HEI 结束附近 | 6.721 → 2.368 | 约 215 ms | 17.126 秒，样本权重合计 4.60 GiB。HEI 1,381 MiB、类加载与转换 1,142 MiB、配方 509 MiB、UU 317 MiB、MM 299 MiB。 |
| 2 | 93.261 秒，PostInit 至 HEI 开始 | 6.287 → 2.124 | 约 212 ms | 14.173 秒，合计 4.20 GiB。类加载与转换 1,468 MiB、配方 888 MiB、脚本 418 MiB、UU 347 MiB、MM 295 MiB。 |
| 3 | 90.829 秒，PostInit 至 HEI 开始 | 6.281 → 2.127 | 约 197 ms | 13.914 秒，合计 4.15 GiB。类加载与转换 1,491 MiB、配方 907 MiB、脚本 407 MiB、UU 303 MiB、MM 229 MiB。 |

模型窗口在三轮都密集分配，区间最高用堆 **6.20/6.23/6.22 GiB**，各有 4 次 GC、合计 **0.545/0.621/0.620 秒停顿**。全程峰值要同时看后期业务：后两轮配方、脚本、UU 和 MM 在同一个 GC 周期内合计约 1.90/1.80 GiB 的分配权重。这里量到的是创建速度与时间重合，具体对象存活到峰值的数量需要峰值附近的保留信息。NMT Other 的离散采样最高 **629.60/420.23/629.60 MiB**，均在 Main 至首个 Construction 之间；第二轮 NMT 自报历史峰约 630 MiB。第一轮在 Other 峰值之后 58 ms，JFR DirectBufferStatistics 记录到 26 个 direct buffer、628.2 MiB 已用；约 5.4 秒后降至 16.4 MiB。其余两轮的默认 5 秒 direct pool 取样跨过峰值。该条证据把早期 Other 峰的首要测点收窄到直接缓冲的创建者与存活窗口。[直接缓冲和最近 NMT 样本](../../../nova/startup-baseline-20261124/analysis/direct-buffer-vs-nmt.csv) · [复算程序](../../../nova/startup-baseline-20261124/analysis/direct-buffer-vs-nmt.py)[14 段分配与 GC](../../../nova/startup-baseline-20261124/analysis/stage-allocation-gc.csv) · [峰前一个 GC 周期的分配明细](../../../nova/startup-baseline-20261124/analysis/peak-cycle-allocation.csv) · [复算方法](../../../nova/startup-baseline-20261124/analysis/document-stage-costs.py)

**V2 的具体改动位置。** Actinium 内嵌 GTNHLib 的 `com.mitchej123.lwjgl.lwjgl2.memory.MemoryUtilities` 在类初始化时依次探测 NIO 缓冲的 position、limit、capacity 字段。三个探针都给私有 `newDirectByteBuffer(-1L, MAGIC_CAPACITY)` 传入 `0x0D15EA5E`。实装包装方法忽略地址参数，直接执行 `ByteBuffer.allocateDirect(capacity)`，所以每次申请 **219,540,062 字节**，合计 **658,620,186 字节（628.11 MiB）**。第一轮 direct pool 高点总容量 658,714,498 字节，仅多 94,312 字节；第三轮 JFR 的 Uniforms 初始化栈进入该类的时间距 Other 高点约 16 ms，随后 Reference Handler 出现 DirectByteBuffer 释放栈。标准 LWJGL 的同名常数走另一种包装实现。开发客户端图标只有 16×16 和 32×32 两张，常规 RGBA 合计 5 KiB。

另一次 20 ms 直接缓冲取样与方法记录在同一启动中看到这三次申请分别耗时 53.79、50.40、52.04 ms，合计 156.23 ms。三块总容量逐级增长，JVM 启动后 18.773 秒观测到池总容量已降至 89,024 字节。[分配方法与释放时间线](../../../nova/startup-m1-20261125/diagnostic/results.md)

Actinium 的单项构建用两只共 1,252 字节的小缓冲，在各自合法的 mark、position、limit、capacity 组合上交叉比对字段。保留运行时布局探测和原有识别失败处理。默认和紧凑对象头下偏移各自唯一，七种缓冲视图通过 reset、clear 等验证。三组实机对照，Construction 前直接缓冲高点均从 **628.20 降至 16.43 MiB**；NMT Other 历史高点从 **629.61 降至 29.94–31.23 MiB**。同期启动 RSS 高水位有升有降，菜单 Full GC 中位数相差 0.10 MiB。首次菜单的三组差值为 +0.747 / −0.857 / −2.562 秒。可确认的收益集中在堆外瞬时峰值。[早期归因与小缓冲验证](../../../nova/startup-baseline-20261124/analysis/early-direct-attribution.md) · [实验分支源码](../../../clone/Actinium-startup-buffer-offsets/GTNHLib/src/main/java/com/mitchej123/lwjgl/lwjgl2/memory/MemoryUtilities.java) · [实机对照](../../../nova/startup-resource-ab-20261125/analysis/medians.json)

**其余有具体持有者的对象也纳入候选。** 下列项目按操作合并，防止同一模型的数组、节点及外层注册表被多个优化重复计作收益。

| 项目 | 证据 | 可验证的改动 |
|---|---|---|
| **M8 渲染几何与模型实例** | Nova 几何包的 24 种非数组类有 1,091,794 个实例，仅对象本体 28.13 MiB。ThermalDynamics 的 RenderDuct 镜像支配 11.22 MiB；实体渲染表 12.41 MiB，方块实体渲染表 6.30 MiB。 | R2 同时评估短序列、形状、材质 wrapper 的数量。按实际 mod 模型核对朝向/面连接的重复几何，尝试基础网格加变换和稠密索引。姿态、动态渲染与第三方修改仍需独立实例。 |
| **M9 具体配方、脚本和补丁表** | Ender IO 合金 lookup 4.42 MiB、匠魂 tableCastRegistry 4.54 MiB、Ingredient 弱集合 4.15 MiB、CrTGlobalEnvironment 4.77 MiB、ZenUtils mappers 3.93 MiB、ClassPatchManager 9.07 MiB。 | 对配方按稳定 ID 缩小候选，保留 NBT、顺序、矿辞和运行期增删。对脚本/映射记录最后读者和重载行为。ClassPatchManager 归入已有类加载器 PR，其 9.07 MiB 只用于解释堆。 |
| **M10 纹理查找、着色器与其他命名缓存** | Celeritas 图集四叉树 5.66 MiB、Angelica GLSL DFA 5.40 MiB、Techguns 渲染表 3.93 MiB、Nova 资源名字符串表 3.50 MiB、LibrarianLib prefixes 2.70 MiB。原未分类启动分配中 Angelica 的解析路径另占 140–146 MiB/轮。 | 优先量每个表的条目数、填充率、命中与代次。对不可变前缀/坐标测试紧凑表示，对 shader 解析测试按输入复用；覆盖换 shader、重载、首次绘制及模组自行注册。 |

全堆已校准约 **1,704.61 MiB**。234 个选定支配目标中，47 个顶层目标的并集为 **910.05 MiB（53.39%）**，其余 **794.56 MiB** 位于这些子树之外，含共享对象及其他持有者。这个比例描述支配树的覆盖面；前面的 byte[] 表则逐一统计全堆的物理数组。两个值得细分的持有者是 IC2 方块状态 index（3.30 MiB，支配 118,946 个对象），以及 CrTGlobalEnvironment 中 SymbolScriptReference 的 3.82 MiB 子树。前者可核对不可变属性组合能否用连续编号；后者先查脚本重载、诊断与惰性查询的读者。另有 PositionTextureVertex 525,120 个、浅大小 12.02 MiB，CodeChickenLib vec 本体 12.02 MiB，GeckoLib 的几何本体 4.94 MiB，可与 R2/R12/M8 联查模型所有者。[校准记录](../../../nova/startup-baseline-20261124/analysis/full-calibration.log) · [字段持有关系](../../../nova/startup-baseline-20261124/analysis/full-heap/owners.tsv)

还有几条会改变改造切入点的**跨业务调用**。以下是整栈包含该路径的分配，与前表的互斥家族交叉，不再加到总量。

- **IC2 预初始化与 JAR manifest。** IC2 PreInit 栈每轮约 923–928 MiB，其中约 600–646 MiB 同时进入 URLJarFile.getManifest。这部分由定义类时取得归档清单触发，归入加载器 PR。IC2 业务候选只针对实际方块状态和 UU 图，报告将两者分别计量。
- **ZenUtils 的另外一次查类。** classpath visitor 约 128–132 MiB/轮，调用经 ZipFS、按路径判断存在再读类字节。可以在 visitor 范围内按类路径次序建立条目→首个来源索引，覆盖目录、后加 URL、脚本环境重建。注册反射、脚本所需字节、JVM 定义类三类消费者的可见来源必须分别核对。
- **MM 的工作池。** 按整栈包含关系，BlockArray 约 201–342 MiB、外部几何约 288–351 MiB、燃料枚举约 134–136 MiB/轮。FuelItemHelper 枚举物品与创意分页并查燃烧时间。可复用本注册代次的候选和已验证的纯查询，保留动态燃料钩子。池中同时在做的结构旋转与外部模型解码通过 B4/M5 解决。
- **资源名和高频容器。** 含核心字符串表的整栈约 56–75 MiB/轮，含 ResourceLocation 的约 168–195 MiB。资源定位、模型包与配方帮助器均会访问。ResourceStringCodec 拆分时会生成临时 String[]；可给已知调用点提供直接读取命名空间与路径的接口。已有弱表分 32 片，三轮 lookup 锁等待的主线程部分仅 0.088/0.164/0.071 秒。先按调用方统计重复串，再决定在哪个生产端复用。全局统计的 reflect.Method、ASM 树和弱键数量与单个缓存支配大小有重叠。

## 启动分配核对

完整 JFR 栈按最近业务来源归组，每个样本只进入一个主分类。下表是三轮加权分配的 GiB，保留三位小数。它覆盖所有主分类，便于检查哪些来源已在前文处理。

| 业务来源 | 第 1 轮 | 第 2 轮 | 第 3 轮 | 清单位置 |
|---|---:|---:|---:|---|
| 类加载与转换 | 15.292 | 15.419 | 15.197 | L1–L4、L6 |
| 模型发现与烘焙 | 6.737 | 6.649 | 6.824 | R1–R4、M3、M8 |
| 模组发现 | 2.880 | 2.864 | 2.885 | L5、M2 |
| 纹理与图集 | 2.069 | 2.069 | 1.979 | R5–R6、R9 |
| HEI 配方与原料 | 1.647 | 1.584 | 1.593 | B2、M4 |
| 配方注册与处理 | 0.963 | 0.963 | 0.984 | B1–B3、B9、M9 |
| MM 机器、结构与配方 | 0.816 | 0.891 | 0.753 | B4、M5 |
| 注册表与方块状态 | 0.631 | 0.576 | 0.590 | L6、B5、M7 |
| ZenScript / CraftTweaker | 0.596 | 0.568 | 0.581 | B1、M4、M9 |
| ClassGraph / ZenUtils 扫描 | 0.596 | 0.614 | 0.583 | B7、ZenUtils visitor |
| Forge 语言加载 | 0.530 | 0.549 | 0.527 | B6、B9 |
| 加载画面 | 0.310 | 0.305 | 0.313 | B6 |
| IC2 UU 图 | 0.309 | 0.349 | 0.306 | B5 |
| FML 事件注册/派发 | 0.229 | 0.196 | 0.207 | L6 |
| 其他资源包 I/O | 0.222 | 0.261 | 0.236 | R7–R8 |
| 配置解析 | 0.220 | 0.205 | 0.203 | B6 |
| 字体 | 0.144 | 0.140 | 0.114 | M6 |
| 初次分类未识别 | 2.321 | 2.161 | 2.180 | 下述二次核对 |

对最后一行再查完整调用栈，仅在这一行内按确定的业务入口分配，依次找到 **1.142 / 1.129 / 1.205 GiB**。包括统计展示、匠魂合金匹配和材质精灵、CTM 决策、GLSL 解析、VintageFix 缓存、资源索引以及 B9 长尾。额外归类后，本步过后三轮尚有 **1.179 / 1.032 / 0.976 GiB** 没有可靠业务归属；此时总分配权重的已归因比例为 **96.77% / 97.16% / 97.29%**。例如有些工作池栈仅显示通用并行集合拆分，缺少请求方。该比例量的是 JFR 分配样本权重。启动墙钟用前面的 14 个区间核对，菜单内存用全堆物理对象和持有关系核对。

再对上述剩余栈按字面匹配，依次找出 Advanced Rocketry OBJ、CodeChickenLib OBJ、Patchouli 书籍、Konkrete 语言、Nova 归档索引、Nova/星核目录索引。在**原余项内**额外解释 **201.04 / 205.34 / 211.24 MiB**。三轮最终剩余 **0.983 / 0.831 / 0.769 GiB**，累计已归因权重 **97.31% / 97.71% / 97.87%**。其中两类资源索引分别额外解释约 31–32 与 50–55 MiB/轮，归入 R8 的按归档/目录复用决策。线程时间和全堆持有者有各自的覆盖口径。[逐轮总量与余量](../../../nova/startup-baseline-20261124/analysis/inventory-residual-final.csv) · [六组匹配规则](../../../nova/startup-baseline-20261124/analysis/document-residual.py)

## 统一验收

1. **建立可配对的对照。** 以实装 522b95e6 为基线，或用同一工作树、同一构建产物只切换待测功能。保留 JDK、mod 与资源包指纹、已有缓存状态和采集参数。交替跑原版与修改版，分别列出每轮、范围及中位数。首轮额外开销和 mcmpu 准备单列；只有端点差持续超过同条件波动，才归为已验证的启动收益。
2. **同时观察时间和空间。** 报首次打开菜单、受影响子阶段、主线程等待、启动分配和用堆峰值。菜单采用相同静置时长和 Full GC 端点，测存活堆、NMT 提交、RSS 高水位及候选所有者。需要验证结构性释放时补采同条件 HPROF。单个数组载荷、支配大小和线程采样秒各按原口径复测。
3. **按改动覆盖行为。** 资源与模型检查图集、CTM 图层、顶点、动画像素、动态覆盖和 F3+T。配方与脚本比较注册 ID、原料顺序、NBT、查询及脚本重载。类与注册表检查延迟加载、包来源、脚本生成类和进入世界。延迟初始化还要测首次搜索、首个区块或首次显示模型的卡顿，以及多次重载后的存活堆。
4. **记录当前证据边界。** 主菜单后首次可交互帧、世界加载和 GPU 显存尚无这组基线；启动器首轮缺少逐步骤时钟，PreInit 后半缺逐模组 TRACE。后台网络 native 等待主要在工作线程，暂缺主线程依赖证据。对这些窗口使用定点计时、队列等待或所有者计数，避免把未定位的整段墙钟算作预计收益。

## 证据入口

采集目录位于本机 `F:\nova\startup-baseline-20261124`。以下相对路径适用于当前工作区及客户端目录布局。

后续验证单列在[九轮单项时间与内存结果](../../../nova/startup-resource-ab-20261125/results.md)、[R6 原始像素持有者](../../../nova/startup-resource-ab-20261125/r6-diagnostic/results.md)、[F3+T 与完整清单对照](../../../nova/startup-resource-ab-20261125/analysis/functional-series/report.md)和[配方数量波动的逐键归因](../../../nova/startup-resource-ab-20261125/analysis/keys-attribution.md)。完成后再次核对开发客户端中 280 个已安装 JAR 和启动配置，其哈希均与实验前一致。[客户端核对记录](../../../nova/startup-resource-ab-20261125/analysis/final-verification.json)

- 启动全程、归因边界和模块内部时钟见[综合报告](../../../nova/startup-baseline-20261124/deep-report.html)、[14 段时间线及日志行号](../../../nova/startup-baseline-20261124/analysis/startup-wall-stages.csv)、[每模组 Forge 计时](../../../nova/startup-baseline-20261124/analysis/startup-time-flare-fml-bars.csv)及[时间覆盖审计](../../../nova/startup-baseline-20261124/analysis/inventory-time-audit.md)。
- 跨线程和跨 mod 的工作见[主线程与工作池分类](../../../nova/startup-baseline-20261124/analysis/flare-family-seconds.csv)、[有方向的父子调用](../../../nova/startup-baseline-20261124/analysis/startup-time-ordered-branches.csv)、[JFR 分配分类原始字节](../../../nova/startup-baseline-20261124/analysis/allocation-families/cross.json)、[业务路径交集](../../../nova/startup-baseline-20261124/analysis/inventory-business-weights.csv)、[算法与实装调用点复核](../../../nova/startup-baseline-20261124/analysis/inventory-business-audit.md)和[剩余分配的二次核对](../../../nova/startup-baseline-20261124/analysis/inventory-residual.csv)。
- 菜单所有者见[全堆审计](../../../nova/startup-baseline-20261124/analysis/inventory-memory-audit.md)、[校准支配大小](../../../nova/startup-baseline-20261124/analysis/full-heap/calibrated.tsv)、[静态持有者](../../../nova/startup-baseline-20261124/analysis/full-heap/static-owners.tsv)、[数组所有者](../../../nova/startup-baseline-20261124/analysis/full-heap/byte-owners.tsv)、[字符串内容去重](../../../nova/startup-baseline-20261124/analysis/full-heap/string-duplicates.tsv)及[手工复算方法](../../../nova/startup-baseline-20261124/analysis/full-heap/reproduce.txt)。
- 核心仓库的改动入口包括[模型元数据](../src/main/java/github/kasuminova/novaeng/client/resource/ModelJsonMetadataIndex.java)、[紧凑几何](../src/main/java/github/kasuminova/novaeng/client/geometry/block/BlockGeometryPages.java)、[动画帧](../src/main/java/github/kasuminova/novaeng/client/memory/OwnedAnimationFrames.java)、[HEI 配方](../src/main/java/github/kasuminova/novaeng/common/performance/HeiRecipeInsertion.java)、[匠魂现有流体索引](../src/main/java/github/kasuminova/novaeng/common/performance/MeltingRecipeIndex.java)与[统计项展示](../src/main/java/github/kasuminova/novaeng/mixin/minecraft/MixinStatListDisplayMemory.java)。
