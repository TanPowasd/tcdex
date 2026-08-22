package org.tp.tcdex.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.chain.ElementActionType;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ElementReaction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Tinkers 桥接接口：Core 只依赖此接口，不直接引用 Tinkers 类。
 */
public interface ITinkersBridge {

    boolean isTinkersTool(ItemStack stack);

    boolean isUsableTinkersTool(ItemStack stack);

    boolean isInitializedTool(ItemStack stack);

    @Nullable
    ElementType getWeaponElement(ItemStack stack);

    /** 获取武器元素；若元素充能尚未固化则立即随机固化并写回物品 NBT（用于伤害转化路径） */
    @Nullable
    ElementType getOrInitializeWeaponElement(ItemStack stack);

    int getCatalystLevel(ItemStack stack);

    void addCatalystProgress(ItemStack stack, int amount);

    void setWeaponElement(ItemStack stack, ElementType element);

    float getToolLightLevel(ItemStack stack);

    int getToolBaseLight(ItemStack stack);

    int getToolInfusion(ItemStack stack);

    void setToolLightLevel(ItemStack stack, int value);

    void addToolInfusion(ItemStack stack, int amount);

    boolean hasToolLightOverride(ItemStack stack);

    void removeToolLightOverride(ItemStack stack);

    int getToolElementalMastery(ItemStack stack);

    boolean hasModifier(ItemStack stack, String modifierId);

    float modifyAbsorbed(Player player, float damageAmount, float absorbed);

    float modifyRegenRate(Player player, float rate);

    float modifyBreakOverflow(Player player, DamageSource source, float overflow);

    void onPlayerShieldBreak(Player player, DamageSource source, float overflow);

    String getApMode(ItemStack stack);

    float getApForbidden(ItemStack stack);

    int getApSin(ItemStack stack);

    int getApCombo(ItemStack stack);

    float modifyElementalDamage(ItemStack stack, ElementType element, float amount);

    float modifyShieldEfficiency(ItemStack stack, ElementType shieldElement, float efficiency);

    float modifyKineticDamage(ItemStack stack, LivingEntity target, float amount);

    float modifyKineticShieldEfficiency(ItemStack stack, ElementType shieldElement, float efficiency);

    float modifyBreakExplosion(ItemStack stack, LivingEntity target, ElementType shieldElement, float damage);

    void onShieldBreak(ItemStack stack, LivingEntity target, ElementType shieldElement, LivingEntity attacker);

    float modifyKeywordMultiplier(ItemStack stack, ElementType keyword, float multiplier);

    float modifyKeywordDamage(ItemStack stack, ElementType keyword, float damage);

    float modifyKeywordRadius(ItemStack stack, ElementType keyword, float radius);

    float modifyReactionAuraCost(ItemStack stack, ElementReaction reaction, float auraCost);

    float modifyReactionDuration(ItemStack stack, ElementReaction reaction, float duration);

    float modifyReactionRadius(ItemStack stack, ElementReaction reaction, float radius);

    float modifyReactionIntensity(ItemStack stack, ElementReaction reaction, float intensity);

    float modifyReactionDamage(ItemStack stack, ElementReaction reaction, float damage);

    int modifyReactionCooldown(ItemStack stack, ElementReaction reaction, int cooldown);

    void onReactionTriggered(ItemStack stack, LivingEntity target, ElementReaction reaction, LivingEntity source, float intensity);

    void applyFiveForcesHitState(ItemStack stack, Player player, LivingEntity target, ElementType element);

    // ===== 原命连携 Hook 桥接 =====

    float modifyChainContribution(ItemStack stack, ElementType element, ElementActionType actionType, float contribution);

    float modifyDetonateDamage(ItemStack stack, float damage);

    float modifyDetonateRadius(ItemStack stack, float radius);

    int modifyDetonateCooldown(ItemStack stack, int cooldown);

    int modifyDetonateBuffDuration(ItemStack stack, int duration);

    float modifyFinisherDamage(ItemStack stack, float damage);

    float modifyFinisherRadius(ItemStack stack, float radius);

    void onChainDetonate(ItemStack stack, LivingEntity player, List<ElementType> elements, @Nullable LivingEntity center);

    void onChainFinisher(ItemStack stack, LivingEntity player, LivingEntity target, List<ElementType> elements);
}
