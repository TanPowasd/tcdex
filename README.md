# TCDEX（原命）

> **Forge 1.20.1** 模组  
> 定位：**原神 × 命运2 融合**，并逐步加入属于“原命”自己的原创机制。

TCDEX 以原神式元素反应为战斗核心，以命运2 式光等、护盾、武器构筑为成长框架，同时接入 Tinkers Construct、冰与火、铁魔法等外部 Mod 的软联动。

---

## 目录

- [核心系统](#核心系统)
- [元素体系](#元素体系)
- [元素反应](#元素反应)
- [光等与成长](#光等与成长)
- [玩家机制](#玩家机制)
- [软依赖 / Add 包架构](#软依赖--add-包架构)
- [开发者 API](#开发者-api)
- [配置总览](#配置总览)
- [开发与构建](#开发与构建)
- [常见问题](#常见问题)
- [后续自创机制方向](#后续自创机制方向)

---

## 核心系统

| 系统 | 来源参考 | 说明 |
|---|---|---|
| 元素附着 / 元素反应 | 原神 | 目标身上有元素附着，使用另一种元素触发反应 |
| 元素关键词 | 命运2 | 灼烧、冻结、Volatile、Jolt、Sever、Refract 等状态效果 |
| 元素护盾 | 命运2 | 怪物携带元素护盾，破盾效率与攻击元素相关 |
| 光等系统 | 命运2 | 世界光等场、维度/群系偏移、时间压力、伤害修正 |
| 玩家护盾 | 命运2 | 脱战自动回复的蓝色护盾层 |
| 元素能量 / 爆发 | 原神 | 攻击/击杀/受击充能，满能量释放元素爆发 |
| 超越系统 | 原神 × 命运2 | 光暗双能量，接近原创“子职业”机制 |
| 失衡 / 破绽 | 原创 | 积累失衡值，破绽后进入处决/易伤窗口 |
| 元素残响 | 原创 | 元素攻击留下残响，可被后续攻击引爆 |
| 元素适应 | 原创 | 怪物会逐渐适应当前常用元素 |
| 圣遗物 | 原神 | Curios 槽位 + 属性加成 |
| Tinkers 词条 | 命运2 × 匠魂 | 武器元素化、光等、催化、元素精通等 |

---

## 软依赖 / Add 包架构

TCDEX 核心不强制依赖任何外部 Mod。当前除 Curios（圣遗物槽位）外，其余外部联动均为可选 Add 包。

| 外部 Mod | Add 包 | 状态 |
|---|---|---|
| Tinkers Construct | `integration.tinkers` | 已实现，`tconstruct` / `mantle` 为可选 |
| Ice and Fire | `integration.iceandfire` | 已实现，可选 |
| Iron's Spellbooks | `integration.irons_spellbooks` | 已实现，可选 |
| JEI | `integration.jei` | 计划中 |
| Curios | `integration.curios` | 当前仍为强制依赖，后续可改软 |

未安装对应 Mod 时，对应 Add 包不会初始化，不会影响 TCDEX 核心功能。

### 统一 Add 包生命周期

所有外部联动通过统一接口注册：

- `ITcdexIntegration`
- `TcdexIntegrationRegistry`
- `TcdexIntegrationBuilder`

```java
TcdexIntegrationBuilder.builder("mymod")
        .displayName("My Mod")
        .shouldLoad(() -> ModList.get().isLoaded("mymod"))
        .onInit(bus -> { /* 注册事件、桥接 */ })
        .onCommonSetup(event -> { /* 跨 Mod 初始化 */ })
        .onServerStarting(event -> { /* 服务端启动 */ })
        .register();
```

查询当前活跃联动：

```java
List<ITcdexIntegration> active = TcdexIntegrationRegistry.getActive();
boolean hasTinkers = TcdexIntegrationRegistry.isModIntegrated("tconstruct");
```

---

## 元素体系

### 元素类型

TCDEX 当前包含以下元素：

| 元素 | 英文 ID | 定位 |
|---|---|---|
| 烈日 | `solar` | 灼烧 DoT、引爆、高伤害 |
| 电能 | `arc` | 连锁闪电、致盲、增幅 |
| 虚空 | `void` | Volatile 爆炸、Weaken、Devour |
| 冰影 | `stasis` | 减速、冻结、Shatter |
| 缚丝 | `strand` | 削弱、悬挂、织甲 |
| 月 | `moon` | 月蚀标记、暗影伤害、特殊反应 |
| 罡流 | `mistflow` | 扩散、位移、聚怪 |
| 水 | `tide` | 潮湿、反应媒介 |
| 落星 | `sinkstar` | 重力、聚怪、结晶护盾 |
| 棱镜 | `prism` | Boss 专属特殊元素，强化反应 |

### 元素关键词

元素状态不只用于反应，也会触发命运2 风格关键词：

| 关键词 | 效果 |
|---|---|
| 灼烧 | 持续 DoT，满层引爆 |
| 冻结 | 减速至冻结，冻结中受击 Shatter 增伤 |
| Volatile | 虚空标记受击爆炸 |
| Jolt | 电弧连锁闪电并致盲 |
| Sever | 带缚丝标记的敌人造成伤害降低 |
| Weaken | 带虚空标记目标受到的伤害提高 |
| Refract | 棱镜标记受击折射溅射 |
| 月蚀 | 月标记持续暗影伤害，可被光能净化 |

---

## 元素反应

### 反应模型

TCDEX 使用原神式“附着量 / 消耗 / 冷却”模型：

1. 元素攻击会给目标叠加元素附着量。
2. 当目标已有元素 A，再次受到元素 B 攻击时，尝试查找 A+B 反应。
3. 如果满足附着量、冷却、催化剂等条件，则消耗附着并触发反应。
4. 反应由 `ElementReactionModule` 执行，支持伤害、控制、增幅、护盾、扩散五类。

### 反应类型

| 类型 | 说明 |
|---|---|
| `DAMAGE` | 造成额外伤害，可带小范围 AOE |
| `CONTROL` | 减速、虚弱、停止寻路、击退/聚怪等 |
| `AMPLIFY` | 给攻击者提供伤害增益 |
| `SHIELD` | 给攻击者/目标提供临时吸收盾 |
| `DIFFUSION` | 把元素扩散到周围敌人 |

### 默认反应覆盖

默认反应表已覆盖全部 **45 个无序元素组合**，并包含两个高优先级三元反应：

- 月 + 虚空 + 落星 → 月结晶（护盾）
- 月 + 电能 + 水 → 月感电（伤害）

所有二元反应会自动注册反向，因此任意顺序触发都能生效。自定义反应可通过 `TcdexReactionAPI.registerReaction(...)` 动态注册。

### 反应优先级

- 每个反应都可设置 `priority`。
- 同一目标身上有多种可反应元素时，引擎按优先级从高到低选择。
- 优先级相同时，选择附着量更高的元素。
- 三元反应和棱镜强化反应默认拥有较高优先级。

### 反应数值调整

反应触发后会经过一套完整的数值修正管线：

```text
基础反应数值
  → Tinkers REACTION Hook
  → 武器催化
  → 元素精通
  → 模块 modifyDamage / modifyDuration / modifyRadius
  → 实际生效数值
```

因此元素精通、匠魂词条和武器催化都会真实影响反应强度。

---

## 光等与成长

### 光等系统

命运2 风格光等，用于修正玩家与怪物之间的伤害倍率。

- 每个匠魂工具/盔甲会根据材料阶级、强化等级和灌注计算光等。
- 普通武器有统一的基础光等。
- 怪物有基础光等，并按世界光等场、维度、群系、距离、时间压力动态调整。
- 光等差距会修正：
  - 玩家对怪物造成的伤害；
  - 怪物对玩家造成的伤害。

### 成长路线

- 光等：提升装备、灌注“光之精华”、探索高光等区域。
- 元素精通：提升元素反应伤害、范围、持续，并降低冷却和附着消耗。
- 圣遗物：提供属性、光等、元素精通、充能效率、护盾加成。
- 元素能量：通过攻击、击杀、受击充能，满能量释放元素爆发。

---

## 玩家机制

| 机制 | 说明 |
|---|---|
| 玩家护盾 | 脱离战斗后自动快速回复，可被元素状态削弱回复速度 |
| 元素能量 | 单一通用能量条，满 100 后释放当前武器元素爆发 |
| 超越 | 光/暗双能量，激活后临时增强攻击，并可配合元素爆发触发棱镜融合爆发 |
| 失衡 / 破绽 | 战斗中积累失衡值，满值进入破绽窗口 |
| 元素残响 | 元素攻击留下残响，可引爆造成额外伤害 |
| 元素适应 | 怪物会逐渐抵抗频繁使用的元素 |
| HUD | 玩家护盾条、Buff 列表、元素能量条、怪物护盾/元素附着显示 |

### 调试指令

TCDEX 提供以下调试指令：

```text
/tcdex debug element on|off         元素伤害/护盾系统调试输出
/tcdex debug reaction on|off        元素反应触发调试输出
/tcdex getlight                     查询玩家与准星目标光等
/tcdex setlight <value>             设置主手匠魂装备光等
/tcdex setlooklight <value>         设置准星目标怪物光等
/tcdex setelement <element>         设置主手元素充能武器元素
/tcdex element                      查看准星目标护盾与元素状态
```

开启 `reaction` 调试后，每次触发反应会在相关玩家聊天中显示类似：

```text
[TCDEX反应] solar+tide -> DAMAGE | 目标: Zombie
[TCDEX反应] moon+void+sinkstar -> SHIELD | 目标: Ender Dragon
```

---

## 开发者 API

TCDEX 提供面向附属 Mod 的公开 API，核心 API 不依赖 Tinkers Construct。

### 作为依赖引入

```groovy
repositories {
    flatDir { dir 'libs' }
}

dependencies {
    implementation fg.deobf("org.tp:tcdex:${tcdex_version}")
}
```

```toml
[[dependencies.yourmodid]]
modId = "tcdex"
mandatory = false
versionRange = "[1.0.0,)"
ordering = "NONE"
side = "BOTH"
```

如果使用 Tinkers 词条/Hook，再额外声明：

```toml
[[dependencies.yourmodid]]
modId = "tconstruct"
mandatory = false
versionRange = "[3.11.2.166,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.yourmodid]]
modId = "mantle"
mandatory = false
versionRange = "[1.11.104,)"
ordering = "NONE"
side = "BOTH"
```

### 元素 API

入口：`org.tp.tcdex.api.TcdexElementAPI`

常用功能：

```java
// 注册自定义护盾提供器
TcdexElementAPI.registerShieldProvider(entity -> ElementType.ARC);

// 护盾黑名单
TcdexElementAPI.addShieldBlacklist("minecraft:slime");

// 权重
TcdexElementAPI.setShieldWeight(ElementType.SOLAR, 3);
TcdexElementAPI.setElementWeight(ElementType.VOID, 2);

// 读取实体元素数据
IElementalEntity data = TcdexElementAPI.getEntityElementData(entity);
data.addElementState(ElementType.SOLAR, 25, 100);

// 查询/注册抗性
TcdexElementAPI.getResistance(entity, ElementType.VOID);
TcdexElementAPI.registerResistance("iceandfire:fire_dragon", ElementType.SOLAR, 0.3f);

// 元素伤害
DamageSource solar = ModDamageSources.element(attacker, ElementType.SOLAR);
```

### 元素反应 API

入口：`org.tp.tcdex.api.TcdexReactionAPI`

```java
// 注册自定义反应（自动双向）
TcdexReactionAPI.registerReaction(
        ElementReaction.builder(ElementType.SOLAR, ElementType.ARC, ReactionType.DAMAGE)
                .damage(10f)
                .radius(2f)
                .cooldown(40)
                .priority(50)
                .build());

// 查询 / 注销
ElementReaction reaction = TcdexReactionAPI.findReaction(ElementType.SOLAR, ElementType.ARC);
TcdexReactionAPI.unregisterReaction(ElementType.SOLAR, ElementType.ARC);

// 手动触发
TcdexReactionAPI.triggerReaction(target, ElementType.SOLAR, ElementType.ARC, player);

// 附着量
TcdexReactionAPI.addAura(target, ElementType.SOLAR, 1.0f, 100);
float aura = TcdexReactionAPI.getAura(target, ElementType.SOLAR);
TcdexReactionAPI.consumeAura(target, ElementType.SOLAR, 1.0f);
```

### 光等 API

入口：`org.tp.tcdex.api.TcdexAPI`

```java
int light = TcdexAPI.getItemLightLevel(stack);
TcdexAPI.setItemLightLevel(stack, 60);

int entityLight = TcdexAPI.getEntityLightLevel(entity);
TcdexAPI.setEntityLightLevel(entity, 40);

int attack = TcdexAPI.getPlayerAttackLightLevel(player);
```

附属 Mod 也可以注册自定义光等提供器：

```java
TcdexAPI.registerItemLightLevelProvider(new IItemLightLevelProvider() {
    @Override public boolean canProvide(ItemStack stack) { return true; }
    @Override public int getLightLevel(ItemStack stack) { return 30; }
    @Override public void setLightLevel(ItemStack stack, int value) { }
});
```

### 实体元素状态

所有 `LivingEntity` 都通过 Mixin 注入 `IElementalEntity`：

```java
IElementalEntity data = IElementalEntity.of(entity);

float stacks = data.getElementStacks(ElementType.SOLAR);
int duration = data.getElementDuration(ElementType.SOLAR);
data.addElementState(ElementType.SOLAR, 25, 100);
data.clearElementState(ElementType.SOLAR);

ElementType shield = data.getShieldElement();
float shieldAmount = data.getShieldAmount();
float overflow = data.consumeShield(damage);
```

### 元素反应模块扩展

`ElementReactionModule` 提供完整扩展点：

```java
public class MyReactionModule implements ElementReactionModule {
    @Override
    public ElementReaction getReaction() { return myReaction; }

    @Override
    public boolean canTrigger(ReactionContext context) { return true; }

    @Override
    public void onTrigger(ReactionContext context) {
        ElementReaction effective = context.getReaction();
        // 使用 effective 获取调整后的数值
    }

    @Override
    public float modifyDamage(ReactionContext context, float damage) { return damage * 1.2f; }
    @Override
    public int modifyDuration(ReactionContext context, int duration) { return duration; }
    @Override
    public float modifyRadius(ReactionContext context, float radius) { return radius; }
    @Override
    public float modifyIntensity(ReactionContext context, float intensity) { return intensity; }
    @Override
    public int modifyCooldown(ReactionContext context, int cooldown) { return cooldown; }
    @Override
    public float modifyAuraCost(ReactionContext context, float auraCost) { return auraCost; }
}
```

注册自定义模块：

```java
TcdexReactionAPI.registerReactionModule(new MyReactionModule());
```

---

### Tinkers Construct 词条 / Hook

Tinkers 联动位于 `org.tp.tcdex.integration.tinkers`。

#### TcdexBaseModifier

`TcdexBaseModifier` 一次性实现匠魂 3.11 大量原生 Hook 以及 TCDEX 自定义 Hook。附属 Mod 可以继承它并直接覆写 `modifierXxx()` 方法：

```java
public class MyModifier extends TcdexBaseModifier {

    @Override
    protected float modifierMeleeDamage(IToolStackView tool, ModifierEntry modifier,
                                        ToolAttackContext context, float baseDamage, float damage) {
        return damage * 1.5f;
    }

    @Override
    protected void modifierAfterMeleeHit(IToolStackView tool, ModifierEntry modifier,
                                         ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            IElementalEntity.of(target).addElementState(ElementType.SOLAR, 25, 100);
        }
    }
}
```

#### TCDEX 自定义 Hook

| Hook | 用途 |
|---|---|
| `KILLING_HOOK` | 工具击杀回调 |
| `ELEMENTAL_ATTACK` | 调整元素伤害与破盾效率 |
| `SHIELD_BREAK` | 怪物护盾破碎回调/爆炸 |
| `PLAYER_SHIELD` | 调整玩家护盾吸收与回复 |
| `PLAYER_SHIELD_BREAK` | 玩家护盾破碎溢出 |
| `ELEMENTAL_STATE_APPLY` | 调整元素状态层数/时长 |
| `ELEMENTAL_KEYWORD` | 调整关键词伤害/倍率/半径 |
| `KINETIC_ATTACK` | 动能伤害与动能破盾 |
| `REACTION` | 调整元素反应全部参数并回调 |

#### 自定义 ModuleHook

附属 Mod 可注册完全自定义的匠魂 Hook：

```java
public interface MyHook {
    default void onMyEvent(IToolStackView tool, ModifierEntry modifier, LivingEntity holder) {}
    record AllMerger(Collection<MyHook> modules) implements MyHook {
        @Override public void onMyEvent(IToolStackView tool, ModifierEntry modifier, LivingEntity holder) {
            modules.forEach(m -> m.onMyEvent(tool, modifier, holder));
        }
    }
}

public final class MyHooks {
    public static final ModuleHook<MyHook> MY_HOOK = ModifierHooks.register(
            new ResourceLocation("yourmodid", "my_hook"),
            MyHook.class, MyHook.AllMerger::new, new MyHook() {});
}
```

#### 词条互斥与依赖

- `ModifierExclusivity`：注册词条互斥。
- `ModifierHelper`：判断前置词条/元素充能。

---

## 配置总览

主要配置文件：`config/tcdex-common.toml`

| 配置项 | 说明 |
|---|---|
| `elementReactionsEnabled` | 元素反应总开关 |
| `auraDecayPerTick` | 元素附着自然衰减速度 |
| `shieldWeight*` | 各元素护盾生成权重 |
| `elementWeight*` | 元素充能随机权重 |
| `monsterElementalAttacks` | 怪物元素攻击开关 |
| `monsterElementalAttackChance` | 怪物元素攻击命中概率 |
| `monsterElementResistances` | 元素抗性/弱点表 |
| `shieldElementEfficiencies` | 护盾破盾效率表 |
| `prismShield*` | 棱镜盾参数 |
| `playerShield*` | 玩家护盾参数 |
| `playerShieldHud` / `monsterShieldHud` / `monsterAuraHud` / `elementEnergyHud` | HUD 开关 |
| `worldBaseLight` / `netherLightOffset` / `endLightOffset` | 世界光等参数 |
| `dimensionLightOffsets` / `biomeBaseLights` / `distanceGradient*` | 维度/群系/距离光等 |

---

## 开发与构建

### 构建

```bash
./gradlew build
./gradlew compileJava
```

### 数据生成

```bash
./gradlew runData
```

生成结果输出到 `src/generated/resources`。

### 纹理生成器

项目提供独立纹理生成工具：

```bash
java tools/texturegen/TextureGenerator.java --fluid molten_prism A78BFA
java tools/texturegen/TextureGenerator.java --item prism_ingot A78BFA ingot
```

---

## 常见问题

**Q：TCDEX 未安装时我的 Mod 会崩溃吗？**  
声明 `mandatory = false` 依赖并在调用前判断 `ModList.get().isLoaded("tcdex")` 即可。

**Q：Tinkers 未安装时 TCDEX 会崩溃吗？**  
不会。Tinkers Add 包不会初始化，核心元素反应、光等、护盾等功能不受影响。

**Q：元素状态会存档吗？**  
不会。元素状态是运行时战斗数据，随实体消失；护盾在生成时重新分配。

**Q：我能给玩家施加元素状态吗？**  
可以。玩家也是 `LivingEntity`，`IElementalEntity.of(player)` 可用。

**Q：元素反应数值能自己改吗？**  
可以。通过 `TcdexReactionAPI.registerReaction` 注册或覆盖默认反应，并设置 `priority` / `damage` / `duration` / `radius` / `intensity` / `cooldown`。

---

## 后续自创机制方向

当前已有不少原创基础，例如失衡/破绽、元素残响、元素适应。后续可以优先开发：

1. **原命星盘 / 元素回路**：玩家通过元素反应点亮星盘，激活专属共鸣。
2. **元素崩解**：目标同时有 3 种以上元素附着时触发多元素混合爆发。
3. **命定词缀**：怪物根据元素命定改变死亡/击杀策略。
4. **元素方碑 / 世界试炼**：把元素反应扩展到探索和解谜。
5. **处决/终结技**：基于失衡/破绽的系统化玩家动作。
6. **圣遗物套装效果**：增强 Build 深度。

---

## 许可证

MIT
