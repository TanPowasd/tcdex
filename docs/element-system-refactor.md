# TCDEX 元素体系重构设计文档

> 目标：融合《命运2》与《原神》的元素体验，建立一套清晰、可扩展、数据驱动的元素体系。

---

## 1. 新元素分类

| 分类 | 元素 | 中文名 | 对应现有元素 | 定位 |
|---|---|---|---|---|
| 光能元素 | SOLAR | 烈日 | 现有 SOLAR | 灼烧、引爆、高伤害 |
| 光能元素 | VOID | 虚空 | 现有 VOID | 挥发、削弱、持续压制 |
| 光能元素 | ARC | 电能 | 现有 ARC | 连锁、麻痹、增幅 |
| 暗影元素 | STASIS | 冰影 | 现有 STASIS | 减速、冻结、粉碎 |
| 暗影元素 | STRAND | 缚丝 | 现有 STRAND | 束缚、悬挂、减伤 |
| 暗影元素 | MOON | 月 | 新增 | 暗影控制、月蚀标记、棱镜暗面 |
| 中性元素 | MISTFLOW | 罡流 | 现有 MISTFLOW | 扩散、位移、聚怪 |
| 中性元素 | TIDE | 水 | 现有 TIDE | 潮湿、导电、冻结媒介 |
| 中性元素 | SINKSTAR | 落星 | 现有 SINKSTAR | 重力、结晶、护盾 |
| 特殊元素 | KINETIC | 动能 | 现有“无元素” | 纯物理，不参与反应 |
| 特殊元素 | PRISM | 棱镜 | 现有 PRISM | 万色棱镜，可参与特殊反应 |

> 动能建议继续作为“无元素”伪元素处理，不进入 `ElementType` 主枚举，避免破坏现有 `null = 动能` 的伤害流程。

---

## 2. 核心数据结构

### 2.1 `ElementCategory`

```java
public enum ElementCategory {
    LIGHT,      // 光能
    DARK,       // 暗影
    NEUTRAL,    // 中性
    SPECIAL     // 特殊
}
```

### 2.2 `ElementType`

现有枚举保留英文 ID，增加 `MOON`：

```java
public enum ElementType {
    SOLAR,      // 烈日
    VOID,       // 虚空
    ARC,        // 电能
    STASIS,     // 冰影
    STRAND,     // 缚丝
    MOON,       // 月（新增）
    MISTFLOW,   // 罡流
    TIDE,       // 水
    SINKSTAR,   // 落星
    PRISM       // 棱镜
}
```

### 2.3 `ElementDefinition`

```java
public final class ElementDefinition {
    private final ElementType type;
    private final String id;               // 注册名：solar / arc / void / moon ...
    private final String displayName;      // 中文显示名：烈日 / 电能 / 月 ...
    private final ElementCategory category;
    private final int color;
    private final ParticleOptions particle;
    private final float stacksPerHit;
    private final int stateDuration;
    private final float doTPerStack;
    private final float auraPerHit;
    private final boolean pseudo;          // true = 动能等非正式元素
    private final boolean reactionParticipant;
    private final List<ElementKeyword> keywords;
    private final ElementEffectProcessor processor;
}
```

### 2.4 `ElementRegistry`

```java
public final class ElementRegistry {
    public static void register(ElementDefinition definition);
    public static ElementDefinition get(ElementType type);
    public static ElementDefinition getById(String id);
    public static Collection<ElementDefinition> all();
    public static Collection<ElementDefinition> byCategory(ElementCategory category);
}
```

---

## 3. 元素机制规划

### 3.1 光能元素

| 元素 | 状态机制 | 关键词 |
|---|---|---|
| 烈日 SOLAR | 每击叠层，持续灼烧 DoT | Ignite 引爆 |
| 虚空 VOID | 标记，受击爆炸 | Volatile / Weaken / Devour |
| 电能 ARC | 标记，受击连锁 | Jolt / Blind / Amplified |

### 3.2 暗影元素

| 元素 | 状态机制 | 关键词 |
|---|---|---|
| 冰影 STASIS | 叠层减速 → 冻结 | Shatter |
| 缚丝 STRAND | 叠层束缚 | Sever / Suspend / Woven Mail |
| 月 MOON | 新增：月蚀标记 + 持续暗影伤害 | Moon Mark：持续 DoT、受击净化、暗影禁锢 |

月元素机制：

- 施加“月蚀”标记，持续造成暗影伤害。
- 月蚀标记存在时，目标受到光能元素伤害会被“净化”，触发额外暗影爆发。
- 月与棱镜存在特殊暗面反应：棱镜 + 月 → 月棱镜爆发。

### 3.3 中性元素

| 元素 | 定位 | 机制 |
|---|---|---|
| 罡流 MISTFLOW | 扩散 / 位移 | 把已有元素扩散到周围敌人，聚怪 |
| 水 TIDE | 潮湿 / 媒介 | 使目标潮湿，与电能/冰影/烈日产生反应 |
| 落星 SINKSTAR | 重力 / 结晶 | 聚怪、重力压制、生成结晶护盾 |

### 3.4 特殊元素

| 元素 | 定位 |
|---|---|
| 动能 KINETIC | 无元素反应，只参与物理伤害、破盾效率 |
| 棱镜 PRISM | 万色棱镜，可与光能/暗影/中性元素触发强化反应，也能破任意元素盾 |

---

## 4. 反应矩阵设计

### 4.1 反应原则

- 光能 × 暗影：强反应，伤害高 / 控制强。
- 光能 / 暗影 × 中性：元素媒介反应，如蒸发、导电、冻结、扩散、结晶。
- 中性 × 中性：辅助反应，如聚怪、扩散、结晶。
- 棱镜 × 任意元素：特殊强化反应。
- 动能：不参与反应。

### 4.2 当前反应表（玩家提供）

| 已有元素 aura | 触发元素 trigger | 催化剂 catalyst | 反应 | 类型 / 附加 |
|---|---|---|---|---|
| 烈日 SOLAR | 冰影 STASIS | - | 融化 | DAMAGE + 挂水 TIDE |
| 烈日 SOLAR | 水 TIDE | - | 蒸发 | DAMAGE |
| 电能 ARC | 水 TIDE | - | 感电 | DAMAGE / CONTROL |
| 冰影 STASIS | 水 TIDE | - | 冻结 | CONTROL |
| 虚空 VOID | 罡流 MISTFLOW | - | 虚空扩散 | DIFFUSION |
| 虚空 VOID | 水 TIDE | - | 暗流涌动 | DAMAGE / CONTROL |
| 虚空 VOID | 落星 SINKSTAR | - | 虚空结晶 | SHIELD |
| 月 MOON | 虚空 VOID | 落星 SINKSTAR | 月结晶 | SHIELD（三元） |
| 月 MOON | 电能 ARC | 水 TIDE | 月感电 | DAMAGE / CONTROL（三元） |
| 月 MOON | 缚丝 STRAND | - | 蜕散 | DAMAGE / CONTROL |
| 冰影 STASIS | 电能 ARC | - | 聚导体 | AMPLIFY |
| 罡流 MISTFLOW | 水 TIDE | - | 扩散 | DIFFUSION |
| 罡流 MISTFLOW | 烈日 SOLAR | - | 扩散 | DIFFUSION |
| 月 MOON | 冰影 STASIS | - | 极致冰流 | CONTROL / DAMAGE |
| 棱镜 PRISM | 月 MOON | - | 月之暗面 | DAMAGE / CONTROL |

> 三元反应需要目标同时拥有 aura、trigger 和 catalyst 三种元素附着。
> 二元反应自动注册反向，因此“烈日-冰影”和“冰影-烈日”都能触发融化；三元反应只按指定方向触发。

---

## 4.3 反应模块系统（类似匠魂词条 Hook）

每个元素反应现在实现为一个 `ElementReactionModule`：

- 内置模块根据 `ElementReactionRegistry` 自动注册到 `ReactionModuleRegistry`
- 反应触发时由 `ElementReactionEvents` 查找模块并调用 `onTrigger`
- 模块可以：
  - 决定是否触发：`canTrigger`
  - 执行效果：`onTrigger`
  - 调整伤害 / 持续时间 / 范围：`modifyDamage` / `modifyDuration` / `modifyRadius`
- 不需要玩家给工具打词条，满足条件自动触发
- Add 包可通过 `TcdexReactionAPI.registerReactionModule(...)` 注册自定义模块

## 4.4 玩家反应词条（默认全部开启）

- 每个反应对应一个词条 ID，例如：
  - `solar_stasis`：融化
  - `moon_void_sinkstar`：月结晶
- 玩家默认拥有并启用所有反应词条
- 可以通过 Capability 禁用特定词条：
  - `TcdexReactionAPI.disableReactionModifier(player, reaction)`
  - `TcdexReactionAPI.enableReactionModifier(player, reaction)`
- 触发反应时，如果来源是玩家，会检查玩家是否启用了该反应词条；默认全部启用，因此不额外配置也能触发全部反应
- 存储使用 Forge Capability：`PlayerReactionModifiersCapability`

## 5. 重构模块划分

```text
element/
├── ElementCategory.java
├── ElementType.java
├── ElementDefinition.java
├── ElementRegistry.java
├── ElementKeyword.java
├── ElementEffectProcessor.java
├── processor/
│   ├── SolarProcessor.java
│   ├── VoidProcessor.java
│   ├── ArcProcessor.java
│   ├── StasisProcessor.java
│   ├── StrandProcessor.java
│   ├── MoonProcessor.java
│   ├── MistflowProcessor.java
│   ├── TideProcessor.java
│   ├── SinkstarProcessor.java
│   └── PrismProcessor.java
├── shield/
│   └── ElementShieldManager.java
├── resistance/
│   └── ElementResistanceManager.java
├── reaction/
│   ├── ElementReaction.java
│   ├── ElementReactionModule.java
│   ├── ReactionContext.java
│   ├── ReactionModuleRegistry.java
│   ├── ElementReactionRegistry.java
│   └── module/
│       ├── DamageReactionModule.java
│       ├── ControlReactionModule.java
│       ├── AmplifyReactionModule.java
│       ├── ShieldReactionModule.java
│       └── DiffusionReactionModule.java
└── api/
    └── TcdexElementAPI.java
```

---

## 6. 迁移步骤

1. 新增 `ElementCategory`、`ElementDefinition`、`ElementRegistry`。
2. 更新 `ElementType`：加入 `MOON`，保留现有英文 ID。
3. 将 `ElementManager` 中的抗性、护盾、权重、怪物攻击拆分到独立 Manager。
4. 将 `ElementalStateEvents` 中的关键词逻辑迁移到 `ElementEffectProcessor`。
5. 将 `ElementReactionEvents` 拆成 `ReactionEngine` + `ReactionRegistry`。
6. 统一 `TcdexElementAPI`，开放自定义元素、处理器、反应注册。
7. 最后再处理 `IElementalEntity` 接口拆分和 Mixin 重构。

---

## 6.5 当前实现进度

已完成：

- [x] 新增 `ElementCategory`
- [x] 新增 `ElementDefinition` / `ElementRegistry`
- [x] `ElementType` 新增 `MOON`
- [x] `ModDamageSources` 新增 MOON 伤害类型
- [x] `ElementManager` 拆分为门面，内部委托给：
  - `ElementResistanceManager`
  - `ElementShieldManager`
  - `ElementMonsterAttackManager`
- [x] 元素反应矩阵：已按玩家提供表注册（含三元反应数据结构与基础触发）
- [x] 数据驱动化：已开放 `TcdexElementAPI.registerElement` / `registerElementEffect`，`TcdexReactionAPI.registerReaction` 已可用
- [x] 玩家反应词条：Capability 已实现，默认全部开启，可单独禁用
- [x] 关键词处理器 `ElementEffectProcessor`：框架已建，Moon 处理器已接入；其余关键词后续迁移
- [x] `IElementalEntity` 接口拆分：已拆出 `ElementStateHolder` / `ElementShieldHolder` / `ElementCombatHolder` 子接口

## 7. 兼容性策略

- 保留 `ElementType.SOLAR / VOID / ARC / STASIS / STRAND / MISTFLOW / TIDE / SINKSTAR / PRISM`。
- `MISTFLOW` 显示名从“岚流”改为“罡流”。
- `TIDE` 显示名从“潮汐”改为“水”。
- `SINKSTAR` 显示名从“沉星”改为“落星”。
- `ARC` 显示名从“电弧”改为“电能”。
- 新增 `MOON`，暂不参与元素充能随机，后续再开放。
- 动能继续使用 `null` 表示，避免大规模改动伤害源。
