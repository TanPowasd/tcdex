# TCDEX

**匠魂 3（TConstruct）命运2 风格附属模组** — Forge 1.20.1

本模组为匠魂工具/盔甲加入命运2 风格的核心系统：

- **元素充能**：武器打上「元素充能」词条后随机获得一种元素（烈日/电弧/虚空/冰影/缚丝），永久固化，攻击伤害从动能转化为元素伤害
- **元素关键词**：灼烧 DoT→引爆、减速→冻结→粉碎、挥发爆炸、连锁闪电、削弱——全部由怪物身上的元素状态驱动（Mixin 注入）
- **元素护盾**：敌对生物生成时随机携带元素护盾（匹配元素 ×2 破盾效率，打穿触发破盾爆炸）
- **光等系统**：命运2 风格光等（世界光等场 + 时间压力蔓延 + 伤害修正）
- **玩家护盾**：脱战自动回复的护盾层 + 命运2 风格 HUD（蓝色护盾条、buff 列表）

本文档面向**其他附属模组开发者**，说明如何通过 TCDEX 的 API 与 Hook 体系扩展本模组。

---

## 目录

- [1. 作为依赖引入](#1-作为依赖引入)
- [2. 元素系统 API](#2-元素系统-api)
- [3. 光等系统 API](#3-光等系统-api)
- [4. 词条 Hook 开发](#4-词条-hook-开发)
  - [4.1 全能词条基类 TcdexBaseModifier](#41-全能词条基类-tcdexbasemodifier)
  - [4.2 击杀 Hook（KILLING_HOOK）](#42-击杀-hookkilling_hook)
  - [4.3 元素攻击 Hook（ELEMENTAL_ATTACK）](#43-元素攻击-hookelemental_attack)
  - [4.4 注册自定义 ModuleHook（扩展模式）](#44-注册自定义-modulehook扩展模式)
- [5. 实体元素状态接口 IElementalEntity](#5-实体元素状态接口-ielementalentity)
- [6. 元素类型与关键词总览](#6-元素类型与关键词总览)
- [7. 配置总览](#7-配置总览)
- [8. 常见问题](#8-常见问题)

---

## 1. 作为依赖引入

**build.gradle**（在匠魂依赖之后）：

```groovy
repositories {
    // 本地 libs 目录（将 TCDEX jar 放入你的 mod 的 libs/ 文件夹）
    flatDir { dir 'libs' }
}

dependencies {
    implementation fg.deobf("org.tp:tcdex:${tcdex_version}")  // 或具体版本，如 1.0.0
    // TCDEX 依赖（传递依赖需自行声明）：
    implementation fg.deobf("curse.maven:tinkers-construct-74072:7449219")
    implementation fg.deobf("curse.maven:mantle-74924:7563777")
}
```

**mods.toml**（软依赖，TCDEX 未安装时你的 mod 应降级功能）：

```toml
[[dependencies.yourmodid]]
modId = "tcdex"
mandatory = false
versionRange = "[1.0.0,)"
ordering = "NONE"
side = "BOTH"
```

代码中判断 TCDEX 是否加载：

```java
public static final boolean TCDEX_LOADED = ModList.get().isLoaded("tcdex");
```

---

## 2. 元素系统 API

入口：`org.tp.tcdex.api.TcdexElementAPI`（静态方法，TCDEX 加载后可直接调用）。

### 2.1 自定义护盾提供器

想让特定生物（含其他 mod 生物）固定获得/获得指定元素的护盾：

```java
public class MyCompat {
    public static void init() {
        TcdexElementAPI.registerShieldProvider(entity -> {
            // 返回 null = 不接管（交给黑名单/加权随机）
            if (entity.getType() == Registries.ENTITY_TYPES.get(new ResourceLocation("twilightforest:naga"))) {
                return ElementType.ARC;  // 娜迦固定电弧盾
            }
            return null;
        });
    }
}
```

分配优先级：**黑名单（绝对无盾）→ 提供器 → 加权随机**（无静态表指定）。

### 2.2 护盾黑名单（不让某些生物带盾）

```java
TcdexElementAPI.addShieldBlacklist("minecraft:slime");   // entity id，兼容任意 mod 生物
TcdexElementAPI.removeShieldBlacklist("minecraft:slime");
boolean noShield = TcdexElementAPI.isShieldBlacklisted(entity);   // 黑名单生物同时没有元素攻击
```

### 2.3 生成比例（运行时调整）

```java
// 护盾元素权重（0 = 不生成该元素盾）
TcdexElementAPI.setShieldWeight(ElementType.SOLAR, 3);
Map<ElementType, Integer> shieldWeights = TcdexElementAPI.getShieldWeights();

// 元素充能随机元素权重（0 = 词条不会随机到该元素）
TcdexElementAPI.setElementWeight(ElementType.VOID, 2);
Map<ElementType, Integer> elementWeights = TcdexElementAPI.getElementWeights();
```

### 2.4 读取/写入实体元素数据

```java
IElementalEntity data = TcdexElementAPI.getEntityElementData(entity);

// 读取
float stacks = data.getElementStacks(ElementType.SOLAR);   // 灼烧层数
int duration = data.getElementDuration(ElementType.VOID);  // 剩余 tick
ElementType shield = data.getShieldElement();              // 护盾元素（null=无）
float shieldAmount = data.getShieldAmount();               // 剩余护盾值

// 写入（例如你的 mod 给怪物施加灼烧）
data.addElementState(ElementType.SOLAR, 25, 100);          // +25 层，100 tick

// 直接扣护盾（返回溢出伤害）
float overflow = data.consumeShield(damage);
data.destroyShield();
```

### 2.5 查询元素抗性

```java
float resistance = TcdexElementAPI.getResistance(entity, ElementType.VOID);
// 1.0 正常 / >1 弱点（受更多伤害） / <1 抗性
```

### 2.6 使用 TCDEX 元素伤害类型

```java
// 动能 / 元素 / 纯粹 / 灼烧 DoT 伤害源（均为服务端权威类型）
DamageSource kinetic = ModDamageSources.kinetic(attacker);
DamageSource solar = ModDamageSources.element(attacker, ElementType.SOLAR);
DamageSource pure = ModDamageSources.pure(attacker);
DamageSource scorch = ModDamageSources.scorch(entity);   // 无视无敌帧的 DoT

boolean isElemental = ModDamageSources.isElementDamage(source);
```

> ⚠️ 元素伤害类型与护盾/转化系统联动：对带护盾目标使用元素伤害源，会按"匹配/不匹配"效率结算破盾。

### 2.7 元素怪物攻击（命中玩家施加元素状态）

**元素的攻击与护盾同源分配**：任何带元素护盾的怪物（护盾分配链：黑名单 → 提供器 → 加权随机，
无静态表指定），其攻击命中玩家时也会施加与护盾**相同元素**的状态（冰霜箭减速/冻结、
电弧光束、虚空火球……）。攻击元素在护盾分配时**固化**：无盾生物没有元素攻击，
**护盾被打破后元素攻击保留**（只失去护盾，不失去元素能力）。

命中玩家时按 `monsterElementalAttackChance`（默认 1.0，配置可调）施加对应元素状态
（层数按怪物系数缩放，标记型元素保底 1 层；时长同玩家武器），元素状态会同步到玩家 Buff HUD，
Shatter / Volatile / Weaken / Jolt 等关键词对玩家同样生效。总开关 `monsterElementalAttacks`（默认 true）。

```java
// 查询怪物的元素攻击类型（= 其护盾元素，null = 无）
ElementType attack = TcdexElementAPI.getMonsterAttackElement(entity);
```

### 2.8 棱镜盾（凋零 / 末影龙 100%）

凋零与末影龙生成时**必定**携带棱镜护盾（内置护盾提供器，护盾量 = 最大生命 × 50%），棱镜盾效果：

| 伤害类型 | 护盾磨损（破盾速度） | 说明 |
|---|---|---|
| 棱镜伤害 | ×2 效率（快） | 匹配元素 |
| 其他元素伤害 | ×0.5 效率（慢） | 对应"50% 减免"：盾承伤翻倍 |
| 动能伤害 | ×0.1 效率（最慢） | 对应"90% 减免"：盾承伤 ×10 |
| 脱战回复 | 10 秒未受伤后，每 5 tick 回复 10% 最大护盾值 | |

棱镜盾是**吸收型**护盾：破盾前所有伤害被盾**完全吸收**（按上表效率磨损护盾），**打不到血量**——
例如 150 点棱镜盾，20 点虚空伤害 ×0.5 = 磨损 10/击，正好 **15 击打破**。

打穿结算区分来源：
- **棱镜伤害打穿** → 护盾**永久失效**（清除护盾元素，不再回复）
- **非棱镜伤害打穿** → 护盾元素保留，**脱战 10 秒后仍会回复**（每 5 tick 10%，重新长满）

破盾后（盾值 0 期间）伤害正常打到血量；Boss 的**元素攻击保留**（攻击元素在分配时固化，不随护盾消失）。

---

## 3. 光等系统 API

入口：`org.tp.tcdex.api.TcdexAPI`。

```java
// 物品光等（匠魂物品 + 已注册自定义物品）
int light = TcdexAPI.getItemLightLevel(stack);
TcdexAPI.setItemLightLevel(stack, 60);

// 实体（怪物）光等
int entityLight = TcdexAPI.getEntityLightLevel(entity);
TcdexAPI.setEntityLightLevel(entity, 40);

// 玩家光等（护甲平均 / 武器 / 攻击 = (4盔甲+武器)/5）
int armor = TcdexAPI.getPlayerArmorLightLevel(player);
int weapon = TcdexAPI.getPlayerWeaponLightLevel(player);
int attack = TcdexAPI.getPlayerAttackLightLevel(player);
```

### 3.1 注册自定义光等提供器

让非匠魂物品/实体参与光等计算：

```java
// 物品：返回物品光等（返回 false 表示不接管）
TcdexAPI.registerItemLightLevelProvider(new IItemLightLevelProvider() {
    @Override public boolean canProvide(ItemStack stack) { return stack.is(MyItems.MY_WEAPON.get()); }
    @Override public int getLightLevel(ItemStack stack) { return 30; }
    @Override public void setLightLevel(ItemStack stack, int value) { /* 写入你的 NBT */ }
});

// 实体
TcdexAPI.registerEntityLightLevelProvider(new IEntityLightLevelProvider() { ... });

// 伤害修正：自定义光等差 → 伤害倍率规则
TcdexAPI.registerDamageModifierProvider(new IDamageModifierProvider() {
    @Override public float modifyDealtDamage(float multiplier, int attackerLight, int defenderLight) { return multiplier * 1.1f; }
    @Override public float modifyTakenDamage(float multiplier, int attackerLight, int defenderLight) { return multiplier; }
});
```

---

## 4. 词条 Hook 开发

### 4.1 全能词条基类 TcdexBaseModifier

`org.tp.tcdex.modifier.base.TcdexBaseModifier` —— 对标 sakuratinker `BaseModifier` 的**全能基类**：
- 一次性 implements **匠魂 3.10 全部 61 个原生 hook** + TCDEX 自定义 `KILLING_HOOK`
- `registerHooks` 已全部注册，子类**无需写任何注册代码**
- 所有 hook 方法委托为可覆写的 `modifierXxx()`（空实现/安全默认值）

```java
public class MyModifier extends TcdexBaseModifier {

    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId("yourmodid", "my_modifier"), new MyModifier());
    }

    // 近战伤害 ×1.5
    @Override
    protected float modifierMeleeDamage(IToolStackView tool, ModifierEntry modifier,
                                        ToolAttackContext context, float baseDamage, float damage) {
        return damage * 1.5f;
    }

    // 命中附加状态
    @Override
    protected void modifierAfterMeleeHit(IToolStackView tool, ModifierEntry modifier,
                                         ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            IElementalEntity.of(target).addElementState(ElementType.SOLAR, 25, 100);
        }
    }

    // 无等级词条（可选）：显示名不附带等级
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }
}
```

注册（对照 TCDEX 主类模式）：

```java
// 你的主类构造器
modEventBus.addListener(MyModifier::registerModifier);
```

可用 `modifierXxx` 速查（按分组）：

| 分组 | 可覆写方法 |
|---|---|
| armor | `modifierOnWalk` / `modifierIsDamageBlocked` / `modifierElytraFlightTick` / `modifierModifyDamageTaken` / `modifierOnAttacked` / `modifierGetProtectionModifier` / `modifierOnEquip/Unequip/EquipmentChange` |
| behavior | `modifierAddAttributes` / `modifierUpdateEnchantmentLevel(s)` / `modifierIsRepairMaterial` / `modifierGetRepairAmount` / `modifierProcessLoot` / `modifierGetRepairFactor` / `modifierCanPerformAction` / `modifierOnDamageTool` |
| build | `modifierModifyStat` / `modifierModifyCraftCount` / `modifierOnRemoved` / `modifierAddTraits` / `modifierAddRawData` / `modifierAddToolStats` / `modifierValidate` / `modifierAddVolatileData` |
| combat | `modifierMeleeDamage` / `modifierBeforeMeleeHit` / `modifierAfterMeleeHit` / `modifierFailedMeleeHit` / `modifierOnMonsterMeleeHit` / `modifierOnDamageDealt` / `modifierUpdateLooting` / `modifierUpdateArmorLooting` |
| display | `modifierGetDisplayName` / `modifierShowDurabilityBar` / `modifierGetDurabilityWidth/RGB` / `modifierDisplayModifiers` / `modifierRequirementsError` / `modifierAddTooltip` |
| interaction | `modifierOnToolUse` / `modifierOnUsingTick` / `modifierOnStoppedUsing` / `modifierOnFinishUsing` / `modifierGetUseDuration` / `modifierGetUseAction` / `modifierOnInventoryTick` / `modifierStartInteract` / `modifierBefore/AfterBlockUse` / `modifierBefore/AfterEntityUse` / `modifierShouldHighlight` / `modifierOverrideStackedOnOther` / `modifierOverrideOtherStackedOnMe` |
| mining | `modifierAfterBlockBreak` / `modifierStartHarvest` / `modifierFinishHarvest` / `modifierOnBreakSpeed` / `modifierModifyBreakSpeed` / `modifierUpdateHarvestEnchantments` / `modifierRemoveBlock` |
| ranged | `modifierFindAmmo` / `modifierShrinkAmmo` / `modifierOnLauncherHitEntity/Block` / `modifierOnProjectileFuseFinish` / `modifierOnProjectileHitEntity/Block/HitsBlock` / `modifierOnProjectileLaunch` / `modifierOnProjectileShoot` / `modifierScheduleProjectileTask` / `modifierOnScheduledProjectileTask` |
| special | `modifierAfterTransformBlock` / `modifierGetAmount/Capacity` / `modifierSetAmount` / `modifierAfterHarvest` / `modifierAfterShearEntity` / `modifierModifySlingAngle/Force` / `modifierAfterSlingLaunch` |
| TCDEX | `modifierOnKillLivingTarget`（击杀） |

### 4.2 击杀 Hook（KILLING_HOOK）

匠魂 3.10 **没有原生击杀 hook**，TCDEX 提供了自定义的（`TcdexHooks.KILLING_HOOK`，由 `TcdexHookEvents` 在 `LivingDeathEvent`(LOWEST) 派发，遍历攻击者双手工具）。

```java
public class ReaperModifier extends TcdexBaseModifier {

    // 基类 registerHooks 已注册 KILLING_HOOK，无需再 addHook

    @Override
    protected void modifierOnKillLivingTarget(IToolStackView tool, LivingDeathEvent event,
                                              LivingEntity attacker, LivingEntity target, int level) {
        // 击杀结算：例如回复耐久 / 触发联动
        if (event.getSource().getEntity() == attacker) {
            // 只处理本工具造成的击杀
        }
    }
}
```

> 不使用基类时，也可直接 `implements KillingHook`（接口默认方法 + `AllMerger` 合并器），并在 `registerHooks` 中 `addHook(this, TcdexHooks.KILLING_HOOK)`。

### 4.3 元素攻击 Hook（ELEMENTAL_ATTACK）

`TcdexHooks.ELEMENTAL_ATTACK`（`ElementalAttackModifierHook`）——工具上任意词条可调整**元素伤害**与**护盾破盾效率**，由 `ElementalDamageEvents` 在伤害转化/护盾结算时链式派发（AllMerger）：

```java
public class ElementalMasterModifier extends TcdexBaseModifier implements ElementalAttackModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, TcdexHooks.ELEMENTAL_ATTACK);   // 基类不预注册本 hook，需手动挂
    }

    // 元素伤害 +20%
    @Override
    public float modifyElementalDamage(IToolStackView tool, ModifierEntry modifier,
                                       ElementType element, float amount) {
        return amount * 1.2f;
    }

    // 破盾效率 ×1.5（匹配 2.0 → 3.0；不匹配 0.5 → 0.75）
    @Override
    public float modifyShieldEfficiency(IToolStackView tool, ModifierEntry modifier,
                                        ElementType shieldElement, float efficiency) {
        return efficiency * 1.5f;
    }
}
```

> 该接口的两个方法均为 `default`（返回原值），只覆写需要的即可。

### 4.4 注册自定义 ModuleHook（扩展模式）

完全自定义 hook 类型（如你的 mod 需要"工具格挡"、"工具治疗"等新触发点），照 TCDEX 模式：

```java
// 1. 接口 + 合并器
public interface MyHook {
    default void onMyEvent(IToolStackView tool, ModifierEntry modifier, LivingEntity holder) {
    }
    record AllMerger(java.util.Collection<MyHook> modules) implements MyHook {
        @Override public void onMyEvent(IToolStackView tool, ModifierEntry modifier, LivingEntity holder) {
            for (MyHook module : modules) module.onMyEvent(tool, modifier, holder);
        }
    }
}

// 2. 注册到 ModifierHooks（全局注册表）
public final class MyHooks {
    public static final ModuleHook<MyHook> MY_HOOK = ModifierHooks.register(
            new ResourceLocation("yourmodid", "my_hook"),
            MyHook.class, MyHook.AllMerger::new, new MyHook() {});
}

// 3. 词条声明 + 触发点（Forge 事件里手动派发）
@Mod.EventBusSubscriber(modid = "yourmodid", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MyEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSomeEvent(SomeForgeEvent event) {
        LivingEntity holder = ...;
        ToolStack tool = Modifier.getHeldTool(holder, InteractionHand.MAIN_HAND);
        if (tool == null) return;
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(MyHooks.MY_HOOK).onMyEvent(tool, entry, holder);
        }
    }
}
```

---

## 5. 实体元素状态接口 IElementalEntity

`org.tp.tcdex.modifier.elemental.IElementalEntity` —— 由 Mixin 注入**所有 LivingEntity**（含其他 mod 生物）。获取方式：

```java
IElementalEntity data = IElementalEntity.of(entity);   // 直接强转，无需判空
```

| 方法 | 说明 |
|---|---|
| `getElementStacks(type)` / `getElementDuration(type)` | 读取元素状态层数 / 剩余 tick |
| `addElementState(type, stacks, duration)` | 叠加状态（层数封顶 100，时长取较大值） |
| `clearElementState(type)` | 清除某元素状态 |
| `getAllElementStates()` | 全部状态（`Map<ElementType, ElementStatus>`） |
| `getShieldElement()` / `getShieldAmount()` | 护盾元素 / 剩余护盾值（懒加载初始化） |
| `consumeShield(damage)` | 扣盾，返回溢出伤害 |
| `destroyShield()` / `setShield(element, amount)` | 破盾 / 直接设置护盾 |

状态在实体 **tick 自动结算**（服务端）：灼烧 DoT（10 tick/跳）、冰影减速/冻结、到期清除。

---

## 6. 元素类型与关键词总览

| 元素 | id | 状态机制 | 关键词效果 |
|---|---|---|---|
| 烈日 SOLAR | `solar` | 每击 +25 层 | 灼烧 DoT（10 tick/跳，无视无敌帧）；满 100 → Ignite 引爆（4 格 AOE） |
| 电弧 ARC | `arc` | 标记型（+1） | 受击 → Jolt 连锁闪电（2 格内 2 目标） |
| 虚空 VOID | `void` | 标记型（+1） | 受击 → Volatile 爆炸（10% 最大生命 AOE） |
| 冰影 STASIS | `stasis` | 每击 +50 层 | ≥50 减速 → 满 100 冻结；冻结中受击 Shatter +50% |
| 缚丝 STRAND | `strand` | 标记型（+1） | 带标记者造成伤害 -40%（Sever） |
| 棱镜 PRISM | `prism` | 标记型（+1） | 受击 Refract 折射：本击 25% 伤害溅射周围；棱镜攻击破任意元素盾（匹配效率 ×2，折射所有光）。**玩家无法通过元素充能词条获得（Boss 专属）** |

元素爆炸/连锁**均不伤害玩家**（命运2 语义）；`AllPermittedModifier` 超载除外（有意设计）。

---

## 7. 配置总览

`config/tcdex-common.toml`（节选，均为运行时重载）：

```toml
# 世界光等场
worldBaseLight = 20
netherLightOffset = 25
endLightOffset = 50
dimensionLightOffsets = []      # 维度偏移表，如 twilightforest:twilight_forest=40
dimensionOffsetOther = 30       # 其他 mod 维度默认偏移
distanceGradientStep = 3        # 每 1000 格 +3
distanceGradientCap = 45
daysPerTimeBonus = 5            # 时间压力：每 5 活跃天 +1
maxTimeBonus = 30
timeSpreadStart = 2000          # 出生点安全区
timeSpreadEnd = 10000

# 护盾与元素权重
shieldBlacklist = []            # 不带元素盾/元素攻击的生物（modid:entity）
shieldWeightSolar/Arc/Void/Stasis/Strand = 1
shieldWeightPrism = 0           # 棱镜暂不参与随机盾
elementWeightSolar/Arc/Void/Stasis/Strand = 1
elementWeightPrism = 0          # 棱镜不可通过元素充能获得（Boss 专属）

# 元素怪物攻击
monsterElementalAttacks = true  # 怪物元素攻击总开关
monsterElementalAttackChance = 1.0  # 每次命中施加元素状态的概率（0-1）

# 玩家护盾
playerShieldEnabled = true
playerShieldRatio = 1.0
playerShieldRegenDelay = 5
playerShieldRegenRate = 0.4
playerShieldHud = true
playerBuffHud = true
```

---

## 8. 常见问题

**Q：TCDEX 未安装时我的 mod 会崩溃吗？**
声明 `mandatory = false` 依赖 + `ModList.isLoaded("tcdex")` 保护即可。只引用 API 类但不在未加载时调用是安全的（类在 TCDEX jar 中，你的 jar 不打包它）。

**Q：元素状态会存档吗？**
不会——元素状态是运行时数据（战斗短时状态），随实体消失；护盾在实体生成时重新分配。

**Q：我可以给玩家施加元素状态吗？**
可以——`IElementalEntity.of(player)` 同样可用（玩家也是 LivingEntity），客户端 HUD 会自动显示（需 TCDEX 的 `PlayerStateSyncPacket` 同步，服务端写入即可）。

**Q：`TcdexBaseModifier` 与匠魂 `NoLevelsModifier` 区别？**
基类继承 `Modifier`（可升级）。需要无等级语义时覆写 `getDisplayName(int level)` 返回 `super.getDisplayName()`（参考 TCDEX 各词条）。

**Q：护盾/元素权重改了不生效？**
配置文件 `/reload` 或重启生效；**已固化的武器元素、已生成的怪物护盾不会重新随机**（固化/生成时确定是设计语义）。

---

## 许可证

MIT（见 LICENSE）
