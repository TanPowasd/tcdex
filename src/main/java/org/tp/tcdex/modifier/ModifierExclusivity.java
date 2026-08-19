package org.tp.tcdex.modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.tp.tcdex.Tcdex;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 词条互斥注册中心（匠魂 3.10 官方无 isExclusive API，自实现）。
 *
 * <p>用法：</p>
 * <ul>
 *   <li>在 {@link #registerAll()} 里集中注册互斥关系（一对 / 一组），注册自动双向</li>
 *   <li>互斥词条在 {@code modifierValidate} 中调用 {@link #validate(IToolStackView, ModifierEntry)}，
 *       工具上已有互斥词条时返回提示（词条工作台会拒绝添加）</li>
 * </ul>
 *
 * <p>对附属 mod 开放：可直接调用 {@link #registerExclusive} / {@link #registerExclusiveGroup}
 * 声明自己的词条与 TCDEX 词条互斥，无需改动 TCDEX 源码。</p>
 */
public final class ModifierExclusivity {

    /** 互斥表：词条 id → 与其互斥的词条 id 集合（注册时自动双向登记） */
    private static final Map<ModifierId, Set<ModifierId>> EXCLUSIVE = new HashMap<>();

    private ModifierExclusivity() {
    }

    /**
     * 集中注册全部内置互斥关系（mod 构造时调用一次）。
     *
     * <p>新增互斥在此追加一行即可，如：{@code registerExclusive(new ModifierId(MODID, "a"), new ModifierId(MODID, "b"));}</p>
     */
    public static void registerAll() {
        // 元素充能 ↔ 棱镜共鸣：棱镜伤害为专属来源（棱镜共鸣），与元素充能的随机元素不可叠加
        registerExclusive(new ModifierId(Tcdex.MODID, "elemental"), new ModifierId(Tcdex.MODID, "prism_resonance"));
        // 动能词条 ↔ 元素体系：动能震颤/动能虹吸为动能武器（无元素词条）专属，与元素充能/棱镜共鸣互斥
        registerExclusive(id("kinetic_tremors"), id("elemental"));
        registerExclusive(id("kinetic_tremors"), id("prism_resonance"));
        registerExclusive(id("kinetic_siphon"), id("elemental"));
        registerExclusive(id("kinetic_siphon"), id("prism_resonance"));
        // 五项之力 ↔ 元素体系：每次攻击随机元素的伤害类型决定权唯一，与元素充能/棱镜共鸣互斥
        // （与动能词条不互斥——可混搭"随机元素攻击 + 动能特性"的 hybrid build）
        registerExclusive(id("five_forces"), id("elemental"));
        registerExclusive(id("five_forces"), id("prism_resonance"));
    }

    /** 注册一对互斥词条（自动双向登记；重复注册幂等） */
    public static void registerExclusive(ModifierId a, ModifierId b) {
        if (a == null || b == null || a.equals(b)) {
            return;
        }
        EXCLUSIVE.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        EXCLUSIVE.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    /** 注册一组互斥词条（组内两两互斥，自动双向；长度为 0/1 时无效果） */
    public static void registerExclusiveGroup(ModifierId... ids) {
        if (ids == null || ids.length < 2) {
            return;
        }
        for (int i = 0; i < ids.length; i++) {
            for (int j = i + 1; j < ids.length; j++) {
                registerExclusive(ids[i], ids[j]);
            }
        }
    }

    /** 返回与指定词条互斥的全部词条 id（未注册返回空集合） */
    public static Set<ModifierId> getExclusiveWith(ModifierId self) {
        Set<ModifierId> set = EXCLUSIVE.get(self);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    /** 工具上是否已存在与指定词条互斥的词条 */
    public static boolean hasConflict(ModifierId self, IToolStackView tool) {
        return findConflict(self, tool) != null;
    }

    /**
     * 查找工具上与指定词条互斥的已有词条。
     *
     * @return 第一个冲突词条 id；无冲突返回 null
     */
    @Nullable
    public static ModifierId findConflict(ModifierId self, IToolStackView tool) {
        Set<ModifierId> exclusive = EXCLUSIVE.get(self);
        if (exclusive == null || exclusive.isEmpty() || tool == null) {
            return null;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (exclusive.contains(entry.getId())) {
                return entry.getId();
            }
        }
        return null;
    }

    /**
     * 词条校验钩子：供词条在 {@code modifierValidate} 中调用。
     *
     * <p>工具上已存在与自身互斥的词条时，返回带对方词条名的互斥提示
     * （匠魂词条工作台/配方添加时经 ToolStack.tryValidate 拒绝添加）；无冲突返回 null。</p>
     */
    @Nullable
    public static Component validate(IToolStackView tool, ModifierEntry modifier) {
        ModifierId conflict = findConflict(modifier.getId(), tool);
        if (conflict == null) {
            return null;
        }
        // 对方词条名：按词条翻译 key 解析（modifier.<namespace>.<path>），与匠魂 Modifier.getDisplayName 一致
        Component name = Component.translatable(Util.makeTranslationKey("modifier", conflict));
        return Component.translatable("modifier.tcdex.exclusive", name);
    }

    /** 构造 TCDEX 命名空间的词条 id（便捷方法） */
    public static ModifierId id(String path) {
        return new ModifierId(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, path));
    }
}
