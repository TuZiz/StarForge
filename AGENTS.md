# StarForge 项目协作规则

## 2026-11-25 图鉴解锁水印

- 图鉴与配方列表从同一玩家解锁规则生成可见条目视图。品类已解锁数为 0 时入口灰暗并显示「未解锁」，仍可进入。任意配方解锁后入口恢复正常。只有仍锁定的可见配方带水印。未授予且无提前可见条件的隐藏配方不列出、不计数。保留锁定详情查看和制作前的实际解锁检查。
- `tools/build-crafting-unlock-assets.py` 导出锁定卡和计数。两行三列常规卡共用 52×36 GUI 尺寸和 2 texel/GUI 像素，计数在图鉴卡上行右格，52×36 模型坐标通过 GUI 缩放落在客户端合法范围。配方遮罩保留铭牌区域，灰色铭牌由共用导出器的 CMD flag 控制，卡片六格沿用同名同 Lore 同点击。只在既有六格像素背景及对应 Shape 占位上放跨槽装饰；其余布局使用文字状态。

## 2026-11-25 锻造配方铭牌统一

- 装备、机器和新增品类的配方卡统一走 `tools/crafting_captions.py`。用 Xerr 的 `fusion_pixel.ttf` 在原生 10 px 栅格化，米色字居中，与「星芽护额」并排比对。48×16 GUI 铜框、128×64 RGBA 图集、96×32 内容范围和 2 texel/GUI 像素采样保持一致，入口保留 `oversized_in_gui: true`。
- 以字形实测宽度决定是否收窄横向排版，保持原字号与笔画高度。名称超过内框 44 px 时允许按最近邻收窄，保留原宽至少 70%。超限由生成器报错，选择经确认的简短显示名。新增或改名时，用同倍率并排核对四字、五字和六字铭牌，检查完整笔画、居中、留边、UV 与 `elements.S.captions` 引用，图标预览复用实际物品资产。只有换皮的发布走 CE 资源同步与合包。

## 2026-11-25 锻造品类热重载

- `recipes.yml` 的 category 与 `gui/crafting-types.yml` 的 `action: 'category'`、category 入口共用品类 ID。保留旧 equipment→weapons、machines 和引用现有配方品类的同名 action；新空品类用显式 category。品类标题由 `gui/crafting.yml` 的 category-titles 配置，旧 machines-title 仍作回退。
- 配方、入口和标题一次解析校验并发布同一锻造快照。重载成功只淘汰旧快照的菜单，由各玩家调度器关闭；失败保留旧快照、已打开菜单和订单/解锁数据。新增配方和品类在新版 JAR 加载后通过 `/sf reload` 生效。

## 2026-11-20 强化石阶梯与裂纹

- levels.yml 的 stone-chances 按真实目标等级覆盖 CE 石头默认概率，GUI、虚拟扣料和旧实物槽共用 ForgePlan。+11 起预扣一颗高级石并计入最低颗数，达到 100% 后仍要满足这条门槛。旧文件按发行默认内存补新字段，旧默认 0.35 降级按新分段替换，文件不重写。
- 降级只在失败后判定，单次降一级，最低 +10；+14 冲 +15 失败时 35% 降级、5% 裂纹、60% 保持，概率互斥。保护符在背包可用且开启时抵消两种后果。预览显示最终结果概率。
- 裂纹由物品 PDC starforge:cracked 持有，专属 Lore 标记幂等刷新，装备可继续穿戴且保留属性与品质。原强化槽右侧修复消耗两颗中级石，复用同一互斥事务和回滚路径；不扣金币或卷轴，保留等级与保底。修复成功后即时重绘原菜单。

## 2026-11-20 强化名称字体边界

- 用户要求强化等级前移除金色星号，默认后缀仅保留空格和 +等级。旧发行 level-suffix 精确匹配后在内存升级，旧装备仍由正常刷新重建后缀，保留品质框、原名及服主自定义格式。
- ItemHub品质名根组件可能带itemhub:quality字体，强化名称后缀的starforge.name.level保留组件须显式font=minecraft:default，防止星号/加号/数字继承品质字形显示方框。保留名称原品质字体及后缀配置显式指定的字体，不用全名扁平化或全局字体替换。
- 已强化装备通过原EquipmentSync/ItemHub刷新惰性重建，保留等级、品质与精确后缀清理；本次只需更新StarForge JAR，正常重启，无需CE合包。

## 2026-11-20 星轨强化台与炉火锻造台

- 实体台新建的订单在 schema 1 的订单记录中可选保存家具 UUID、世界、坐标和朝向。旧订单和指令订单没有台锚点。只从已提交的 running 订单计算每台最后完工时间。制作中每台最多一把重锤，四击循环 61 tick，逐次使用对应的声音，末击最重。已完工待领不继续敲击。
- 重锤原稿在 `artifacts/workstations-design-preview/models/forge_hammer.bbmodel`，显示入口为 `starforge:workstations/forge_hammer`，音效命名空间为 `starforge_workstations`。领取成功才从当前领取台炉口抽出展示副本，越过炉唇上色，翻面响一次，默认保持原貌 10 tick。发布需同时交付 JAR 和 CE 包中的重锤入口、几何、声音定义及五个 OGG。

- 当前可编辑台体与视口预览在 `artifacts/workstations-design-preview`。运行模型使用原家具 ID、约 3×2 格和原手持变换。强化台的八面青晶引用 `starforge:block/workstations/orrery_crystal_facets`，发布时连同两份几何同步并重建 CE 包。
- 强化展示在晶座前沿升起旋转并停顿。领取成功的锻造成品从炉口抽出，越过炉唇后出现余热轮廓，翻面后恢复原貌。轨迹和粒子都随 CE 实际摆放角度旋转，展示副本保留物品外观及数据。调整炉膛或晶座位置时，核对 `StationMotion` 模型点及四向测试。

## 2026-11-17 首装资源完整性

- InitialResourceInstaller在异步配置阶段识别不存在或仅含空目录的首次安装，安装六份非GUI配置；GuiResourceInstaller继续负责六菜单缺失补齐。已有非空数据/配置目录不能被视为全新安装，保留原非GUI配置不自动补回约定。
- 发行资源在写入前整批读取校验存在，临时文件完成后不覆盖移动；失败只回滚本次仍未被修改的文件，诊断路径与原因。不得改回单纯root.exists判断，也不得打包订单/解锁/玩家数据或恢复已退役materials.yml。
- 114测试/package与Purpur26.3/Java25空目录实际启动12份资源、再次启动自定义字节保留通过；CE材料仍由其资源目录维护。发布见artifacts/default-resources-delivery.json，未重启不等于已加载。

## 2026-11-16 防具强化

- 星芽四件及原版头/胸/腿/脚防具可使用原强化槽；每件独立等级与保底。防具主属性为defense，武器仍为attack，不增加生命/法防/韧性，不重抽ItemHub品质。
- WeaponResolver按材质实际穿戴槽查询AsterStats readBase；ItemHub备用路径同样读取品质快照与slot限制。NativeArmorAttributes隔离原生ARMOR与starforge:defense修饰器，vanilla后端保留原生护甲/韧性并只在对应槽生效。
- levels.yml增加可选defense-flat/defense-percent，默认每级0.20/0.05，和attack成长独立；旧schema1缺字段只在内存默认，不重写文件。weapons.yml现有选择器/黑名单/上限/倍率同样适用防具，等级覆盖支持防御字段。
- StarForgeApi新增slot-aware contribution重载，AsterStats配套桥接传递真实slot；旧接口保留。防具在主手/副手/其他防具槽无强化贡献，旧StarForge提供者仍仅主手兼容。必须配套交付StarForge与AsterStats，不把仅Lore显示当作属性接入。
- 防御行以starforge.lore.defense_bonus精确标记幂等装饰；baseItem只清理自身名字/Lore/修饰器，不回调其他服务。法术/魔法防御不匹配物理防御行。
- GUI确认使用stat-name/current/next-value动态显示攻击或防御；next-attack旧占位符继续兼容。旧发行攻击预览及武器提示仅精确匹配后内存升级，自定义文案/Shape/文件不覆盖。无需修改CE贴图或重新合包。

## 2026-11-16 制作成功后进入我的锻造

- 确认制作新单在reserve/扣料扣费/activate持久化成功后进入ORDERS，返回目标RECIPES；不再仅重绘确认页。事务分离成功导航和失败重绘回调，activate失败不导航，材料/金币/容量/解锁预检失败保留确认页。
- 完成回调在玩家scheduler且原会话valid时才导航：插件有效、在线、权限、配置快照、会话身份和实际打开的Inventory一致。异步等待中关闭/换页不强制重开，沿用既有站点锚点。存档、进度、并行同配方与领取规则不变；GUI只定向替换旧默认一句操作说明，不覆盖用户布局。

## 2026-11-16 强化名称后缀幂等

- ItemHub刷新可能保留已经强化的名字，不能把Modifier输入一律当成新裸名称。HubBridge与装备同步统一调用StarForgeApi.refresh路径，不再使用freshName跳过自身后缀清理。
- 强化后缀用空fallback的保留组件键starforge.name.level标记；递归只剥离自身标记。旧物品依据last_name/base_name/name_suffix快照及精确尾部组件清理重复同一后缀，包括已污染base_name，不按正则删除任意“+数字”。原版未命名物品还原customName=null，保留外部改名、耐久和其他PDC。旧重复在正常EquipmentSync事件或ItemHub刷新时惰性修复，不扫描离线存档。

## 2026-11-16 强化页信息与像素重排 v2

- 最新实机修正覆盖旧聊天选量规则：数字与强化石图标共享左右±1、Shift左右±5，移除数字左键/中键聊天输入、监听器和超时状态；中键忽略。两种按钮名称和完整Lore一致，使用真实CE名称并在最终customName/Lore递归italic=false，不能把无样式的CE itemName直接复制成默认斜体customName。真实武器槽中心(34,62)，原图标分区53～74导致纵向偏上；分隔线改71、装备/强化标签上移1px，三图标分区与槽位中心一致，槽位和真实武器不动。
- 中央材料区最新v4覆盖初/中/高竖卡：用户再次否定三根竖框，改为单块横向铜框（60,53～115,88），三真实CE石头与左右装备/强化图标同行，数量在下层，标题“强化石”位于上方。移除等级单字、顶部空栏和重复格线；保留九S动作映射及选量/悬浮行为，Shape和JAR不变。只改forge_menu.png，成功率v3、符文、关闭不改；旧竖卡不恢复。
- 用户随后条件认可v4：实际居中就保留此版。中心轴88，武器34/强化142对称，三石70/88/106；不得再重设计。数量奇数位使用crafting/count，偶数位使用enhancement/count_even向右2px，修正共享九字符位置向左偏移；0～2304全量坐标及数字一致性校验。even仅2/4位，使用原3×5图集、两色和pos2～5共80geometry，不改工坊共享数字模型；新增入口oversized_in_gui=true。工程预览必须读取真实3×5数字geometry/atlas，以2倍画布精确表达半像素，不能用10px字体模拟。此修正需新JAR和CE新模型同时加载，不能沿用v4纯贴图无需JAR的说明。
- 最新v3覆盖成功率面板：用户反馈“大且丑”，移除大框，改为原生10px单行“成功率 百分比”及2px细条；图集512×256、单元64×16，geometry GUI translation=6,10,0，Q仍10，其他位置不变。tools/build-enhancement-assets.py保留整数UV、不缩字。v2大框不再恢复。
- 左上Q=10展示随配料/实际幸运符/保底变化的成功率，原生item_model enhancement/chance选取0～100整数刻度（向下取整，真实100%才显示100），悬浮显示精确成功率与可用状态。不通过重开菜单刷新概率，不用NMS或shader。
- 五行Shape第二行为FQFSSSFFF；确认按钮仅25/34两格。S仍为三列九格动作区，上行改初/中/高标签，中行真实CE石头，下行数量；贴图为连续竖卡，不再画顶部闲置格。W19、L6、P8、X40位置不改。武器/确认标签保持一体框。
- 幸运/保护各自availability-lore显示真实可用张数与本次实用0/1张，保留CE名称/Lore及用户toggle-lore。两种计数独立；背包没有只显示等待材料，不虚加概率。新字段按实际action在内存继承默认，已有文件不自动覆盖。
- chance使用gui_blank+ItemMeta.itemModel，101个单面geometry与一张原生图集（v3尺寸见上，1texel/GUI单位），不注册101个CE物品。字体引用现有Xerr10px，不复制字体原件。资源与对应Shape部署先备份，仅修改已确认旧布局；自定义其他Shape不得整页替换。

## 2026-11-16 虚拟强化石搭配（覆盖旧九格实物输入约定）

- 仅W托管单件武器；原九个S格按Shape顺序分成三列虚拟材料卡，真实石头始终留在背包。图标左/右±1、Shift左/右±5；下方数字左键或任意卡格中键输入0～背包拥有量（上限2304）。默认选量0，三种可混搭；不自动抽取或替换材料。非普通批量背包操作仍拦截。
- 中键/数字输入临时关闭GUI，装备继续托管，聊天输入或取消/300tick超时返回同一Holder；打开其他菜单、退出、重载取消输入并返还武器，迟到回调不能重复返还。输入恢复与结果恢复共享安全路径。30tick结果重开保留武器、数量与卷轴开关，真正关闭只返还武器，绝不能返还S的虚拟图标。
- ForgeStoneSelection与ForgePlan供预览和事务共用，按virtual-materials.materials顺序累计，100%且满足最低用量停扣。所选必须全部在背包拥有，即便未实扣部分；强化后库存不足保留原选择并标红禁用。确认前核对最后一次预览的武器、选量和背包快照，变化先刷新再要求确认。
- 幸运/保护仍独立虚拟开关，背包有才扣一张/生效，缺失显示等待材料且不影响强化。GUI材料保留CE名字/Lore，追加选择信息；数字复用现有crafting/count模型，不新增CE注册或位图。
- 已有gui/main.yml原样保留，仅在内存补齐新增virtual-materials及实际forge动作对应selection-lore默认字段，不补旧Shape/材质等无效字段。新键语言同样继承发行默认。用户新配置优先，修改生效需正常重启新JAR。

- 2026-11-16三处GUI显示修复：26.x客户端ClientItem顶层oversized_in_gui默认false，所有跨槽名称框、进度和材料数量composite入口必须显式true，否则只显示中间16px而丢边框/笔画；当前111入口，三个导出脚本须保留。不重画正确的PNG。MenuArt.compose为无显式字体的标题正文建立minecraft:default边界，防止动态武器名继承starforge:offsets；保留子组件明确字体、客户端翻译和统一非斜体。资源修复接受新包生效，Java字体修复须正常重启新JAR，不热加载。

- 2026-11-16手持尺寸修正：两工作台geometry的display显式设置左右手firstperson等比0.16、thirdperson等比0.18；放置家具仍scale1，elements/纹理/GUI/ground/fixed不改。后续从Blockbench原件重新导出时必须保留运行模型的四个手持变换，不能因多格模型未定义手持display而恢复巨大尺寸。

## 2026-11-16 工作台结果动画

- 两台沿用真实 Blockbench MCP v4 模型（85/73 Cube），原生约3×2格、CE scale1不拉伸；本轮不重新制作位图或重复合包。
- `CeStationOrigins` 只观察未取消主手家具事件，原CE保护条件与非OP命令继续执行；一次性同tick位置绑定到菜单会话。手输指令、过期位置和无关家具不借用上次工作台。
- 强化仅COMMITTED成功后播放本次结果副本升空旋转；保留30tick重开、武器/余石原槽托管。锻造仅成功发放且持久化领取完成后关闭工坊展示，不对离线计时完成生成实体。
- `workstation` 包独立负责位置、动画配置和展示生命周期；`workstations.yml`新安装释放，旧目录缺失则记录使用内置默认且不自动补回。无效配置拒绝应用。
- 动画是非持久ItemDisplay，无真实掉落物或背包扣发。每台最多一份视觉，同台其他交易照常。Paper/Purpur播放；Folia暂不创建展示实体，避免停用跨区移除无法保证，不宣称Folia动画已支持。业务兼容声明不变。

## 2026-11-15 独立 CE 工作台

- 2026-11-16用户明确要求使用Blockbench MCP制作：已实际通过MCP create_project和原生Cube/Group/Texture+Undo建立两项目、修复纹理绑定、添加铜槽/炉侧束带，再由原生codec导出。最新强化台85块、锻造台73块，目录artifacts/workstations-mcp-v4；真实正反面视口已目视检查，不能再用v3工程渲染冒充Blockbench。两个项目留在Blockbench标签页。CE运行geometry来自原生导出，补minecraft纹理命名空间和既有display变换；台体仍scale1,1,1，保留现有像素称号及动作。九文件备份同步并回读哈希一致，云端已运行、CE重载完成并提交default工作流；最终包检查和客户端验证分别记录。
- 最新v3覆盖v2缩放：用户实机反馈“只是拉长了”，要求重做模型。两台直接重建原生约3×2格几何，CE台体scale必须1,1,1，position=0,0.5,0，不用不等比缩放制造多格模型；晶石、铁砧和工具保持自然比例。强化台83块含中央祭坛/四柱/后门架/副台，锻造台71块含独立左炉/中砧/右桌/工具架；面UV保持1像素/模型单位，长部件分段。源文件与工程预览artifacts/workstations-v3，构建工具tools/build-workstation-v3.py；称号锚点改2.6格，保留字体与框。九运行文件已备份同步云端并回读哈希一致，云端停止未合包。用户打开Blockbench后MCP已连通，但本轮导入停在Blockbench原生插件文件访问授权，需用户处理，不自动代点权限。
- 用户随后反馈太小且需要上方像素称号：v2在CE中将台体放大为2.5×1.4×2，约2.5×2格占地、建议预留3×2格，落地position=0,0.7,0。两排三列交互区覆盖台体；名字与立体阶梯像素框共用billboard center、position=0,2.9,0。称号框为内部enhancement_nameplate/crafting_nameplate物品，不列入分类，名字在furniture.yml的text_display.text修改；字体starforge:workstations/title复用Xerr现有12px TTF，不复制字体原件。独立包共九个运行文件已备份同步本地和云端、回读哈希通过；云端仍关服，未合包/游戏验收。当前Blockbench MCP连接拒绝，v2预览为简化材质工程渲染，不是Blockbench或游戏截图。
- 强化台、锻造台使用独立 `craftengine/星锻工作台` 包，物品、家具、分类分别配置；分类 `starforge:workstations`，两个物品/家具 ID 为 `starforge:enhancement_station`、`starforge:crafting_station`。
- CE 原生 furniture_item 地面放置、四向居中、三次命中回收；右键主手且通过 interact 保护检查后，以玩家身份非 OP 执行 `starforge:starforge` 或 `starforge:starforge craft`。不依赖 BetterModel，不增加 Java 点击桥接。
- 模型由 artifacts/workstations-v1 导出，运行包只存两份 geometry，使用原版纹理；CE model 字段生成客户端入口，不把直接 item_model 入口当 geometry。
- 六个运行文件已同步本地 leaf 与云端；本轮云端实例停止，CE reload 提交失败，不能声称资源已加载或托管 ZIP 已更新。待正常启动后执行 CE workflow default，并验收摆放、回收与菜单交互；本轮没有启动服务器。

## 2026-11-13 工坊装饰模型

- 进度、空闲/锁定状态、翻页页码及装备名称框不再逐项注册 CE 物品；保留原图片与 geometry，通过共用 `starforge:gui_blank` 的 `ItemMeta.setItemModel` 显示。
- `CraftingDecorationModels` 仅识别保留的内部旧 ID；既有 GUI 的 craft-engine 字段仍可引用它们。真实材料和装备继续走 CE/ItemHub 原接口。模型入口在 `assets/starforge/items/crafting/*.json`，不能直接把 geometry 路径当 item_model。
- 三个素材导出脚本只生成客户端模型定义，不恢复 crafting-orders-icons.yml、crafting-orders-navigation.yml、crafting-pages-icons.yml 的独立 items 注册；名称/悬浮信息归 GUI，配方六格仍统一真实产物信息。
- 本地 leaf 已备份并清理上述注册；云端自动 JAR 交付不包含 CE 资源。2026-11-13 用户反馈名称框紫黑缺失后，已定向补齐云端160个item model入口、备份移除三份旧装饰注册，执行CE重载和default工作流，并检查generated/hosted两个ZIP的160套入口、geometry、PNG及哈希一致。后续模型入口变更仍须独立同步及重建资源包，不能把JAR上传当作资源已上线。

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
FFFFFFLFP
FFFSSSFCF
FWFSSSFCF
FFFSSSFCF
FFFFXFFFF
```

- 零基槽位：武器 W=19；强化石 S=12、13、14、21、22、23、30、31、32；确认 C=16、25、34；幸运 L=6、保护 P=8在右上角；关闭 X=40在底栏正中（用户2026-11-09最新布局）。业务以 Shape 映射为源，不硬编码这些索引。装备、强化文字和上方槽位为一体框；两符文仅显示真实CE图标，不烘焙文字，悬浮文案保留。
- 除输入物品、符文图标外使用像素背景和透明按钮，不恢复满屏玻璃或实物按钮；底部两个符文装饰留空，不连成一条。
- 预览与执行共用 ForgePlan；展示当前/下级攻击、实际概率、金币、放入/实扣/返还数量。
- 武器仅允许单件；左右键和拖拽只允许合法输入。未实现的 Shift、数字键、双击等批量动作继续拦截，避免越槽与复制。
- 关闭、退出、重载时返还托管输入，先清空槽再入背包，溢出掉落；回滚同时恢复本次 GUI 输入和玩家背包。
- 用户2026-11-09最新要求：强化成功或失败后临时关闭显示结果Title，武器及未消耗强化石继续托管在原GUI对应输入槽，30tick后重开同一菜单并保留符文开关，不临时返还背包，不受背包空位影响。真正关闭、退出、重载、停用、另开菜单取消重开并先清空原槽再返还，避免重复。标题动态显示当前武器真实名称、取出后清除，原版名称保留客户端翻译；公开Bukkit API重建标题时保持同一Holder及完整输入，屏蔽临时关闭返还。校验或扣费失败保持原菜单。
- 重绘、点击与重开检查插件有效性、在线、权限、配置和会话身份；重开不能覆盖玩家另开的菜单。
- 背景 `resourcepack/assets/starforge/textures/font/gui/forge_menu.png` 为 176×110，CE 图片 ID starforge:forge_menu，当前 height=110、ascent=13；改 Shape 必须同步图形位置并检查悬浮/点击对齐。
- 标题 starforge:ui 引用已有 xerr_font:fusion_pixel_quest_12_sc.ttf，size=12、oversample=1；来源为 leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font，不能复制修改字体原件。
- 本会话已允许直接生图，素材任务可沿用授权并遵循对应技能；普通业务、配置和文案修改不触发重新制作素材。

## 资源、文本与指令

- config.yml 只放全局上限、保底、幸运、公告等级、属性后端与外部材料路径；等级、武器、GUI、语言放各自文件，不新增无效配置。
- 默认全量配置仅在全新数据目录首次释放；用户2026-11-08明确要求GUI缺失自动释放：启动和重载时在异步配置加载阶段补齐发行JAR中缺失gui/*.yml（新增类型页后共六个），已有GUI内容、注释和未知字段不覆盖，不自动替换无效配置。非GUI配置仍不补回；GUI发布使用临时文件完整写入后移动，不覆盖目标，失败记录路径与原因并保留旧配置快照。
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

## 原生箱子物品工坊

- 用户2026-11-09要求配方卡片整个2行×3列六格按钮一致：背景透明按钮、真实装备图标、名称框共用真实产物的名称和完整动态Lore及同一个配方点击行为，不再让名称框使用独立CE说明或只扩展背景点击。保留各格模型，空卡不生成按钮；类型页同样六格共用其类型定义，不能覆盖用户插入的导航/其他动作。

- `/sf craft` 只打开原生箱子菜单，不接入 Rafter；配方列表、详情、制作记录分别为 `gui/crafting.yml`、`gui/crafting-detail.yml`、`gui/crafting-orders.yml`，一文件一菜单。
- 用户2026-11-08在六工位预览后确认“做，图标支持ce的物品”：只有我的锻造恢复像素背景craft_orders_six（176×110），最新反馈认可紧凑底栏：去掉大号返回/关闭文字，返回小图标位于底栏正中（B=40），关闭按钮与贴图移除，页码信息也移除，正常六工位只显示返回，底行FFFPBNFFF；只有存在可去页面才显示翻页箭头，恢复独立原生物品栏标题区，不再试图用背景覆盖原生文字。使用五行Shape，两层各3位，下方CE进度物品直接显示0～100%和进度条，每秒更新，完成点击原物品或进度领取。背包槽从y122开始不覆盖，实际显示和点击需客户端验收。默认订单菜单无页码/info组件，info仅作为可选动作兼容旧自定义配置；旧超额单必要时仍有条件翻页，不限制领取。配方仍4×7普通箱子，确认页保留用户布局，旧三张crafting背景不恢复；强化forge_menu/字体/材料及Rafter退役不变。订单图标从冻结物品反序列化，保留CE/ItemHub的名称/Lore/PDC/模型；GUI功能图标可直接用craft-engine完整ID，原版仍用material。位置按Shape映射，进度同行列下一行；修改Shape须同步背景。
- 默认解锁/学习后显示、背包扣料扣费、逐物品进度、离线计时和冻结产物领取保持独立业务逻辑，不能退化为即时合成，也不能与强化混用。
- 用户2026-11-08确认同配方同时锻造、每份独立计时。无共享冷却组时用独立订单编号，每次确认一份、单独扣费扣料、逐单领取；显式cooldown-group仍限制每玩家组内一单。配方详情用于新单确认，从我的锻造进入才绑定具体订单；旧schema 1订单继续读取领取，不清空或改写旧存档。
- recipes.yml的max-active-orders默认6（1～54），每玩家独立，prepared/running/claiming与完成未领取均占位；玩家预检及单写入器原子预留都检查，未付费释放/成功领取才释放。降低上限不删旧单，旧超额单保留分页领取。进度为普通CE物品平面模型，不用NMS、不每秒重开窗口、不改着色器；只有成功领取后重开刷新标题占用数。
- 配方悬浮说明直接显示所有真实材料名称、所需/拥有/缺少数量，充足绿、不足红；原版名称保留客户端翻译，CE/ItemHub名称直接来自物品，不复制到配置。用户自定义Shape和按钮位置不得整页覆盖；close均可省略，详情previous/next可省略，仅保留confirm/back也可加载，以Esc关闭。
- 旧 Rafter 工坊源码及专属工具归档到 ignored 的 `artifacts/retired-rafter/`，不参与构建。实服专属菜单、模板、图片和字体先备份再移除；保留服务器其他菜单和插件本体。
- 退役 Rafter 工坊不能残留全局着色器覆盖：CE 不再合并 `Rafter/generate/resource-pack`，移除 `rafter-xerr-compat` 资源及只为其设置的四文件冲突规则，保留其他合包来源与冲突规则。26.2/26.3 的 text.vsh/text.fsh 恢复为 Xerr 原生生成版本；必须检查实际托管ZIP而非只检查源目录。2026-11-08 客户端曾出现 `rafterFrame` 未初始化警告及三角拉伸，编译通过不能证明组合着色器安全。
- 新插件正常重启加载后，CE 配置与资源移除执行 `/ce reload`、`/ce workflow default`，客户端接受资源包再验收。只落盘、单测和构建不代表实机显示、点击与交易已经通过。


## 2026-11-09 工坊类型与双页像素UI（覆盖旧普通配方/确认布局）

- 用户认可framed-navigation-v1后要求“做”：类型/装备列表返回灰框居中，B=39/40/41；我的锻造铜框在右下，T=42/43/44并有小铁砧。类型根页B以close返回游戏，装备页B返回类型。装备/铁剑名称铜框随有效卡片显示，五空卡不显示名称框。相关Shape最后一行类型FFFBBBTTT、配方PNFBBBTTT；P/N只在有可去页时显示。强化另有同画风预览请求，尚未授权落地强化。

- 用户明确六个位置是类型预留，目前只显示装备，其他五个类型不显示文字/图标且不可点击。`/sf craft` 进入独立 `gui/crafting-types.yml`，装备进入原 `gui/crafting.yml`，点击真实已解锁配方进入 `gui/crafting-detail.yml`；不新增假配方。
- 三页分别使用 craft_types_six / craft_gallery_six（176×110）和 craft_detail_six（176×128）。装备卡片下为具体装备名称，不用“查看配方”；CE名称图标由GUI `elements.S.captions` 按配方ID映射，新增/改名需同步CE名称资源，缺映射时省略表面名称而保留真实物品悬浮名称，不显示通用错误名称。
- 确认页两层三列六材料，每个真实材料下一行同列显示拥有/需要，绿色充足、红色不足；原生 custom_model_data floats/flags + composite/range_dispatch 字形模型，不用NMS、shader或重开窗口。超过六材料保留条件分页；完整数量、费用和耗时在悬浮说明保留。
- CE gui_blank内部按钮允许GUI name；其他CE名称/Lore/PDC/模型仍保留。缺失GUI自动释放新增类型页后共六文件，已有文件不覆盖。强化、我的锻造六位及存档不改；Rafter/Canvas不恢复。
- 本轮66测试及package通过、资源和57文本渲染检查已执行，JAR已暂存update。尚未重启、CE合包或真实客户端验收，不把工程预览当作游戏截图。

## 装备品质约定（2026-11-16）

- 品质业务属于AsterStats；StarForge通过ItemHubGenerationApi的RANDOM模式生成一次后保存订单物品字节，GUI使用PREVIEW，不得随机生成预览或领取。兼容新asterstats:quality_roll与旧itemhub:quality_roll订单；材料路径不变，随机品质每单1件。强化fallback通过通用integration读取该件最终属性，不再依赖ItemHubQualityApi。
- 用户2026-11-16要求领取后才显示品质：全部未领取订单（制作中、完成待领取、claiming）的图标与详情使用CraftingItems.orderPreview，隐藏真实品质、成色、属性和品质概率；仅保留基础装备名及订单进度/领取说明。不得修改存档快照或在领取时重抽；CraftingDelivery继续读取原itemBytes。未提交的新配方预览仍可展示范围/概率。
