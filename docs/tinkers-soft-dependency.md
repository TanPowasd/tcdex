# TCDEX 全联动 Add 包架构设计文档

## 1. 核心理念

TCDEX **核心不硬依赖任何外部 mod**。

所有外部联动内容都作为 **add 包（Integration Add-on）** 存在：
- 检测到对应 mod 加载 → 自动启动该联动
- 未检测到 → 完全跳过，不影响核心功能

## 2. 联动范围

| 外部 mod | add 包 | 说明 |
|---|---|---|
| Tinkers Construct | `integration.tinkers` | 词条、匠魂光等、元素武器、催化 |
| Ice and Fire | `integration.iceandfire` | 龙类元素护盾/攻击 |
| Iron's Spellbooks | `integration.irons_spellbooks` | 法术元素化 |
| JEI | `integration.jei` | 隐藏物品等 |
| Curios | `integration.curios` | 圣遗物槽位（当前已强制，后续可改软） |

## 3. 架构

```
src/main/java/org/tp/tcdex/
├── api/
│   └── ITcdexIntegration.java
├── integration/
│   ├── IntegrationManager.java
│   └── <modid>/
│       └── XxxIntegration.java
├── core/                 # 核心系统，不依赖外部 mod
└── ...
```

## 4. 统一接口

```java
public interface ITcdexIntegration {
    String getModId();
    void init();
}
```

## 5. 统一管理器

```java
public final class IntegrationManager {
    static {
        INTEGRATIONS.add(new TinkersIntegration());
        INTEGRATIONS.add(new IceAndFireIntegration());
        INTEGRATIONS.add(new IronSpellsIntegration());
        INTEGRATIONS.add(new JeiIntegration());
    }

    public static void init() {
        for (ITcdexIntegration integration : INTEGRATIONS) {
            if (ModList.get().isLoaded(integration.getModId())) {
                integration.init();
            }
        }
    }
}
```

## 6. 事件注册策略

- add 包内的事件类 **不自动注册**
- 统一在 `init()` 中手动注册：

```java
MinecraftForge.EVENT_BUS.register(XxxEvents.class);
```

- 避免未安装 mod 时加载外部类

## 7. 依赖声明

- `mods.toml` 中所有外部 mod 均为 `mandatory=false`
- 只保留 Forge / Minecraft 为强制依赖

## 8. 迁移步骤

### Phase 1：框架
- [x] 新增 `ITcdexIntegration`
- [x] 新增 `IntegrationManager`
- [x] 新增 Tinkers add 包占位
- [x] 主类调用 `IntegrationManager.init()`

### Phase 2：核心解耦
- 将现有 `TcdexCompat` 改为 add 包
- 将 Tinkers 相关代码收拢到 `integration.tinkers`
- 公共代码通过 API 访问外部功能

### Phase 3：各 add 包实现
- [x] Tinkers：词条 / 光等 / 催化
- [x] Ice and Fire：龙类联动
- [x] Iron's Spellbooks：法术联动
- [ ] JEI：隐藏物品

### Phase 4：测试
- 每种 mod 有无安装组合测试
