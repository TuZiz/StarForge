# 锻造菜单 crafting_menu

- 用户要求像素画菜单，直接内置生图，不使用 Canvas。最新玩法为 EpicCraftingsPlus 风格的耗时制作、逐物品进度、到时领取。
- 资产为 176×110 RGB/RGBA PNG 背景，无烘焙文字，文字由菜单配置渲染。原图保留在工具输出位置。
- 内置 imagegen 提示词要点：正交低分辨率 Minecraft 箱子菜单、紫黑曜石与铜边、左侧 3×3 配方架、中间铁砧、右侧成品框、底部翻页/锻造/关闭，硬像素边缘。
- 工具原图：C:/Users/32633/.codex/generated_images/01a1b639-2a60-7892-a71f-410103fee808/exec-6594d0b6-539b-4b5d-bf85-4577c1e2dbbe.png
- 目标：craftengine/强化石/resourcepack/assets/starforge/textures/font/gui/crafting_menu.png，CE ID starforge:crafting_menu。
- 原图与 176×110 最近邻导出均已目视检查，PNG 文件和 CE 引用已核对。游戏对齐待验证，用户认可尚未取得。

## 2026-11-08 对齐修订
- 用户实服截图证实全部错位。旧图配方框约在 x27/44/62、y35/53/71，真实目标应为 x34/52/70、y44/62/80。
- 保留紫黑曜石/铜边与铁砧，只修改槽框及底部按钮位置，新增原本缺失的“我的锻造”像素时钟。
- 精确逻辑画布 176×110。配方框中心 (34,44),(52,44),(70,44)，后两行 y62,y80；成品中心 (142,62)。按钮上一页 (16,98)、进度 (52,98)、确认 (88,98)、下一页 (124,98)、关闭 (160,98)。底部 Shape 对应 BFTFCFNFX。
- 原图/旧版保留，修订生成后导出并叠加真实网格验收。游戏实测待用户反馈。
- 修订原图：C:/Users/32633/.codex/generated_images/01a1b639-2a60-7892-a71f-410103fee808/exec-358a6c39-e7af-465a-b6c6-926f47bc390a.png。提示词要求保留美术，严格按上述坐标排列3×3槽位和五个独立底部按钮。
- 生成结果仍偏离网格，使用 .verification/crafting-alignment/export.ps1 按组件切片导出，真实槽位中心为唯一坐标源。保留旧图 crafting-menu-before.png 与生成候选 generated-v2.png，不打入发行JAR。
- 最终176×110与4倍点击网格叠图 hitboxes.png 已目视检查。配方9格、成品框、底部5按钮全部按整数坐标对齐。
- 已备份并同步实服菜单Shape与CE贴图，待 /sf reload 和 /ce workflow default 后客户端重新接受包实测。没有执行服务器重启或宣称客户端通过。
