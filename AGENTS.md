# StarForge 协作规则

- 默认中文；源码 Java25 + Maven，Paper API26.2，目标 Paper/Purpur/Folia。Java 包 `ym.starforge`。
- 开工读 pom、README 和相关源码；`src/` 为运行源码，`artifacts/` 为验证工具与证据，`.verification/` 是独立测试服，不能视为源码或发布包。
- api 为 Bukkit 服务契约；config 只在 async 读取文件；item 管单件数据和幂等重建；transaction 管准备/扣费/回滚；integration 隔离可选插件；gui/command 仅调服务，text 递归关闭可见子组件斜体。
- GUI只展示主手副本，不能把装备搬入GUI；实际操作须在目标玩家 scheduler 上检查主手快照、权限、配置与在线状态。
- 属性只能选择一条路径；AsterStats仅合并API额外贡献，禁止同时加原版伤害。基础属性变化必须重算，不能从旧最终值继续累加。
- 普通功能统一 starforge.use；管理员统一 starforge.admin。文案写 lang，GUI写独立 gui/*.yml；RGB与单引号，不在GUI补斜体标签。
- 默认资源仅全新数据目录释放；已有文件不覆盖、不补齐。首次导出失败应明确报错，不能静默覆盖用户配置。
- 不自动替换 leaf 插件 JAR 或重启实服。CE强化石资源是用户已授权的新增目录，已有文件不得覆盖。
- 验证用 Java25 执行 mvn package；本地依赖路径由pom属性覆盖；交付检查JAR不含API、第三方插件、测试、源码或探针。
- Vault同步API不具备与玩家存档的跨系统事务；不得声称断电绝对不丢钱。回滚失败记录tx和上下文，不自动重试扣费。
