# StarForge 项目协作规则

## 范围与技术

- 默认中文，直接执行明确修改；交付说明重要改动、实际验证和剩余风险。当前用户要求优先，不把建议扩写为需求。
- 源码目录 `D:/codex/StarForge`。开工读本文件、pom、相关源码、资源与测试，保留已有未提交改动；README 仍有主手展示、旧分级石等历史段落，不能据此恢复旧行为。
- Java 25、Maven、包名 `ym.starforge`，Paper API `26.2.build.121-stable`；目标 Paper/Purpur/Folia，不使用 NMS，具体版本以 pom 为准。
- 本地编译依赖 Vault、PlaceholderAPI 2.12.3、CraftEngine 26.9.2、ItemHub；路径通过 vault.jar/papi.jar/ce.jar/itemhub.jar 覆盖，第三方插件与服务端 API 不打包。
- 默认搜索 src/、craftengine/、pom.xml、README.md、AGENTS.md；target 为构建输出，dist 为交付副本，artifacts 与 .verification 为工具、证据或独立测试服，不能当成运行源码。
- 跨项目 API 变更先读 `D:/codex/ItemHub`、`D:/codex/AsterStats` 的项目规则，不做无关重构。
- 使用 mc-plugin-neuron，以项目 ID starforge 执行 begin_task，提交真实检查后 validate_task/finish_task；缺验证记 pending，未连接则明确说明。

## 代码职责

- StarForge.java 只装配和管理生命周期；api 是 Bukkit ServicesManager 契约；config 负责异步读取、校验、迁移与配置快照。
- item 管武器识别、PDC、原始物品视图与幂等展示；transaction 管计划、随机结果、扣费扣料、回滚与异步日志。
- integration 隔离 CE/ItemHub/AsterStats/PAPI；gui 管会话、输入槽与交互；command 只整理参数、权限并调服务；text 统一渲染。
- 不继续往入口或 ForgeMenus 堆材料算法、属性计算及 IO，不新增万能 Manager/Utils。独立职责归到现有模块。

## 强化规则与武器识别

- 默认 +0～+15，上限可配；levels.yml 按目标等级配置金币、最低材料数、概率备用值、成长和降级概率。
- +1～+10 失败不降级，+11 起按规则概率降一级；任何失败都不销毁装备。保底连续失败次数绑定单件武器，成功重置。
- 自由强化 + 配置覆盖：原版剑、斧，以及符合正主手攻击条件的 ItemHub/AsterStats 自定义武器，无需逐件注册；普通非武器默认禁止。
- API 或 `/sf mark on [标记]` 可以扩展可强化物品；任意匹配黑名单始终优先。
- weapons.yml schema 2 只放特殊规则：itemhub/vanilla/marker 选择器、blacklist、max-level、growth-multiplier 和按等级部分覆盖；未配置武器用默认等级规则。
- 多规则命中时除黑名单外，按 ItemHub ID > marker > 材质选一项，不叠加多个倍率；降低上限不截断已存等级。
- 旧 schema 1 迁移须保留中文注释、未知字段、编码/换行和旧武器兼容，先备份、原子替换、可重复执行；失败保留旧快照。

## 属性与 Lore 契约

- 基础攻击优先读 AsterStats 单件基础属性（PDC > ItemHub 定义 > Lore），其次最新 ItemHub 定义及原武器属性；排除自身强化贡献和修饰器。
- 旧 base-attack 仅为对应旧登记武器的最后备用值，不能作为所有武器统一基础攻击。
- 额外攻击 =（累计固定成长 + 最新基础攻击 × 累计百分比成长）× 武器成长倍率。逐级累加、不复利，不从旧最终攻击继续叠加。
- 当前默认每级 +0.20 固定攻击与基础攻击的 +5%；倍率 1、基础攻击 2 时每级额外 +0.30，+15 额外 +4.50。只是默认示例，运行时以配置为准。
- 当前仅实现 attack；暴击率、暴击伤害只预留扩展，不宣称已生效。
- attribute-backend 唯一选择 asterstats/vanilla/api。AsterStats 合并 API 额外贡献；vanilla 只管理 starforge:attack 并保留材质默认攻击/攻速；api 交消费插件处理，避免重复计算。
- StarForgeApi 提供 level、failures、contribution、refresh、baseItem、canEnhance、maxLevel、markWeapon；物品变更方法返回副本，调用方在所有权线程应用。
- baseItem 只剥除自身精确记录的名称后缀、Lore 和修饰器，不调用 ItemHub/AsterStats 服务，避免递归。
- 名称显示强化等级；原攻击行末追加 `(+加成)`，例如 `攻击力 » +2 (+1.5)`；无匹配行时加简短攻击加成行，+0 不加强化 Lore。
- 精确组件标记 starforge.lore.attack_bonus 用于幂等重建；只移除自身内容，保留其他插件后改的名称、Lore、附魔、模型、耐久与额外 PDC。
- ItemHub 完整刷新走 Modifier API，Lore-only 走独立 ItemHubLoreDecoratorApi；基础及外部追加行完成后装饰攻击行。原始属性更新后按新基础重算，ItemHub 管理字段仍遵循其定义。
- 桥接改动核对 ItemHub、AsterStats、StarForge 配套 API；不能宣称提供接口就等于其他插件已接入。
- 等级、保底与单件身份使用 starforge:level、starforge:failures、starforge:weapon_uid，不从名称/Lore 反推。

## CE 材料、材质与配置归属

- 实服资源根目录 `C:/Users/32633/Desktop/leaf/plugins/CraftEngine/resources/强化石`；工程副本 `craftengine/强化石`。实服可能已有用户文案，不能用工程副本整目录覆盖。
- 材料 name、Lore、模型、贴图唯一来源是 CE。StarForge 数据目录和发行 JAR 不存、不释放 materials.yml，也不重复维护材料文案。
- config.yml 的 materials-file 指向 `../CraftEngine/resources/强化石/starforge-materials.yml`，外部文件只管材料来源和强化规则。
- CE `configuration/items.yml` 管 name/Lore/texture；`configuration/categories.yml` 管“强化材料”分类，参考服务器货币分类。这不是新增一种 StarForge/ItemHub 来源类型。
- 三种强化石当前均默认全等级通用、成功提升 1 级，可混放：starforge:stone 初级单颗 10%，starforge:intermediate_stone 中级 20%，starforge:advanced_stone 高级 35%。不恢复初级仅 0→1 且必成的旧限制。
- 另有 starforge:lucky 幸运符、starforge:protect 保护符；starforge:gui_blank 仅内部透明交互使用，不列入材料分类。
- 五种材料贴图和透明图当前为 32×32 RGBA PNG，位于 `resourcepack/assets/starforge/textures/item/`；用户允许 32×32 或 64×64，保持像素网格及透明边缘。菜单背景另用尺寸，不能混淆。
- 每个材料只选 vanilla/craft-engine/binding 一种来源；原版严格匹配无额外元数据物品，防止误吞其他自定义物品。
- 自定义 enhancement 仍支持 min-target/max-target/steps/chance；省略范围为全等级，省略单颗 chance 用目标等级概率。混放须检查各堆范围与提升步数一致。
- 按槽位从左到右、从上到下累加单颗概率；保底与实际可用幸运只算一次，上限 100%。达到 100% 且满足最低材料数即停扣，多余石头保留；不足 100% 时用实际有效石头计算。
- 使用十进制累加，避免 10×10% 浮点误差多扣一颗。例：最低数量不超过 10、无额外加成时，放 64 颗初级只扣 10、剩 54；幸运实际增加 10 个百分点且最低数不超过 9 时只扣 9。
- 幸运/保护是两个独立开关：启用且背包有才消耗一张并生效；没有就跳过，不影响强化，不虚加幸运概率。保护只防降级，不保证成功。
- `/锻造台` 与 `/warp dzt` 为服务器已有传送指令，仅在材料说明引用，不由 StarForge 注册，不擅自增加右键传送。

## 当前玩家菜单与像素 UI

- 玩家手动把单件武器放武器槽，强化石放中间九格；GUI 真正托管输入，不是主手副本展示，不自动从背包抽取强化石。幸运/保护符仍留背包。
- 玩家和管理菜单分别为 gui/main.yml、gui/admin.yml，一文件一菜单，Shape 每行恰好 9 字符、1～6 行，非空符号全部有映射。
- 当前玩家 Shape：

```text
FFFFFFFFF
FFFSSSFFF
FWFSSSFCF
FFFSSSFFF
FFLFFFPXF
```

- 零基槽位：武器 W=19；强化石 S=12、13、14、21、22、23、30、31、32；确认 C=25；幸运 L=38；保护 P=42；关闭 X=43。业务以 Shape 映射为源，不硬编码这些索引。
- 除输入物品、符文图标外使用像素背景和透明按钮，不恢复满屏玻璃或实物按钮；底部两个符文装饰留空，不连成一条。
- 预览与执行共用 ForgePlan；展示当前/下级攻击、实际概率、金币、放入/实扣/返还数量。
- 武器仅允许单件；左右键和拖拽只允许合法输入。未实现的 Shift、数字键、双击等批量动作继续拦截，避免越槽与复制。
- 关闭、退出、重载时返还托管输入，先清空槽再入背包，溢出掉落；回滚同时恢复本次 GUI 输入和玩家背包。
- 提交强化后关闭 UI、返还武器和剩余石头、显示实际成功/失败 Title，30 tick 后重开空菜单并保留符文开关；玩家需重新放入武器。校验或扣费失败保持原菜单。
- 重绘、点击与重开检查插件有效性、在线、权限、配置和会话身份；重开不能覆盖玩家另开的菜单。
- 背景 `resourcepack/assets/starforge/textures/font/gui/forge_menu.png` 为 176×110，CE 图片 ID starforge:forge_menu，当前 height=110、ascent=13；改 Shape 必须同步图形位置并检查悬浮/点击对齐。
- 标题 starforge:ui 引用已有 xerr_font:fusion_pixel_quest_12_sc.ttf，size=12、oversample=1；来源为 leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font，不能复制修改字体原件。
- 本会话已允许直接生图，素材任务可沿用授权并遵循对应技能；普通业务、配置和文案修改不触发重新制作素材。

## 资源、文本与指令

- config.yml 只放全局上限、保底、幸运、公告等级、属性后端与外部材料路径；等级、武器、GUI、语言放各自文件，不新增无效配置。
- 默认资源仅在全新数据目录首次释放；已有目录不覆盖、不补回删掉的文件，不在每次启动重新释放。缺文件或无效配置明确报路径与原因，失败保留旧快照。
- YAML 带 schema-version，使用空格和块式集合；新增/修改文案用单引号，数值布尔保持类型。迁移保留注释、未知字段和用户配置。
- 玩家反馈在 lang/zh_cn.yml，GUI title/name/lore 在对应 GUI 文件；RGB MiniMessage。Texts 递归保证可见子组件 italic=false，不往 GUI 模板批量补斜体标签。
- GUI 的 CE 图标保留原名称/Lore，只附加状态和 toggle-lore，不复制 CE 材料文案到 StarForge。
- 普通功能统一 starforge.use，管理员统一 starforge.admin，与 plugin.yml 一致，不额外拆普通子权限。
- `/sf`（`/starforge`）打开强化；管理员功能 admin/give/bind/set/mark/reload。指令变化同步帮助、补全、权限与 README，未知或多余参数不得误触发。
- PAPI level/failures 从玩家事件维护的缓存读取，不在占位符请求线程访问背包。

## 事务、线程与生命周期

- 文件、迁移和日志异步；配置应用在 global；玩家背包、GUI、扣料与效果在 player/entity scheduler。禁止阻塞等待或异步直接访问受限 API。
- 每玩家互斥和点击冷却；准备前、扣费后核对玩家、权限、配置、武器和石头快照。完整准备结果及扣料方案后才收费。
- 收费失败不动材料；写入失败恢复本次输入与背包并退款。重入改变时不能用旧快照覆盖无关新物品；不自动重试扣费，回滚/退款异常记录事务、玩家和原异常。
- operations.log 单线程异步记录 PREPARED/DECLINED/COMMITTED/ROLLED_BACK/REFUND_FAILED 等，停用关闭并处理已提交工作。
- Vault 同步 API 与玩家存档没有跨系统事务，不能宣称断电、崩溃、扣费后抛错或退款失败时绝对不丢钱；经济提供者须线程安全且快速返回。
- 停用清理任务、会话、服务、外部注册、PAPI 缓存与日志线程；迟到回调不能继续应用。Folia 禁用期间跨玩家托管物品返还尚待实际验证，不能宣称完整覆盖。

## 构建、部署与验证

- 构建进程用 `C:/Program Files/Zulu/zulu-25`；Maven 可用 `C:/Users/32633/.codex/tmp/apache-maven-3.9.11/bin/mvn.cmd`，执行前检查路径及所需 settings，不写死失效临时文件。
- 运行相关测试及一次必要 `mvn -B -ntp package`，不随意 clean 清验证证据。产物 target/StarForge-1.0.0.jar；交付检查不含服务器 API、第三方插件、测试、探针或素材原稿。
- 当前实服 `C:/Users/32633/Desktop/leaf`，已知运行过 Purpur 26.3；编译版本不等于运行版本，验收前重新确认。
- 本会话已授权暂存 leaf/plugins/update/StarForge-1.0.0.jar；不热替换已加载的根目录 JAR，不自行重启服务器。改实服配置/CE 资源先备份到 leaf/.codex-backups，只改相关文件与字段。
- Java 逻辑修改无需重建 CE 包；CE 定义/图片/字体变更按实际服务器执行 `/ce workflow default` 并下发资源包。文件落盘不等于客户端生效，CE 新定义也不等于已有旧物品文案已同步。
- 文档任务检查源码一致性、路径和格式，无需重跑构建；业务任务跑相关测试和必要构建，发布核对 JAR 清单与暂存哈希。
- 相关回归包含：64 石停扣、混放顺序与精度、最低材料数、缺符不阻止、保底、扣费失败、剩余返还、关闭/Title/重开、ItemHub 更新后加成不重复。
- 单测/构建不代表客户端、Folia 并发或真实经济事务通过；旧主手模式探针不能替代当前 GUI 托管流程测试。Folia 另需兼容的 Vault 和线程安全经济插件。
- Git 远端 https://github.com/TuZiz/StarForge；上传仅在用户要求时执行，不把本地更新称为 GitHub 已同步。
