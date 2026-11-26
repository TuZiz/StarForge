# 星锻工作台

独立 CraftEngine 资源包与分类，不依赖“强化石”包中的家具配置。

| 工作台 | CE 物品 / 家具 ID | 放置后右键 |
| --- | --- | --- |
| 强化台 | `starforge:enhancement_station` | 以玩家身份执行 `starforge:starforge` |
| 锻造台 | `starforge:crafting_station` | 以玩家身份执行 `starforge:starforge craft` |

- `configuration/categories.yml`：独立“星锻工作台”分类 `starforge:workstations`。
- `configuration/items.yml`：物品名称、说明、模型和地面放置规则。
- `configuration/furniture.yml`：独立家具类型、交互区域、菜单动作、破坏次数与回收掉落。
- `resourcepack/`：两份台体、两份像素称号框几何和一份字体定义；纹理引用原版 `minecraft:block/*`，字体复用现有 Xerr TTF 原件。

## 多格尺寸与名称

v3按约3×2格空间重做几何，实际占地约2.8×1.86格；台体统一 `scale: 1,1,1`，不再把单格模型横向拉宽。强化台为中央祭坛、独立副台与符文门架；锻造台为左炉、中砧、右工作桌。各部件保持自然比例，面UV按原版像素密度映射。台体高约1.9～2格，位置 `0,0.5,0` 抵消Java模型中心偏移；所有坐标保持Java模型边界内。

上方2.6格处显示独立的阶梯像素铜框和“强化台 / 锻造台”名称。框与字共用 `billboard: center`；文字不透墙、没有原生半透明矩形背景。修改 `configuration/furniture.yml` 中各自 `text_display.text` 即可改名；修改 `position` 可改悬浮高度，框和字应一起改。当前字体 `starforge:workstations/title` 引用 `xerr_font:fusion_pixel_quest_12_sc.ttf`，由现有Xerr合包来源提供，不复制修改字体原件。

`starforge:enhancement_nameplate` 与 `starforge:crafting_nameplate` 是内部显示物品，不放入分类；分类仍只有两个可拿取工作台。每个台体使用两排三列的六个CE交互区覆盖扩大后的外观，交互区不是实体物理碰撞箱。

管理员获取：

```text
/ce item get starforge:enhancement_station
/ce item get starforge:crafting_station
```

持物品对地面放置，四向旋转、居中对齐。右键打开对应菜单，保留 StarForge 原有玩家权限，不以 OP 身份执行。家具配置为命中三次后破坏，并通过原生 `furniture_item` 掉落回收。交互区域不是实体物理碰撞箱。

更新配置后使用 `ce reload`，模型更新后执行 `ce workflow default` 并接受新资源包；不可只复制模型而不重新合包。本包由 CE 的 `model` 字段生成客户端物品定义，不使用素材原稿中的直接 item_model 入口。

2026-11-15 v3：九个运行文件已备份同步本地leaf和云端，并回读SHA256全部一致。云端仍停止，CE重载命令提交失败；未生成新版托管包或游戏验收。正常启动后执行上述CE工作流并验收；已摆放的旧家具需要根据实际更新情况重新放置。新bbmodel源文件与简化材质工程预览位于 `D:/codex/StarForge/artifacts/workstations-v3`；Blockbench已连上，但导入需用户处理其原生插件文件访问权限框。
