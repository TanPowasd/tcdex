package org.tp.tcdex.integration.tinkers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.api.ITinkersBridge;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.integration.tinkers.catalyst.CatalystManager;
import org.tp.tcdex.integration.tinkers.modifier.elemental.ElementalModifier;
import org.tp.tcdex.integration.tinkers.modifier.elemental.FiveForcesModifier;
import org.tp.tcdex.integration.tinkers.modifier.elemental.PrismResonanceModifier;
import org.tp.tcdex.integration.tinkers.modifier.hook.ElementalAttackModifierHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.ElementalKeywordHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.KineticAttackModifierHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.PlayerShieldBreakHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.PlayerShieldHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.ReactionModifierHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.ShieldBreakHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.TcdexHooks;
import org.tp.tcdex.integration.tinkers.modifier.special.ElementalMasteryModifier;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

/**
 * Tinkers 桥接实现：把 Core 需要的 Tinkers 能力封装到这里。
 */
public class TinkersBridge implements ITinkersBridge {

    private static final ResourceLocation LIGHT_LEVEL_KEY = ResourceLocation.fromNamespaceAndPath("tcdex", "light_level");
    private static final ResourceLocation LIGHT_LEVEL_OVERRIDE_KEY = ResourceLocation.fromNamespaceAndPath("tcdex", "light_level_override");

    @Override
    public boolean isTinkersTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IModifiable;
    }

    @Override
    public boolean isInitializedTool(ItemStack stack) {
        return isTinkersTool(stack) && ToolStack.isInitialized(stack);
    }

    @Override
    public boolean isUsableTinkersTool(ItemStack stack) {
        return isInitializedTool(stack) && !ToolStack.from(stack).isBroken();
    }

    @Override
    @Nullable
    public ElementType getWeaponElement(ItemStack stack) {
        if (!isTinkersTool(stack)) {
            return null;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return null;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (entry.getModifier() instanceof PrismResonanceModifier) {
                return ElementType.PRISM;
            }
        }
        return ElementalModifier.parseElement(
                tool.getPersistentData().getString(ElementalModifier.ELEMENT_KEY));
    }

    @Override
    @Nullable
    public ElementType getOrInitializeWeaponElement(ItemStack stack) {
        if (!isTinkersTool(stack)) {
            return null;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return null;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (entry.getModifier() instanceof PrismResonanceModifier) {
                return ElementType.PRISM;
            }
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (entry.getModifier() instanceof ElementalModifier elemental) {
                ElementType element = elemental.getElement(tool);
                tool.updateStack(stack);
                return element;
            }
        }
        return null;
    }

    @Override
    public void setWeaponElement(ItemStack stack, ElementType element) {
        if (!isInitializedTool(stack) || element == null) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        tool.getPersistentData().putString(ElementalModifier.ELEMENT_KEY, element.getId());
        tool.updateStack(stack);
    }

    @Override
    public int getCatalystLevel(ItemStack stack) {
        if (!isTinkersTool(stack)) {
            return 0;
        }
        return CatalystManager.getLevel(ToolStack.from(stack));
    }

    @Override
    public void addCatalystProgress(ItemStack stack, int amount) {
        if (!isTinkersTool(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        CatalystManager.addProgress(tool, stack, amount);
    }

    @Override
    public float getToolLightLevel(ItemStack stack) {
        if (!isTinkersTool(stack) || !ToolStack.isInitialized(stack)) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.getPersistentData().contains(LIGHT_LEVEL_OVERRIDE_KEY)) {
            return tool.getPersistentData().getInt(LIGHT_LEVEL_OVERRIDE_KEY);
        }
        int base = 10;
        for (MaterialVariant variant : tool.getMaterials()) {
            if (!variant.isUnknown()) {
                IMaterial material = variant.get();
                base += material.getTier() * 8;
            }
        }
        for (ModifierEntry entry : tool.getModifiers()) {
            base += entry.getLevel() * 3;
        }
        base = Math.max(1, base);
        int infusion = tool.getPersistentData().getInt(LIGHT_LEVEL_KEY);
        return base + infusion;
    }

    @Override
    public int getToolBaseLight(ItemStack stack) {
        if (!isInitializedTool(stack)) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);
        int base = 10;
        for (MaterialVariant variant : tool.getMaterials()) {
            if (!variant.isUnknown()) {
                IMaterial material = variant.get();
                base += material.getTier() * 8;
            }
        }
        for (ModifierEntry entry : tool.getModifiers()) {
            base += entry.getLevel() * 3;
        }
        return Math.max(1, base);
    }

    @Override
    public int getToolInfusion(ItemStack stack) {
        if (!isInitializedTool(stack)) {
            return 0;
        }
        return ToolStack.from(stack).getPersistentData().getInt(LIGHT_LEVEL_KEY);
    }

    @Override
    public void setToolLightLevel(ItemStack stack, int value) {
        if (!isInitializedTool(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        tool.getPersistentData().putInt(LIGHT_LEVEL_OVERRIDE_KEY, Math.max(1, value));
        tool.updateStack(stack);
    }

    @Override
    public void addToolInfusion(ItemStack stack, int amount) {
        if (!isInitializedTool(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        int current = tool.getPersistentData().getInt(LIGHT_LEVEL_KEY);
        tool.getPersistentData().putInt(LIGHT_LEVEL_KEY, Math.max(0, current + amount));
        tool.updateStack(stack);
    }

    @Override
    public boolean hasToolLightOverride(ItemStack stack) {
        return isInitializedTool(stack) && ToolStack.from(stack).getPersistentData().contains(LIGHT_LEVEL_OVERRIDE_KEY);
    }

    @Override
    public void removeToolLightOverride(ItemStack stack) {
        if (!isInitializedTool(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        tool.getPersistentData().remove(LIGHT_LEVEL_OVERRIDE_KEY);
        tool.updateStack(stack);
    }

    @Override
    public int getToolElementalMastery(ItemStack stack) {
        if (!isTinkersTool(stack)) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);
        int mastery = 0;
        for (ModifierEntry entry : tool.getModifierList()) {
            if (entry.getModifier() instanceof ElementalMasteryModifier) {
                mastery += org.tp.tcdex.mastery.ElementalMasteryManager.MASTERY_PER_LEVEL * entry.getLevel();
            }
        }
        return mastery;
    }

    @Override
    public boolean hasModifier(ItemStack stack, String modifierId) {
        if (!isTinkersTool(stack)) {
            return false;
        }
        ToolStack tool = ToolStack.from(stack);
        for (ModifierEntry entry : tool.getModifierList()) {
            if (entry.getId().getPath().equals(modifierId) || entry.getId().toString().equals(modifierId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public float modifyAbsorbed(Player player, float damageAmount, float absorbed) {
        for (ToolStack tool : allTools(player)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                absorbed = entry.getHook(TcdexHooks.PLAYER_SHIELD)
                        .modifyAbsorbed(tool, entry, player, damageAmount, absorbed);
            }
        }
        return absorbed;
    }

    @Override
    public float modifyRegenRate(Player player, float rate) {
        for (ToolStack tool : allTools(player)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                rate = entry.getHook(TcdexHooks.PLAYER_SHIELD)
                        .modifyRegenRate(tool, entry, player, rate);
            }
        }
        return rate;
    }

    @Override
    public float modifyBreakOverflow(Player player, DamageSource source, float overflow) {
        for (ToolStack tool : allTools(player)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                overflow = entry.getHook(TcdexHooks.PLAYER_SHIELD_BREAK)
                        .modifyBreakOverflow(tool, entry, player, source, overflow);
            }
        }
        return overflow;
    }

    @Override
    public void onPlayerShieldBreak(Player player, DamageSource source, float overflow) {
        for (ToolStack tool : allTools(player)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(TcdexHooks.PLAYER_SHIELD_BREAK)
                        .onShieldBreak(tool, entry, player, source, overflow);
            }
        }
    }

    private static java.util.List<ToolStack> allTools(Player player) {
        java.util.List<ToolStack> tools = new java.util.ArrayList<>();
        for (ItemStack stack : java.util.List.of(
                player.getMainHandItem(),
                player.getOffhandItem(),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET))) {
            if (!stack.isEmpty() && stack.getItem() instanceof IModifiable) {
                ToolStack tool = ToolStack.from(stack);
                if (!tool.isBroken()) {
                    tools.add(tool);
                }
            }
        }
        return tools;
    }

    @Override
    public String getApMode(ItemStack stack) {
        if (!isInitializedTool(stack)) {
            return "";
        }
        return ToolStack.from(stack).getPersistentData().getString(
                ResourceLocation.fromNamespaceAndPath("tcdex", "ap_mode"));
    }

    @Override
    public float getApForbidden(ItemStack stack) {
        if (!isInitializedTool(stack)) {
            return 0;
        }
        return ToolStack.from(stack).getPersistentData().getFloat(
                ResourceLocation.fromNamespaceAndPath("tcdex", "ap_forbidden"));
    }

    @Override
    public int getApSin(ItemStack stack) {
        if (!isInitializedTool(stack)) {
            return 0;
        }
        return ToolStack.from(stack).getPersistentData().getInt(
                ResourceLocation.fromNamespaceAndPath("tcdex", "ap_sin_timer"));
    }

    @Override
    public int getApCombo(ItemStack stack) {
        if (!isInitializedTool(stack)) {
            return 0;
        }
        return ToolStack.from(stack).getPersistentData().getInt(
                ResourceLocation.fromNamespaceAndPath("tcdex", "ap_combo"));
    }
    @Override
    public float modifyElementalDamage(ItemStack stack, ElementType element, float amount) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return amount;
        for (ModifierEntry entry : tool.getModifierList()) {
            amount = entry.getHook(TcdexHooks.ELEMENTAL_ATTACK)
                    .modifyElementalDamage(tool, entry, element, amount);
        }
        return amount;
    }

    @Override
    public float modifyShieldEfficiency(ItemStack stack, ElementType shieldElement, float efficiency) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return efficiency;
        for (ModifierEntry entry : tool.getModifierList()) {
            efficiency = entry.getHook(TcdexHooks.ELEMENTAL_ATTACK)
                    .modifyShieldEfficiency(tool, entry, shieldElement, efficiency);
        }
        return efficiency;
    }

    @Override
    public float modifyKineticDamage(ItemStack stack, LivingEntity target, float amount) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return amount;
        for (ModifierEntry entry : tool.getModifierList()) {
            amount = entry.getHook(TcdexHooks.KINETIC_ATTACK)
                    .modifyKineticDamage(tool, entry, target, amount);
        }
        return amount;
    }

    @Override
    public float modifyKineticShieldEfficiency(ItemStack stack, ElementType shieldElement, float efficiency) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return efficiency;
        for (ModifierEntry entry : tool.getModifierList()) {
            efficiency = entry.getHook(TcdexHooks.KINETIC_ATTACK)
                    .modifyKineticShieldEfficiency(tool, entry, shieldElement, efficiency);
        }
        return efficiency;
    }

    @Override
    public float modifyBreakExplosion(ItemStack stack, LivingEntity target, ElementType shieldElement, float damage) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return damage;
        for (ModifierEntry entry : tool.getModifierList()) {
            damage = entry.getHook(TcdexHooks.SHIELD_BREAK)
                    .modifyBreakExplosion(tool, entry, target, shieldElement, damage);
        }
        return damage;
    }

    @Override
    public void onShieldBreak(ItemStack stack, LivingEntity target, ElementType shieldElement, LivingEntity attacker) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return;
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(TcdexHooks.SHIELD_BREAK)
                    .onShieldBreak(tool, entry, target, shieldElement, attacker);
        }
    }

    @Override
    public float modifyKeywordMultiplier(ItemStack stack, ElementType keyword, float multiplier) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return multiplier;
        for (ModifierEntry entry : tool.getModifierList()) {
            multiplier = entry.getHook(TcdexHooks.ELEMENTAL_KEYWORD)
                    .modifyKeywordMultiplier(tool, entry, keyword, multiplier);
        }
        return multiplier;
    }

    @Override
    public float modifyKeywordDamage(ItemStack stack, ElementType keyword, float damage) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return damage;
        for (ModifierEntry entry : tool.getModifierList()) {
            damage = entry.getHook(TcdexHooks.ELEMENTAL_KEYWORD)
                    .modifyKeywordDamage(tool, entry, keyword, damage);
        }
        return damage;
    }

    @Override
    public float modifyKeywordRadius(ItemStack stack, ElementType keyword, float radius) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return radius;
        for (ModifierEntry entry : tool.getModifierList()) {
            radius = entry.getHook(TcdexHooks.ELEMENTAL_KEYWORD)
                    .modifyKeywordRadius(tool, entry, keyword, radius);
        }
        return radius;
    }

    @Override
    public float modifyReactionAuraCost(ItemStack stack, ElementReaction reaction, float auraCost) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return auraCost;
        for (ModifierEntry entry : tool.getModifierList()) {
            auraCost = entry.getHook(TcdexHooks.REACTION)
                    .modifyReactionAuraCost(tool, entry, reaction, auraCost);
        }
        return auraCost;
    }

    @Override
    public float modifyReactionDuration(ItemStack stack, ElementReaction reaction, float duration) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return duration;
        for (ModifierEntry entry : tool.getModifierList()) {
            duration = entry.getHook(TcdexHooks.REACTION)
                    .modifyReactionDuration(tool, entry, reaction, duration);
        }
        return duration;
    }

    @Override
    public float modifyReactionRadius(ItemStack stack, ElementReaction reaction, float radius) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return radius;
        for (ModifierEntry entry : tool.getModifierList()) {
            radius = entry.getHook(TcdexHooks.REACTION)
                    .modifyReactionRadius(tool, entry, reaction, radius);
        }
        return radius;
    }

    @Override
    public float modifyReactionIntensity(ItemStack stack, ElementReaction reaction, float intensity) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return intensity;
        for (ModifierEntry entry : tool.getModifierList()) {
            intensity = entry.getHook(TcdexHooks.REACTION)
                    .modifyReactionIntensity(tool, entry, reaction, intensity);
        }
        return intensity;
    }

    @Override
    public float modifyReactionDamage(ItemStack stack, ElementReaction reaction, float damage) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return damage;
        for (ModifierEntry entry : tool.getModifierList()) {
            damage = entry.getHook(TcdexHooks.REACTION)
                    .modifyReactionDamage(tool, entry, reaction, damage);
        }
        return damage;
    }

    @Override
    public int modifyReactionCooldown(ItemStack stack, ElementReaction reaction, int cooldown) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return cooldown;
        for (ModifierEntry entry : tool.getModifierList()) {
            cooldown = entry.getHook(TcdexHooks.REACTION)
                    .modifyReactionCooldown(tool, entry, reaction, cooldown);
        }
        return cooldown;
    }

    @Override
    public void onReactionTriggered(ItemStack stack, LivingEntity target, ElementReaction reaction, LivingEntity source, float intensity) {
        ToolStack tool = toolFrom(stack);
        if (tool == null) return;
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(TcdexHooks.REACTION)
                    .onReactionTriggered(tool, entry, target, reaction, source, intensity);
        }
    }

    @Override
    public void applyFiveForcesHitState(ItemStack stack, Player player, LivingEntity target, ElementType element) {
        ToolStack tool = toolFrom(stack);
        if (tool != null) {
            FiveForcesModifier.applyHitState(tool, player, target, element);
        }
    }

    private static ToolStack toolFrom(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)) {
            return null;
        }
        ToolStack tool = ToolStack.from(stack);
        return tool.isBroken() ? null : tool;
    }
}
