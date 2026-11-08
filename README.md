# StarForge 第一版

Java 25 + Maven + Paper API 26.2；目标 Paper、Purpur、Folia，不使用 NMS。采用自由强化与特殊规则覆盖：原版剑、斧及具备主手攻击属性的 ItemHub / AsterStats 自定义武器无需登记，普通非武器默认拒绝，不修改服务器的星芽短剑配置。

## 安装

- 交付目录 `dist/` 有 StarForge、带新接口的 ItemHub、带强化贡献桥接的 AsterStats 三个 JAR；停服备份后成套替换，再启动。不要用旧 ItemHub/AsterStats 配新 StarForge。
- `CraftEngine/resources/强化石` 已写入 leaf；工程内 `craftengine/强化石` 是同一资源包副本。重载 CE 并重新生成/下发资源包后，五种材料 ID 为 `starforge:stone`、`starforge:intermediate_stone`、`starforge:advanced_stone`、`starforge:lucky`、`starforge:protect`。内部透明物品 `starforge:gui_blank` 仅用于菜单，不列入材料分类。材料贴图均为透明 32×32 PNG，生图原稿保留于 Codex generated_images。
- Vault 及其经济提供者用于收费，CE 用于默认材料，PAPI 可选。配置成 vanilla 或 binding 来源时材料不需要 CE。缺少经济服务且费用非零时停止强化，不扣材料；缺少 CE 定义时构造/校验材料报错，不扣费。默认属性后端需要本次 AsterStats 桥接；独立使用可把后端改为 vanilla，原版剑斧同样无需逐把配置。
- 默认文件仅在整个 StarForge 数据目录不存在时释放。已有目录不补回被删文件；缺文件/配置有误时明确报错，重载保留旧配置，不覆盖中文注释。

## 使用

玩家主手持有单件符合条件的武器后 `/sf`（`/starforge`）。GUI 不托管装备，只显示主手副本；点击确认时重新核对原物品。幸运符与保护符默认不启用，点击各自按钮切换，启用则额外消耗一张；保护符阻止降级，不保证成功。

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
`levels.yml`：每个目标等级分别配置成功率、money、material、amount、attack-flat、attack-percent、downgrade-chance。0.05 表示 5%；成长为逐级增量，+5 会累加 1~5 的成长。+1~+10 不降级；+11 起失败按目标等级的降级概率降1级。任何失败都不销毁武器，成功清零连续失败。
`CraftEngine/resources/强化石/starforge-materials.yml`：每种材料只用一个来源 vanilla / craft-engine / binding。vanilla 严格匹配无额外元数据的原版物品，避免误吞其他自定义物品。
`weapons.yml`：schema-version 2，仅定义特殊规则；可为空。每项以 itemhub（中文完整ID）、vanilla（材质名）或 marker（管理员标记）之一匹配。支持 blacklist、max-level（1到全局上限）、growth-multiplier（0到100）、levels（按目标等级部分覆盖，与 levels.yml 同字段）。未覆盖字段使用默认等级规则。多个规则命中时任意黑名单优先，其他规则按 ItemHub ID、marker、材质的优先级选择一项，不叠加倍率。普通非武器不会因增加一个倍率条目就变成武器，需正攻击属性或主动标记。
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

叶服菜单配置与CE新资源已写入；更新JAR暂存 `plugins/update/StarForge-1.0.0.jar`。下次重启载入配套插件后执行 `/ce workflow default` 构建/发送资源包。五张材料图均为32×32；176×110是整个菜单背景，二者用途不同。生成背景已目视检查，14项测试及构建通过，但客户端字体基线、GUI缩放与点击对齐仍待游戏实测。

## 客户端反馈后的菜单与分级石调整

当前玩家界面只显示三个真实材料图标：本次强化石、幸运符、保护符。武器不再用实物按钮展示，主手武器名字在预览悬浮说明中显示；预览符文、宽强化按钮和关闭X均画入背景，用透明CE物品 `starforge:gui_blank` 承接悬浮/点击。材料仍从背包自动扣取，不需要也不允许把武器或材料拖入菜单。

标题改用Xerr已打包的fusion_pixel_quest_12_sc.ttf，size12、oversample1，文字缩短为“星锻强化 +等级”。不再把100%等长数字挤在标题；成功率仍在预览/确认按钮悬浮说明中。

强化石现为三级：初级stone只用于0→1，steps1、chance1.0；中级intermediate_stone用于目标2~10；高级advanced_stone用于目标11~15，中高级省略材料chance时使用levels.yml概率。材料配置enhancement支持min-target/max-target（结果等级）、steps（成功提升数）、chance（覆盖该石的基础概率）；保底和幸运在此基础上计算。金币、数量和失败惩罚来自实际目标等级。预览和扣费共用ForgePlan，规则不适用时不给扣费；等级映射与材料范围不一致在reload直接报错。

`/sf give 玩家 intermediate_stone 16` 发中级石，管理菜单也有对应按钮。中级新增32×32青色独立贴图，初级蓝色和高级紫色保留已有ID。目标上限或steps改变后应同步调整逐级材料映射，不能让最后一级配置成越过上限的石。

当前14项测试与Maven构建通过；独立Purpur真实物品/代理玩家经济20项探针通过。新版像素画和字体尚待再次客户端验收，未宣称已校准实际GUI缩放。leaf配置/资源已备份后更新，JAR在plugins/update，下次重启并执行ce workflow default生效；不要用旧运行JAR直接reload新菜单。

### 武器展示位与槽位校准（2026-11-06）

最新修正恢复第二行第六列的主手武器副本，保留原物品名称、Lore，并在副本追加强化预览；空手时显示武器展示位提示。无需把武器拖进菜单，拖动和取出仍被拦截。四个展示格按箱子18像素网格重排，强化按钮对应第四行第4～6列，关闭对应第五行第5列。复用现有像素素材校准，不重新生图。

本次修正版位于dist/StarForge-1.0.0.jar；配套菜单配置与背景位于dist/menu-alignment/，必须一起安装。未覆盖leaf已有配置、CE素材或插件JAR；安装前备份对应文件，停服更新JAR和gui/main.yml、CE背景后启动，再构建并下发资源包。静态坐标校验和构建不能代替客户端悬浮/点击验收。


## 材料由 CraftEngine 统一维护（当前约定）

StarForge 的数据目录和发行 JAR 不再存 materials.yml，不保存材料 name/lore。config.yml 只用 materials-file 指向同级 CraftEngine/resources/强化石/starforge-materials.yml：此文件仅保存来源ID和强化规则；物品名称、Lore、纹理唯一来源为 CE 的 configuration/items.yml。GUI中的强化石/符文不覆盖CE文案；状态、金币与当前概率显示在预览透明按钮的悬浮说明中。修改CE文案后重载CE即可使用当前定义；修改规则再执行StarForge reload。

CE configuration/categories.yml 新增“强化材料”分类，列出初级/中级/高级强化石、幸运符、保护符，隐藏内部透明模型。该分类沿用服务器“货币”的CE分类语法；不是新增一种StarForge item来源类型。旧的CE分散定义已备份后合并到items.yml，避免重复ID。

五种材料均写明前往铁匠铺，输入 /锻造台 或 /warp dzt，材料留在背包无需手动放入。这里只提供已有传送指令的使用说明，不注册或修改这些指令，也不会让右键材料自动传送。材料规则加载错误包含外部文件路径和字段，失败保留旧快照；不会自动恢复旧材料配置。

## 耗时锻造

`/sf craft` 打开像素锻造菜单，权限为 `starforge.use`。选择配方并确认后从背包扣除材料及金币，开始计时。点击「配方 / 我的锻造」查看每件订单的百分比、剩余秒数与领取状态。完成后点击订单领取，背包不足时保留订单。

`recipes.yml` 配置材料、金币、产物和 `duration-seconds`（制作耗时，秒）。`source` 支持 `vanilla`、`craft-engine`、`itemhub`，使用对应物品 ID。原版材料严格匹配无额外元数据物品。示例铁剑配方为 8 个 CE 初级强化石（starforge:stone）、2 个 CE 中级强化石（starforge:intermediate_stone）、100 金币、60 秒，产出 1 把原版铁剑，正式经济数值请按服务器调整。同配方每玩家一单，`cooldown-group` 非空时组内共用制作位置，领取后可再次提交。不同配方默认可同时制作。

订单保存在 `crafting-orders.yml`，离线和重启期间继续计时，产物使用提交时快照，已删除配方的订单仍可从我的锻造领取。文件读写异步执行。`prepared`、`claiming` 表示处理中，重启后遇到这两种状态需管理员核对玩家背包和经济记录，避免自动重试造成重复发放。Vault、玩家存档和订单文件无跨系统原子事务，崩溃窗口仍需人工核对。

已有安装需显式添加 `recipes.yml` 与 `gui/crafting.yml`，不会自动补回缺失资源。新背景为 CE `starforge:crafting_menu`，来源 `craftengine/强化石/configuration/gui-images.yml` 与 `resourcepack/assets/starforge/textures/font/gui/crafting_menu.png`。部署后需要执行 CE workflow 并让客户端接受资源包，才能验收实际对齐。
