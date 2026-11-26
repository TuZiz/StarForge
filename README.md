# StarForge 第一版

## 默认配置释放（2026-11-17）

源码 `src/main/resources/` 包含12份默认配置。插件目录不存在或只有空目录结构时，首次启动释放 `config.yml`、`levels.yml`、`weapons.yml`、`lang/zh_cn.yml`、`recipes.yml`、`workstations.yml`，并安装六份GUI配置。已修复提前创建空插件目录导致六份主配置漏放的问题。

已有非空配置目录仍按管理员配置保留，不自动补回误删的主配置；缺失的GUI继续按既有规则补齐，已有文件和无效配置不会被默认文件替换。首装在写文件前读取全部发行资源，缺失发行文件会报告路径并保留可重试状态，不留下半份目标文件。

强化材料的规则、名称和贴图仍归CraftEngine；`materials-file` 指向CE目录，不在StarForge重新生成 `materials.yml`。CE资源源码位于 `craftengine/强化石/`。订单、解锁和日志是运行时数据，不作为默认配置打包。114项测试及Purpur26.3/Java25空目录/再次启动配置保留验收通过；记录见 `artifacts/default-resources-delivery.json`。

## 防具强化（2026-11-16）

星芽护额、轻甲、护腿、短靴与原版四槽防具可放入现有强化槽，每件独立等级和保底。武器提升攻击，防具提升防御；生命、法防与ItemHub随机品质保持不变。材料、概率、费用、幸运/保护符和结果重开沿用现有流程。

`levels.yml` 的 `defense-flat` / `defense-percent` 独立控制防具成长，默认每级 `0.20 + 基础防御 × 0.05`，逐级累加、不复利。基础防御按对应穿戴槽读取，优先使用AsterStats与ItemHub品质快照。旧等级配置缺字段时仅在内存采用默认，不重写文件。`weapons.yml` 的选择器、黑名单、上限、成长倍率及等级覆盖也适用于防具。

本功能需配套更新StarForge与AsterStats并正常重启；无需重新生成CE资源包。AsterStats只在正确穿戴槽合并防御强化，拿在手中不会获得强化防御；`vanilla`后端则使用对应槽的原生护甲修饰器。

## CE 工作台模型与成品动画（2026-11-20）

星轨强化台以开口铜环围绕双尖青晶，炉火锻造台由开放炉膛、铁砧和锤架组成。两台约占 3×2 格，上方带像素名称框。可编辑工程和视口预览在 `artifacts/workstations-design-preview`，发行资源在 `craftengine/星锻工作台`。CE 分类为 `starforge:workstations`，使用 `/ce item get starforge:enhancement_station` 或 `/ce item get starforge:crafting_station` 获取并放置。

通过实体锻造台开始制作后，附近玩家可以看见一把重锤从上往下敲击砧上的热坯。每轮四击，末击抬得最高、火星最宽。同台所有制作中的订单共用一把锤，关菜单或下单人离线后仍按订单截止时间计时。移动或转向同一张家具时，锤击跟随新的砧面。附近无人时收起，最后一单完工后停锤。指令开单和历史上未记录工作台的订单照常制作、领取。

锻造成品发放且领取记录保存成功后，工坊关闭。成品从当前领取台的炉口抽出，越过炉唇露出余热轮廓，翻面时响一声清音，随后按原外观停留展示。默认停留约半秒。强化成功时，装备副本仍从晶座前沿旋转升起，在高点停留后收束。原装备留在强化槽，菜单在 30 tick 后重开。

`workstations.yml` 独立配置启用、时长、升空高度和离台距离，支持 `/sf reload`。首次安装随默认配置释放；旧数据目录缺失此文件时明确记录并使用 JAR 内的默认设置，不覆盖或补回非 GUI 文件。已有文件无效则拒绝该次配置应用，保留旧快照。自定义时长不改变强化菜单 30 tick 重开规则。

展示使用非持久化 `ItemDisplay` 副本，保留真实 CE/ItemHub 物品外观，不生成可拾取掉落物。退出、传送、换世界、家具移除、重载及停用清理展示。当前动画支持 Paper/Purpur；Folia 暂不生成展示实体，避免公开调度 API 在停用时无法保证跨区域即时移除，强化与锻造业务仍可用。此限制与主插件的 Folia 业务支持分开。

更新重锤时，同时交付 StarForge JAR 和 `craftengine/星锻工作台` 中的重锤物品入口、几何及五段音效。正常重启加载 JAR，运行 `/ce workflow default` 并让客户端接受新包。

Java 25 + Maven + Paper API 26.2；目标 Paper、Purpur、Folia，不使用 NMS。采用自由强化与特殊规则覆盖：原版剑、斧及具备主手攻击属性的 ItemHub / AsterStats 自定义武器无需登记，普通非武器默认拒绝，不修改服务器的星芽短剑配置。

## 安装

- 交付目录 `dist/` 有 StarForge、带新接口的 ItemHub、带强化贡献桥接的 AsterStats 三个 JAR；停服备份后成套替换，再启动。不要用旧 ItemHub/AsterStats 配新 StarForge。
- `CraftEngine/resources/强化石` 已写入 leaf；工程内 `craftengine/强化石` 是同一资源包副本。重载 CE 并重新生成/下发资源包后，五种材料 ID 为 `starforge:stone`、`starforge:intermediate_stone`、`starforge:advanced_stone`、`starforge:lucky`、`starforge:protect`。内部透明物品 `starforge:gui_blank` 仅用于菜单，不列入材料分类。材料贴图均为透明 32×32 PNG，生图原稿保留于 Codex generated_images。
- Vault 及其经济提供者用于收费，CE 用于默认材料，PAPI 可选。配置成 vanilla 或 binding 来源时材料不需要 CE。缺少经济服务且费用非零时停止强化，不扣材料；缺少 CE 定义时构造/校验材料报错，不扣费。默认属性后端需要本次 AsterStats 桥接；独立使用可把后端改为 vanilla，原版剑斧同样无需逐把配置。
- 全量默认配置在首次安装时释放。启动或 `/sf reload` 会自动补齐缺失的五个 `gui/*.yml`，包括整个 `gui` 目录被删除的情况；已有菜单内容、注释和未知字段不覆盖。非GUI配置缺失或已有配置有误仍明确报错、保留旧配置，不自动覆盖修复。

## 使用

使用 `/sf`（`/starforge`）打开强化，手动把单件装备放入左侧槽；强化石始终留在背包。中央三列分别选择初级、中级、高级石，图标和数字均可左键 +1、右键 −1，Shift 左键 +5、Shift 右键 −5。悬浮可查看目标等级的每颗概率贡献、拥有、选择和实扣。冲 +11 起先从搭配中扣 1 颗高级石，再按菜单材料顺序累计，达到 100% 且满足最低颗数便停扣。确认栏显示最终成功率、费用及保持、降级和裂纹的实际概率。背包有对应卷轴才会消耗并生效，保护符阻止降级和裂纹。

冲 +15 失败可能产生强化裂纹。裂纹装备可以继续穿戴和使用，等级、属性、品质和失败保底随装备保留。把裂纹装备放入强化台，点击右侧铁砧，消耗背包内 2 颗中级强化石即可修复。修复后可继续强化。

强化成功或失败后显示结果 Title，30 tick 后重开原菜单；武器保留在原GUI槽位，材料选择数量和符文开关保留。库存不足时标红并阻止执行，不自动替换材料。真正关闭菜单、退出、重载、停用或另开菜单时取消重开并返还武器；虚拟图标不会发入背包。标题中的 `{weapon}` 动态显示当前武器真实名称，原版保留客户端翻译；取出武器后清除，强化后更新。现有GUI文件不覆盖，新增虚拟材料样式仅在内存继承发行默认；可在 `gui/main.yml` 的 `virtual-materials` 和确认按钮 `selection-lore` 自定义。

强化名称使用自身组件标记，ItemHub保留名称刷新时仍只显示一份强化等级。旧版本产生且有自身快照可识别的重复后缀，会在切换手持、关闭背包或ItemHub刷新时整理；不会按“+数字”文本删改玩家名称，也不扫描离线存档。

普通权限统一 `starforge.use`，默认 true。管理员统一 `starforge.admin`，默认 OP：

- `/sf admin`：管理菜单，发放五种材料、打开强化、绑定主手强化石与重载。
- `/sf give <在线玩家完整名> <stone|intermediate_stone|advanced_stone|lucky|protect> [数量]`：1~2304，默认1，背包溢出在目标脚下掉落，结果通知收件人。
- `/sf bind <材料ID>`：为主手物品写独立材料标记；在外部 `CraftEngine/resources/强化石/starforge-materials.yml` 对应条目使用 `binding: stone` 等来源后生效。不会偷偷修改材料配置。
- `/sf set <等级>`：设置自己主手武器等级，重置该武器保底；记录控制台审计。
- `/sf mark on [标记]`、`/sf mark off`：为单件主手物品添加或移除可强化标记，可选中文标记对应 weapons.yml 的 marker；黑名单始终优先。已有等级保留，不隐式降级或销毁装备。
- `/sf reload`：异步读取全部配置，成功应用后关闭旧菜单、刷新在线装备。结果在控制台；玩家只提示已提交，不把提交当成功。

PAPI：`%starforge_level%`、`%starforge_failures%` 为主手缓存值。上线、切换主副手、背包操作和关闭容器时合并刷新，不在 PAPI 请求线程读取背包。不能识别未持有的离线武器。

## 配置与计算

`config.yml`：上限、保底每次增加的小数概率、幸运加成、公告等级、唯一属性生效路径。
`levels.yml` 按目标等级配置成长、费用、最低用量、单颗概率 `stone-chances`、必扣材料 `required-stone` 和失败后果。单颗概率按 +1～+3、+4～+6、+7～+10、+11～+12、+13～+15 五档递减。降级最多一级并保留 +10。冲 +15 的默认失败结果为 60% 保持、35% 降级、5% 裂纹，三者互斥。旧配置缺少新字段时仅在内存补齐；旧的默认 35% 降级值在 +11～+14 按新分段生效，其他已改数值保留。
`CraftEngine/resources/强化石/starforge-materials.yml`：每种材料只用一个来源 vanilla / craft-engine / binding。vanilla 严格匹配无额外元数据的原版物品，避免误吞其他自定义物品。
`weapons.yml`：schema-version 2，仅定义特殊规则；可为空。每项以 itemhub（中文完整ID）、vanilla（材质名）或 marker（管理员标记）之一匹配。支持 blacklist、max-level（1到全局上限）、growth-multiplier（0到100）、levels（按目标等级部分覆盖费用、用量、成长、降级等既有字段，石头阶梯与裂纹继承 levels.yml）。未覆盖字段使用默认等级规则。多个规则命中时任意黑名单优先，其他规则按 ItemHub ID、marker、材质的优先级选择一项，不叠加倍率。普通非武器不会因增加一个倍率条目就变成武器，需正攻击属性或主动标记。
`gui/main.yml`、`gui/admin.yml` 各自一个菜单，Shape 和按钮文案可配置；`lang/zh_cn.yml` 为反馈与强化附加文案。代码统一递归关闭斜体，动态参数按纯文本插入。

强化额外攻击 =（累计固定成长 + 最新基础攻击 × 累计百分比成长）× 武器成长倍率。例：默认倍率1，+5固定加1、百分比加25%，基础2时总3.5，基础4时总6。基础优先查询 AsterStatsItemApi 的该件物品属性（PDC > ItemHub定义 > Lore，不含强化），其次读取最新 ItemHub 定义及原武器主手 AttributeModifier；原版基础不包含玩家自身1点攻击。旧 base-attack 仅为对应旧登记物品在无真实属性时的最后备用值，绝不作为全体武器默认值。原版后端补加自身修饰器时保留材质默认攻击和攻速。

旧 weapons.yml 的 schema 1 在完整配置校验通过后异步迁移：只修改版本行，先保存 weapons.yml.v1.bak，原子替换并保留全部中文注释、原有未知字段和选择器；重复加载不再迁移。原有 base-attack 条目继续识别其对应旧武器，包括零值，真实属性优先于备用值。迁移失败停止本轮重载并保留旧快照，不默默覆盖文件。调低武器上限只禁止继续强化，不截断已存等级。

默认 `asterstats` 后端只由 AsterStats 在主手装备读取时合并强化贡献，不另写原版攻击修饰器或 AsterStats 覆盖型 PDC。`vanilla` 后端只替换 `starforge:attack` 修饰器，不通过 AsterStats 再叠加。`api` 后端交由属性消费插件计算，安装本次 AsterStats 时它仍会消费该接口；其他消费者不得同时重复消费。API 为字符串 stat ID → 数值映射，当前仅返回 attack，保留 crit_chance/crit_damage 等扩展空间。

## API 与自动更新

通过 Bukkit ServicesManager 获取 `ym.starforge.api.StarForgeApi`：level、failures、contribution(item, latestBase)、refresh(item)、canEnhance(item)、maxLevel(item)、markWeapon(item,enabled)、baseItem(item)。新增方法保留默认实现兼容现有调用方。markWeapon/refresh/baseItem均返回副本，调用方在物品所有权线程读/写背包；contribution只返回额外值。baseItem纯剥除自身精确记录的名称后缀、Lore装饰及攻击修饰器，不调用属性服务，供AsterStats读取真实基础，避免循环调用和叠加。未强化物品的refresh保持原样。

ItemHub 的 ItemHubModifierApi 在基础配置与外部 Lore 之后执行纯副本回调；本次新增独立 ItemHubLoreDecoratorApi（旧追加接口保持兼容），StarForge用它在基础及外部追加行完成后装饰攻击行，因此 Auxo 等插件调用 Lore-only 重建也会保留强化收益。三个插件需使用配套新JAR。ItemHub 每次完整刷新用最新基础名称/Lore重新组合强化后缀，保留强化PDC；原基础属性更新后强化加成同步重算。StarForge只剥除自身精确装饰，保留其他插件后改的名称/Lore、附魔、模型与PDC；ItemHub完整刷新时其配置管理字段仍服从ItemHub定义。

物品显示只在原攻击行末尾追加绿色 `(+加成)`，例如 `攻击力 » +2 (+1.5)`；+0不追加任何强化Lore，没有可识别攻击行时增加简短 `攻击力 +1.5`。不再追加“基础+强化=总值”长公式。匹配表达式在 weapons.yml 的 lore.attack-line-pattern，文案在 lang/zh_cn.yml 的 weapon-attack-suffix / weapon-bonus-line。使用精确标记Component重组，重复刷新不会累加；名称仍保留强化等级。

提供者不能 IO、访问玩家/世界、递归调用 ItemHub 或延迟改写；不同 Folia 玩家可以并发调用。停止/关闭注册句柄自动释放，无静态跨重载插件状态。物品等级保存在 `starforge:level`、失败次数 `starforge:failures`、单件UUID `starforge:weapon_uid`，不要从名字或 Lore 解析。

## 安全与诊断

每玩家互斥＋500ms点击冷却；原主手快照校验；完整准备材料移除与结果副本后才扣费；收费失败不改背包；收费后再次检查插件、玩家、配置与背包；写入异常恢复完整背包并请求退款，失败明确记录 tx/player 上下文。无自动重试扣费。背包或经济插件重入改变物品时保留重入后的背包并退回本次费用，避免覆盖无关物品。

`operations.log` 单线程异步追加 PREPARED / DECLINED / COMMITTED / ROLLED_BACK / REFUND_FAILED 等记录，包含事务ID与玩家UUID；管理设级记录控制台。日志线程停用时关闭并排空已提交工作。Vault 是同步接口，经济提供者必须能在玩家线程安全快速返回，不得在其实现里阻塞远程 IO。

跨 Vault 与 Minecraft 玩家存档没有统一事务：进程崩溃、断电、经济提供者扣费后抛异常、退款服务失败不能保证自动原子恢复，需要根据日志核账。本版本不宣称具备跨系统崩溃一致性或能阻止其他插件恶意复制含相同UUID的物品。

## 构建和验证

Java25 下执行 `mvn -B -ntp package`。本地插件依赖路径在 pom 属性 vault.jar/papi.jar/ce.jar/itemhub.jar，可通过 -D 覆盖；只编译使用，不打包第三方插件或服务器API。源码按 api/config/item/transaction/integration/gui/command/text 分离。GUI/命令不执行磁盘IO。

详细验证见 `artifacts/verification.md` 及本次 `artifacts/free-weapons-verification.md`。探针、测试服和生图原稿不在发行 JAR 内。尚无真实玩家 GUI、Folia 多区域并发、CE资源包构建或客户端显示验收，不把构建通过等同于全部运行环境通过。

Folia 补充：独立 Arbor/Folia26.2 启动成功，ItemHub、AsterStats、StarForge 均加载。服务器目录中的 Vault 1.7.3 未声明 Folia 支持，被服务器拒绝加载；因此没有在 Folia 上执行扣费探针，正式 Folia 需要兼容版 Vault 与线程安全经济提供者。未改写该第三方 JAR 的兼容声明。完整交易探针仅在 Purpur 上执行。

## 像素强化菜单

玩家强化页增加176×110像素背景，通过CE图片 `starforge:forge_menu` 和独立负间距字体定位，不覆盖玩家背包；`background-image` 可删除以去掉背景。标题使用 `starforge:ui` 引用已有 `xerr_font:fusion_pixel_quest_12_sc.ttf`（由现有CE合并Xerr资源包提供），字体不复制或修改。强化石、幸运符和保护符使用CE材料图标，其余按钮由背景像素画与透明物品承接交互。

强化背景采用与工坊一致的深灰锻铁和细铜框，装备/强化标签分别和上方槽位或按钮连成一体；两个符文没有烘焙文字，保留CE图标及悬浮说明。确认点击区由 Shape 中三个 C 格覆盖，武器仍只接收一个 W 槽；输入槽及九格强化石位置保持。

叶服菜单配置与CE新资源已写入；更新JAR暂存 `plugins/update/StarForge-1.0.0.jar`。下次重启载入配套插件后执行 `/ce workflow default` 构建/发送资源包。五张材料图均为32×32；176×110是整个菜单背景，二者用途不同。生成背景已目视检查，14项测试及构建通过，但客户端字体基线、GUI缩放与点击对齐仍待游戏实测。

## 客户端反馈后的菜单与分级石调整

当前玩家界面只显示三个真实材料图标：本次强化石、幸运符、保护符。武器不再用实物按钮展示，主手武器名字在预览悬浮说明中显示；预览符文、宽强化按钮和关闭X均画入背景，用透明CE物品 `starforge:gui_blank` 承接悬浮/点击。材料仍从背包自动扣取，不需要也不允许把武器或材料拖入菜单。

标题使用Xerr已打包的fusion_pixel_quest_12_sc.ttf，size12、oversample1，并动态显示武器名称。左上独立面板实时显示成功率和小进度条，整数刻度向下取整，精确概率保留在悬浮说明中；中央三列为初级、中级、高级石的图标和所选数量。幸运符、保护符的悬浮说明分别显示可用数量、开关状态和本次使用张数，确认按钮同时汇总两者。

强化石分为初级（stone）、中级（intermediate_stone）和高级（advanced_stone），默认均可混搭。目标等级的 stone-chances 优先于材料自身 chance。材料配置enhancement支持min-target/max-target（结果等级）、steps（成功提升数）、chance（覆盖该石的基础概率）；保底和幸运在此基础上计算。金币、数量和失败惩罚来自实际目标等级。预览和扣费共用ForgePlan，规则不适用时不给扣费；等级映射与材料范围不一致在reload直接报错。

`/sf give 玩家 intermediate_stone 16` 发中级石，管理菜单也有对应按钮。中级新增32×32青色独立贴图，初级蓝色和高级紫色保留已有ID。目标上限或steps改变后应同步调整逐级材料映射，不能让最后一级配置成越过上限的石。

当前14项测试与Maven构建通过；独立Purpur真实物品/代理玩家经济20项探针通过。新版像素画和字体尚待再次客户端验收，未宣称已校准实际GUI缩放。leaf配置/资源已备份后更新，JAR在plugins/update，下次重启并执行ce workflow default生效；不要用旧运行JAR直接reload新菜单。

### 武器展示位与槽位校准（2026-11-06）

最新修正恢复第二行第六列的主手武器副本，保留原物品名称、Lore，并在副本追加强化预览；空手时显示武器展示位提示。无需把武器拖进菜单，拖动和取出仍被拦截。四个展示格按箱子18像素网格重排，强化按钮对应第四行第4～6列，关闭对应第五行第5列。复用现有像素素材校准，不重新生图。

本次修正版位于dist/StarForge-1.0.0.jar；配套菜单配置与背景位于dist/menu-alignment/，必须一起安装。未覆盖leaf已有配置、CE素材或插件JAR；安装前备份对应文件，停服更新JAR和gui/main.yml、CE背景后启动，再构建并下发资源包。静态坐标校验和构建不能代替客户端悬浮/点击验收。


## 材料由 CraftEngine 统一维护（当前约定）

StarForge 的数据目录和发行 JAR 不再存 materials.yml，不保存材料 name/lore。config.yml 只用 materials-file 指向同级 CraftEngine/resources/强化石/starforge-materials.yml：此文件仅保存来源ID和强化规则；物品名称、Lore、纹理唯一来源为 CE 的 configuration/items.yml。GUI中的强化石/符文不覆盖CE文案；状态、金币与当前概率显示在预览透明按钮的悬浮说明中。修改CE文案后重载CE即可使用当前定义；修改规则再执行StarForge reload。

CE configuration/categories.yml 新增“强化材料”分类，列出初级/中级/高级强化石、幸运符、保护符，隐藏内部透明模型。该分类沿用服务器“货币”的CE分类语法；不是新增一种StarForge item来源类型。旧的CE分散定义已备份后合并到items.yml，避免重复ID。

五种材料均写明前往铁匠铺，输入 /锻造台 或 /warp dzt，材料留在背包无需手动放入。这里只提供已有传送指令的使用说明，不注册或修改这些指令，也不会让右键材料自动传送。材料规则加载错误包含外部文件路径和字段，失败保留旧快照；不会自动恢复旧材料配置。

## CE 箱子物品工坊

`/sf craft` 打开原生箱子工坊，权限统一为 `starforge.use`。配方列表、材料详情、我的锻造分别由 `gui/crafting.yml`、`gui/crafting-detail.yml`、`gui/crafting-orders.yml` 定义。配方页保持4行×7列共28格，详情页用于扣料扣费前确认。我的锻造使用上下两层、每层三个的像素工位，直接展示真实产物、进度条和0～100%百分比，每秒刷新；完成后点击该物品或进度条直接领取。背包不足保留订单。菜单支持 Esc 关闭，不依赖 Rafter。

配方列表悬浮说明直接逐项列出材料真实名称、所需数量、当前背包数量及不足数量，充足为绿色、不足为红色；每秒随菜单刷新。名称来自 CE/ItemHub 当前物品，原版材料保留客户端翻译；不限制为单组堆叠数量。`gui/crafting.yml` 的 `elements.R.material-enough`、`material-missing` 配置逐项文案，`lore` 中单独一行 `{materials}` 指定清单插入位置；字段缺失时加载错误指出具体路径。

关闭按钮可从 Shape 移除，仍可用 Esc 关闭。详情页的上一页材料、下一页材料也是可选按钮，可保留仅确认与返回的布局；移除材料分页按钮时只展示当前页材料，配方列表的悬浮清单仍列出全部材料。

`recipes.yml` 配置材料、金币、产物和 `duration-seconds`（制作耗时，秒）。来源支持 `vanilla`、`craft-engine`、`itemhub`；原版材料严格匹配无额外元数据物品。同配方可同时提交多单，每次确认一份、单独扣料扣费、独立计时和领取；`cooldown-group` 非空时仍限制该玩家组内一个订单，领取后释放。示例 `iron_sword` 每单消耗8个 CE 初级强化石、2个 CE 中级强化石、100金币，60秒后产出1把原版铁剑。

`recipes.yml` 根节点 `max-active-orders` 为每玩家未领取订单上限，默认6、范围1～54；制作中、完成未领取和处理中都占工位。收费扣料前检查上限，订单单写入器再次原子校验；不同玩家独立。领取或释放未付费预留后才释放位置。降低上限不会删除旧订单，超出6单的旧记录仍可翻页领取；未开放工位显示暗色。每单产物来自冻结物品，CE/ItemHub 的模型、PDC、名称及原有Lore保留，只在展示副本附加进度说明。

新增锻造品类时，在 `recipes.yml` 给配方填写 `category: 'consumables'`，再在 `gui/crafting-types.yml` 的 `shape` 放置入口字符，并给该元素配置 `action: 'category'`、`category: 'consumables'` 和图标。品类 ID 可用小写字母、数字、下划线及连字符。旧的 `action: 'equipment'` 和 `action: 'machines'` 仍可用，已有配方的品类 ID 也可直接写为 `action`。各品类的配方页标题可写在 `gui/crafting.yml` 的 `category-titles` 下，省略则使用默认标题。修改后运行 `/sf reload`，配方与入口一起生效。配置验证失败时保留上一版，已有订单和解锁记录继续使用。

配方 `default-unlocked` 默认 true；false 时在学习前隐藏。管理员执行 `/sf recipe unlock <在线玩家> <配方ID>` 授予，权限为 `starforge.admin`。学习记录在 `recipe-unlocks.yml` 异步原子保存；提交和扣费扣料时重新检查解锁。普通玩家不需要其他功能权限。

测试配方 `star_sword` 需要解锁；材料、费用和时间与上述示例相同，产出 ItemHub `星锻测试:试作长剑`。定义在 `examples/itemhub/锻造测试武器.yml`，安装到 ItemHub 的 `items/`。通过 `integrations.asterstats` 提供主手攻击+12、暴击几率+5%、暴击伤害+50%，移除材质默认攻击加成。Lore负责展示，实际属性由 AsterStats 读取，玩家自身基础属性另计。

订单在 `crafting-orders.yml`，离线和重启期间继续计时。产物使用提交时保存的物品快照；后续改配方、删除配方或收回解锁不影响已有订单领取。配方详情用于提交新单；成功创建后自动进入“我的锻造”查看进度，失败保持确认页。等待期间若已关闭或切换菜单，不强制打开。“我的锻造”返回配方列表，每个工位只领取对应独立订单，不会误领同配方其他订单。旧版单配方订单仍能查看领取，无需清空存档。`prepared`、`claiming` 表示处理中；重启后异常停留需管理员核对背包和经济记录。Vault、玩家存档和订单文件没有跨系统原子事务，崩溃窗口仍需人工核对。

### 资源部署

上述工坊 GUI 文件（含新增类型页）缺失时由启动或 `/sf reload` 自动从发行JAR补齐；已有文件的升级改动仍须显式部署，不覆盖用户配置。此前仅我的锻造使用新背景（两页新像素背景以本节2026-11-09更新为准）： `starforge:craft_orders_six`（176×110）、已有 `starforge:ui` 字体，底栏正中小号返回图标（B=40），不设置关闭按钮，Esc关闭；不显示页码；五行Shape去掉顶部空行，正常六工位的底栏仅显示居中返回；旧超额订单等确有多页时才显示翻页箭头。底板在原生“物品栏”标题上方收边，背包槽从y122开始；真实客户端对齐仍需实测。CE底栏资源独立放在 `configuration/crafting-orders-navigation.yml`，保留旧自定义菜单的页码资源兼容；默认订单菜单已移除页码，不限制旧订单领取。配方列表和确认页已于2026-11-09接入独立像素背景，原生箱子交互不变。CE资源 `configuration/crafting-orders-icons.yml` 定义0～100%的独立进度纹理、空闲/未开放/处理中图标，纹理64×32、实际显示48×16，模型为普通平面模型，不添加着色器。物品按钮与下方进度的位置由Shape成对读取，改Shape需同步背景。强化页的 `starforge:forge_menu`、字体和材料贴图保持不变，不恢复旧三张锻造背景。

所有工坊功能按钮支持 `craft-engine: 'namespace:item_id'`，直接填写真实CE物品ID；使用后无需 `material`，保留CE名称/Lore/模型，GUI只追加Lore。原版按钮继续使用 `material` 与 `name`。订单产物图标自动使用提交时的实际物品，不需要给每个配方额外配置图标；产出来源使用 `craft-engine` 即可显示CE物品。找不到的CE定义在游戏菜单显示不可用并记录诊断，不退回错误的原版图标。

新JAR暂存 `plugins/update/StarForge-1.0.0.jar` 后由管理员正常重启加载，不热替换已加载的根目录JAR。随后执行 `/ce reload`，等待完成后执行 `/ce workflow default`，让客户端接受新资源包。再次修改现有菜单可执行 `/sf reload`。StarForge不再需要执行Rafter命令或注册提供器；其他Rafter菜单源和插件本体保留。

退役工坊时还需清除旧资源包链：从 CE `resource-pack.merge-external-folders` 移除 `Rafter/generate/resource-pack`；备份归档 CE `resources/rafter-xerr-compat`，并移除 `duplicated-files-handler` 中只针对该目录的四文件 `retain_matching` 规则。保留 MobBars、Xerr、ModelEngine 等其他来源及其他冲突规则。26.2/26.3 text shader 应由 Xerr 原生包提供；旧组合 shader 在客户端出现过 `rafterFrame` 未初始化警告与三角拉伸，不能因还有其他示例菜单就继续保留该全局覆盖。此项只涉及资源包，无须重新编译 StarForge JAR。源配置更改后须重新生成并下发，核对 `cache/hosted/default/resource_pack.zip` 的生效 overlay 及 shader 来源；客户端画面仍需验收。

旧工坊Rafter源码、专属工具与预览已归档至本地 ignored 的 `artifacts/retired-rafter/`，不会打入发布JAR。单测和构建验证配置结构、订单与解锁规则；真实客户端资源包、点击/悬浮对齐、Paper/Folia调度和真实经济交易仍需实机验收。

## 像素工坊入口（2026-11-09）

2026-11-09 像素工坊入口更新：`/sf craft` 先打开 `gui/crafting-types.yml`。六个类型卡位当前只开放“装备”，其余五位没有图标、文字和动作；点击装备进入六卡配方列表，再进入制作确认。只显示真实已解锁配方，不增加示例武器。类型与配方底栏现为居中灰框返回（39～41）、右下铜框我的锻造（42～44）；类型根页返回游戏，配方页返回类型。卡片名称带铜色边框，只对实际显示的装备生效，空卡不显示名称框。

类型、配方列表与确认背景分别是 `craft_types_six`（176×110）、`craft_gallery_six`（176×110）和 `craft_detail_six`（176×128）。`gui/crafting.yml` 的 `elements.S.captions` 按配方 ID 引用 CE 铭牌。未配置铭牌时，玩家仍可悬浮查看真实产物名称。

新增或改名时，在 `tools/crafting_captions.py` 维护配方 ID 和铭牌文字，运行 `python tools/crafting_captions.py <配方ID>` 导出对应贴图和模型，然后更新 `elements.S.captions`。装备、机器及其他品类使用同一种米色像素字与铜框。命令会检查字宽，排不下时应先确定简短的卡片显示名。导出后对照星芽装备卡，以相同缩放查看字高、居中和留边，再随 CE 资源包发布。

图鉴显示每位玩家的「已解锁数 / 可见配方数」。品类零解锁时，入口呈灰暗样式并显示「未解锁」，仍可打开查看。品类已有任意配方解锁时，入口显示正常样式，未解锁的可见配方独立显示灰暗卡片。收获条件类配方提前可见，未授予的隐藏配方不计入分母。锁定卡可查看详情，制作时仍检查解锁资格。

现有六格像素背景与配套布局直接启用水印及计数。同背景下，自定义品类可在 `gui/crafting-types.yml` 的品类元素填 `locked-card-model`，配方可在 `gui/crafting.yml` 的配方元素用 `locked-models` 按配方 ID 指定锁定 ItemModel。每个模型覆盖 52×36 GUI 像素，以图标格为中心锚点。配方模型预留下一行的铭牌区域。使用现有生成器导出铭牌会同时生成彩色和灰色版本。其他背景或布局保留悬浮解锁状态与原点击布局。

配方卡片的2行×3列六格共用同一配方名称、完整动态材料需求Lore与点击动作；背景、装备图标和名称框悬浮内容一致，图标和名称框模型保持。空卡不可点击，自定义Shape中的其他动作不被覆盖；类型页六格使用同一类型按钮定义。

确认页提供六个材料位，材料下方用原生 item model 数字显示拥有/需要，绿色充足、红色不足，每秒随背包刷新；费用与耗时在成品和制作按钮悬浮说明中显示完整值。材料超过六种时可条件翻页，数字不依赖着色器或重开窗口。`starforge:gui_blank` 作为内部透明按钮时允许使用该GUI配置的名称，其他CE按钮继续保留原始名称。

该变更已构建并暂存，尚未完成真实客户端资源包、数字模型与点击验收；正常重启加载更新JAR后执行 `/ce reload`、`/ce workflow default` 并接受资源包。强化与我的锻造资源保持。

## ItemHub 随机品质成品（2026-11-16）

品质与成色属于AsterStats；锻造通过ItemHub通用 `ItemHubGenerationApi` 的 `RANDOM` 模式，在提交准备阶段调用AsterStats提供者抽取一次，再将完整物品字节保存到订单。领取读取订单快照，重启、领取或强化不重抽；预览使用 `PREVIEW`。旧 `itemhub:quality_roll` 与新 `asterstats:quality_roll` 订单均隐藏未领取结果。材料创建和匹配路径保持原有API，铁片/铁环不加随机品质。随机品质成品每订单必须为1件，超出会在扣材料/扣费前拒绝。

未提交订单的配方列表和详情使用不抽取的品质预览，显示属性范围及概率。订单创建后，制作中、已完成待领取和领取处理中的菜单只显示基础装备名称、进度及领取说明，隐藏已保存的品质、成色和实际属性；只有领取到背包后才揭示品质。抽取仍在准备时执行一次，领取直接读取原成品字节，不因预览、重启或领取重抽。须与配套ItemHub和AsterStats更新，避免旧API缺失。已有订单保留原成品快照，不追补随机品质。
