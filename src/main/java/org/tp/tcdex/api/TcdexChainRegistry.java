package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.chain.ElementActionType;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用元素链注册中心。
 *
 * <p>附属 Mod 可以注册：
 * <ul>
 *   <li>通用元素链反应：自定义条件 + 内置效果/回调；</li>
 *   <li>元素行为来源：让自定义物品/技能自动计入连携链。</li>
 * </ul>
 * </p>
 */
public final class TcdexChainRegistry {

    private static final List<GenericChainReaction> REACTIONS = new ArrayList<>();
    private static final List<ElementActionSource> SOURCES = new ArrayList<>();

    private TcdexChainRegistry() {
    }

    /** 注册通用元素链反应 */
    public static synchronized void registerGenericReaction(GenericChainReaction reaction) {
        if (reaction != null) {
            REACTIONS.removeIf(r -> r.getId().equals(reaction.getId()));
            REACTIONS.add(reaction);
        }
    }

    /** 注册自定义元素行为来源 */
    public static synchronized void registerElementActionSource(ElementActionSource source) {
        if (source != null) {
            SOURCES.add(source);
        }
    }

    /** 获取全部通用元素链反应（只读） */
    public static synchronized List<GenericChainReaction> getGenericReactions() {
        return Collections.unmodifiableList(new ArrayList<>(REACTIONS));
    }

    /** 获取全部元素行为来源（只读） */
    public static synchronized List<ElementActionSource> getElementActionSources() {
        return Collections.unmodifiableList(new ArrayList<>(SOURCES));
    }

    /** 尝试从已注册来源解析本次元素行为类型；没有匹配返回 null */
    @Nullable
    public static synchronized ElementActionType resolveElementAction(Player player, ItemStack stack,
                                                                     @Nullable LivingEntity target,
                                                                     ElementType element) {
        for (ElementActionSource source : SOURCES) {
            if (source.matches(player, stack, target, element)) {
                return source.getActionType();
            }
        }
        return null;
    }
}
