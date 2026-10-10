# 开发日志

## 0.1.0

> 中文开发日志。记录截至 0.1.0 的新增内容与开发环境。
> 版本号、依赖版本以 `gradle.properties` 为唯一来源；测试方法见 `TESTING.md`；
> 逐条可行性结论见工作区根目录的 `机械动力火炮附属开发大纲-开发任务报告.docx`。

---

## 一、开发环境

### 运行环境

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.228 |
| Java | OpenJDK 21.0.3 LTS（Microsoft 构建） |
| Gradle | 8.12（wrapper 已提交） |
| 构建插件 | ModDevGradle 2.0.148（`net.neoforged.moddev`） |

`gradle-wrapper.properties` 中的分发地址指向腾讯云镜像
（`mirrors.cloud.tencent.com`）。原因是 `services.gradle.org` 会重定向到 GitHub，
而当前网络访问 GitHub 会停滞（实测约 312 B/s）。这是**本机网络环境的权宜之计**，
首次构建若走官方地址可改回。

### 依赖

必装（缺失会导致加载失败）：

| 模组 | 版本 | 说明 |
| --- | --- | --- |
| Create | 6.0.10-281 | 本体，提供 Registrate、配方类型、应力、流体、无线红石 |
| Create Big Cannons | 5.11.7 | 被附属的目标本体 |
| Ritchie's Projectile Library | 2.1.2 | CBC 的弹体前置 |

可选（缺失只影响对应兼容，不影响加载）：

| 模组 | 版本区间 |
| --- | --- |
| Sable | [2.0.0, 3.0.0) |
| Create Aeronautics | [1.3.0,) |
| Simulated | [1.3.0,) |
| Offroad | [1.3.0,) |
| Copycats | [3.0.0,) |
| Framed Blocks | [10.0.0,) |
| Curios | [9.2.0,) |

编译期库版本刻意与 Create 6.0.10-281 在 `META-INF/jarjar/` 内自带的版本对齐
（Registrate `MC1.21-1.3.0+67`、Ponder `1.0.82+mc1.21.1`、Flywheel `1.0.6`），
以保证编译期与运行期一致。

`imgui-mc` **不声明为依赖**（CBC 的 optional 依赖，本附属不使用）。

### 开发运行环境

`run/`、`run-server/` 等开发用游戏目录不入库。所有运行配置共用 `run/` 目录，
由 `syncMods` 任务自动把外部模组 jar 放入 `run/mods`，无需手工准备。

---

## 二、0.1.0 新增内容

### 2.1 模组骨架（第 0 期）

| 项目 | 值 |
| --- | --- |
| modId | `createbigcannons_theroarofsteel` |
| 资源／数据命名空间 | `cbc_rs` |
| 基础包 | `com.cbcrs` |
| 显示名（中／英） | 机械动力火炮：钢铁轰鸣 / Create Big Cannons: The Roar of Steel |

**双标识设计**：modId 采用描述性全名（避免冲突、便于依赖声明），资源命名空间用
短名 `cbc_rs`（资源路径与注册 ID 更短）。两者刻意不同，详见 `README.md` 第 3 节。

**命名空间隔离**（设计决定 ⑦）：火炮内容用 `cannon_*`，机炮内容用
`autocannon_*`，两套不共用任何 tag、数据组件类型、菜单类型或资源路径。0.1.0
已落地三级口径标签骨架（`cannon_caliber/{small,medium,large}` 与
`autocannon_caliber/{small,large}`，方块与物品两侧共 10 份空标签文件）。

**类结构**：

```
com.cbcrs
├─ TheRoarOfSteel              入口；MOD_ID / NAMESPACE / resource()
├─ TheRoarOfSteelRegistrate    唯一的 CreateRegistrate 实例
├─ CBSReference                ID 辅助（cannon() / autocannon() / loc()）
├─ index/
│  ├─ CBSIndex                 注册顺序的唯一入口
│  ├─ CBSItems                 钨锭
│  ├─ CBSMaterials             粗白钨/粗黑钨、粉碎白钨矿/粉碎黑钨矿、钨粒
│  ├─ CBSOres                  白钨矿、黑钨矿方块
│  ├─ CBSTabs                  专属创造标签页
│  └─ CBSTags                  口径标签键
├─ client/TheRoarOfSteelClient 客户端入口 + 资源断言
├─ gametest/                   游戏测试（显式注册 + 12 项测试）
├─ mixin/                      空的 mixin 配置（预留）
└─ datagen/                    datagen 包（预留）
```

### 2.2 物品与方块

共 **8 件**（2 个方块 + 6 个物品）。

| 内容 | ID | 类型 | 说明 |
| --- | --- | --- | --- |
| 钨锭 | `cbc_rs:tungsten_ingot` | 物品 | 两条矿链的共同终点，也是标签页图标 |
| 钨粒 | `cbc_rs:tungsten_nugget` | 物品 | 承接原版「粉碎矿 → 9 个粒」范式 |
| 粗白钨 | `cbc_rs:raw_scheelite` | 物品 | 白钨矿掉落物 |
| 粗黑钨 | `cbc_rs:raw_wolframite` | 物品 | 黑钨矿掉落物 |
| 粉碎白钨矿 | `cbc_rs:crushed_raw_scheelite` | 物品 | 白钨侧中间产物 |
| 粉碎黑钨矿 | `cbc_rs:crushed_raw_wolframite` | 物品 | 黑钨侧中间产物 |
| 白钨矿 | `cbc_rs:scheelite_ore` | 方块 | 石头层专属，需石制工具 |
| 黑钨矿 | `cbc_rs:wolframite_ore` | 方块 | 深板岩层专属，需石制工具 |

命名遵循 Create 的 `crushed_raw_*` 约定，与源贴图文件名一致。

### 2.3 矿石生成

两种矿**层位互斥**，通过「只给一个目标方块规则（target）」硬性实现，不依赖高度、
也不受其它模组生成石头／深板岩的影响：

| 矿 | target | 结果 |
| --- | --- | --- |
| 白钨矿 | 仅 `minecraft:stone_ore_replaceables` | 只生成在石头层 |
| 黑钨矿 | 仅 `minecraft:deepslate_ore_replaceables` | 只生成在深板岩层 |

套用的原版分布逻辑与实测参数（取自工作区内的反编译源码）：

| 矿脉 | 次数 | 高度分布 | 套用对象 |
| --- | --- | --- | --- |
| `scheelite_ore_main` | 2 | 三角分布 y -32～32 | 青金石主脉 |
| `scheelite_ore_buried` | 4 | 均匀分布 y -64～64 | 青金石埋藏脉 |
| `wolframite_ore_main` | 4 | 三角分布 y -64～32 | 黄金主脉 |
| `wolframite_ore_secondary` | 10 | 均匀分布 y -48～-16 | 黄金副脉（高度收紧，次数下调） |

矿脉大小：白钨矿 7，黑钨矿 9（含空气暴露丢弃系数 0.5）。
黑钨矿副脉高度由原版黄金的 y 32～256 **收紧到 y -48～-16**，使其完全落在深板岩层
内——原版高度位于石头层，与「只生成在深板岩层」冲突（设计决定 ⑰）。
**次数由原版黄金的 50 下调为 10**：高度收窄后密度被放大约 7 倍（原版 50 次摊在 225 格
高度上，收窄后只剩 33 格），实测手感偏密，故下调。10 对应「略多于原版黄金」的丰度
（主脉:副脉 约 1:7.3，原版黄金约 1:5.4）。
未复刻原版黄金的 `ORE_GOLD_LOWER`（y -64～-48）。

注册方式：3 份 datapack registry JSON（configured feature / placed feature /
biome modifier），全部走 bootstrap，**零新增代码**，也未使用运行时注入。

### 2.4 矿石掉落

直接采用**原版自己的**掉落表构造器（`createOreDrop`），与铁矿石／金矿石完全同构：

| 情况 | 掉落 |
| --- | --- |
| 无附魔 | **恰好 1 个**对应粗矿 |
| 精准采集 | 矿石方块本身 |
| 时运 | 走 `minecraft:ore_drops`，按原版公式放大 |

⚠️ 掉落表由 **datagen 生成**，不存在手写版本。原因见 `TESTING.md`：`src/generated/resources`
的资源根顺序**早于** `src/main/resources`，手写表会被静默遮蔽（此坑已由游戏测试抓出）。

### 2.5 配方（共 20 份）

| 类别 | 数量 | 内容 |
| --- | --- | --- |
| `create:crushing` | 4 | 2 种矿石 + 2 种粗矿 |
| `create:splashing` | 2 | 2 种粉碎矿 |
| `minecraft:smelting` | 6 | 矿石×2、粗矿×2、粉碎矿×2 |
| `minecraft:blasting` | 6 | 同上 |
| `minecraft:crafting` | 2 | 九合一（9 钨粒 → 1 钨锭）与分解（1 钨锭 → 9 钨粒） |

**粉碎**（分别对齐原版铁矿石与深层铁矿石）：

| 输入 | 时间 | 产出 |
| --- | --- | --- |
| 白钨矿 | 250 | 1 + 0.75 概率 1 个粉碎白钨矿、0.75 经验粒、0.125 圆石 |
| 黑钨矿 | 350 | 2 + 0.25 概率 1 个粉碎黑钨矿、0.75 经验粒、0.125 深板岩圆石 |
| 粗白钨／粗黑钨 | 400 | 1 个对应粉碎矿、0.75 经验粒 |

**洗涤**（固定产出 9 个钨粒）：

| 输入 | 副产物 |
| --- | --- |
| 粉碎白钨矿 | 1 粘土球（15%） |
| 粉碎黑钨矿 | 1 铁粒（15%）+ 1 玫瑰石英（2%） |

**熔炼／高炉**：完全沿用原版 MC 与原版 Create 的取值，四种输入（2 粗矿 + 2 粉碎矿）
全部产出同一个钨锭，且不引入自定义熔炼类型，以保证与其它模组熔炉及 Create
批量熔炼兼容。

### 2.6 资源与本地化

- **贴图 8 张**：6 张物品图标（16×16，带透明通道）+ 2 张方块贴图（16×16，全不透明）。
- **语言键 13 条**：`en_us` 由 datagen 生成，`zh_cn` 手工维护。
- **标签**：矿脉／挖掘工具／`c:` 通用材料（raw_materials、crushed_raw_materials、
  nuggets 系列）与 `minecraft:*_ores` 共 12 份。
- **模型与方块状态**：8 个物品模型 + 2 个方块模型 + 2 个 blockstate，全部由
  datagen 生成。

---

## 三、0.1.0 不包含的内容

以下按规划**尚未**实现，属后续版本：

- **钨钢材质链**：钨钢锭、熔融钨钢、钨钢火炮铸模
- **钨钢火炮系列**：小／中／大口径炮管与炮闩、对应铸模、模型与贴图
- **膛压与散布机制改造**：bar 语义膛压状态机、内弹道求解、全新散布角机制、
  装填期计算缓存
- **装药体系**：装药槽位、装药倍率组件、发射药包改名与数值调整
- **机构**：辅助推弹机、漏夹、链式装弹机
- **雷管**：无线红石收发器式起爆装置
- 以上均未触碰 CBC 内部逻辑，**0.1.0 不含任何 mixin 实现**（mixin 配置为空）

---

## 四、验证状态

| 项目 | 结果 |
| --- | --- |
| `gradlew build` | 成功，产物 `createbigcannons-theroarofsteel-0.1.0.jar`（约 63 KB） |
| `gradlew runData` | 成功，生成物品/方块模型、blockstate、en_us 与掉落表 |
| `gradlew runGameTestServer` | **全部 12 项测试通过** |
| `gradlew runClient` | 8/8 贴图与 8/8 模型加载通过，会话零 ERROR / 零 FATAL |
| 离线结构自检 | `_tools/verify_scaffold.py` **235 项全部通过** |
| 配方加载 | `Loaded 3111 recipes` = 基线 3091 + 本模组 20 |

游戏测试覆盖：物品与方块注册、方块物品可用性、堆叠合法性、**全局只有一个钨锭**
（设计决定 ⑯ 的约束）、三种掉落行为（无附魔／精准采集／时运，时运采样 200 次
并与原版铁矿对照）、生成数据已注册、20 份配方全部加载且无解析错误、
钨粒与钨锭双向合成。

### 已知风险

1. **Sable 与 Flywheel 冲突**：Sable 启动时自行声明会覆盖着色器并替换光照存储，
   预期造成兼容性问题。两者均为本项目必装，无法通过配置消除。任何游戏内光照／
   视觉异常应优先怀疑此项。
2. **Sable 子世界报错**：进入世界时可能出现一条
   `Received a sub-level movement packet for a non-existent sub-level`，
   不会崩溃。0.1.0 的一次实测客户端会话未复现。
3. **开发环境噪声**：空的 mixin 配置会产生两条 `refmap.json could not be read`
   提示；`framedblocks` / `iris` 未安装会产生可选兼容 mixin 的目标缺失警告。
   以上均为预期，不影响功能。

---

## 五、开发过程中的重要约定

1. **不改 CBC 的注册 ID**。改名一律通过附属模组的 lang 覆盖实现。
2. **不扩展 CBC 的 record 数据类**（`BigCannonMaterialProperties` 等），
   避免客户端与服务端反序列化不一致。
3. **不在每 tick 遍历整台火炮结构**，为后续 Sable 子世界与大型空中炮舰预留性能余量。
4. **`neoforge.mods.toml` 保持纯 ASCII**：Gradle `expand()` 以平台默认编码读取模板，
   写入中文会导致乱码；中文只放在 lang 文件中。
5. **注册 ID 一旦发布不可随意改名**：改名会让已存档的物品解析失败。
   开发期遇到此问题通过重建测试世界解决。
