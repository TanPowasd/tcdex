package org.tp.tcdex.integration.tinkers.modifier.base;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.tp.tcdex.integration.tinkers.modifier.hook.ElementalKeywordHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.ElementalStateApplyHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.KillingHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.KineticAttackModifierHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.PlayerShieldBreakHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.PlayerShieldHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.ReactionModifierHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.ShieldBreakHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.TcdexHooks;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ArmorWalkModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.DamageBlockModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.ElytraFlightModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.ProtectionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.MaterialRepairModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ProcessLootModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.RepairFactorModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolActionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.CraftCountModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierTraitHook;
import slimeknights.tconstruct.library.modifiers.hook.build.RawDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.ArmorLootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.DamageDealtModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MonsterMeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.RequirementsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.AreaOfEffectHighlightModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.KeybindInteractModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockBreakModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockHarvestModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.HarvestEnchantmentsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.RemoveBlockModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.BowAmmoModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.LauncherHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileFuseModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ScheduledProjectileTaskModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.BlockTransformModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.CapacityBarHook;
import slimeknights.tconstruct.library.modifiers.hook.special.PlantHarvestModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.ShearsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.sling.SlingAngleModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.sling.SlingForceModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.sling.SlingLaunchModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.utils.RestrictedCompoundTag;
import slimeknights.tconstruct.library.utils.Schedule;
import slimeknights.tconstruct.library.materials.definition.MaterialId;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * TCDEX 全能词条基类（对标 sakuratinker 的 BaseModifier）。
 *
 * <p>一次性 implements 匠魂 3.10 的全部原生 hook 接口 + TCDEX 自定义 {@link KillingHook}，
 * registerHooks 里一次性注册全部 hook。所有接口方法均委托给可覆写的 {@code modifierXxx()} 方法
 * （全部空实现 / 返回安全默认值），子类只需按需覆写对应方法。</p>
 *
 * <p>注意：sling 三个 hook 接口匠魂未在 {@link ModifierHooks} 提供注册字段，仅做接口兼容，
 * 子类覆写不会生效（TCDEX 无 sling 工具，无实际影响）。</p>
 */
public abstract class TcdexBaseModifier extends Modifier implements
        // armor
        ArmorWalkModifierHook, DamageBlockModifierHook, ElytraFlightModifierHook, EquipmentChangeModifierHook,
        ModifyDamageModifierHook, OnAttackedModifierHook, ProtectionModifierHook,
        // behavior
        AttributesModifierHook, EnchantmentModifierHook, MaterialRepairModifierHook, ProcessLootModifierHook,
        RepairFactorModifierHook, ToolActionModifierHook, ToolDamageModifierHook,
        // build
        ConditionalStatModifierHook, CraftCountModifierHook, ModifierRemovalHook, ModifierTraitHook,
        RawDataModifierHook, ToolStatsModifierHook, ValidateModifierHook, VolatileDataModifierHook,
        // combat
        ArmorLootingModifierHook, DamageDealtModifierHook, LootingModifierHook, MeleeDamageModifierHook,
        MeleeHitModifierHook, MonsterMeleeHitModifierHook,
        // display
        DisplayNameModifierHook, DurabilityDisplayModifierHook, RequirementsModifierHook, TooltipModifierHook,
        // interaction
        AreaOfEffectHighlightModifierHook, BlockInteractionModifierHook, EntityInteractionModifierHook,
        GeneralInteractionModifierHook, InventoryTickModifierHook, KeybindInteractModifierHook,
        SlotStackModifierHook, UsingToolModifierHook,
        // mining
        BlockBreakModifierHook, BlockHarvestModifierHook, BreakSpeedModifierHook, HarvestEnchantmentsModifierHook,
        RemoveBlockModifierHook,
        // ranged
        BowAmmoModifierHook, LauncherHitModifierHook, ProjectileFuseModifierHook, ProjectileHitModifierHook,
        ProjectileLaunchModifierHook, ScheduledProjectileTaskModifierHook,
        // special
        BlockTransformModifierHook, CapacityBarHook, PlantHarvestModifierHook, ShearsModifierHook,
        SlingAngleModifierHook, SlingForceModifierHook, SlingLaunchModifierHook,
        // TCDEX 自定义
        KillingHook, ShieldBreakHook, PlayerShieldHook, PlayerShieldBreakHook, ElementalStateApplyHook,
        ElementalKeywordHook, KineticAttackModifierHook, ReactionModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this,
                // armor
                ModifierHooks.BOOT_WALK, ModifierHooks.DAMAGE_BLOCK, ModifierHooks.ELYTRA_FLIGHT, ModifierHooks.EQUIPMENT_CHANGE,
                ModifierHooks.MODIFY_HURT, ModifierHooks.MODIFY_DAMAGE, ModifierHooks.ON_ATTACKED, ModifierHooks.PROTECTION,
                // behavior
                ModifierHooks.ATTRIBUTES, ModifierHooks.ENCHANTMENTS, ModifierHooks.MATERIAL_REPAIR, ModifierHooks.PROCESS_LOOT,
                ModifierHooks.REPAIR_FACTOR, ModifierHooks.TOOL_ACTION, ModifierHooks.TOOL_DAMAGE,
                // build
                ModifierHooks.CONDITIONAL_STAT, ModifierHooks.CRAFT_COUNT, ModifierHooks.REMOVE, ModifierHooks.MODIFIER_TRAITS,
                ModifierHooks.RAW_DATA, ModifierHooks.TOOL_STATS, ModifierHooks.VALIDATE, ModifierHooks.VALIDATE_UPGRADE,
                ModifierHooks.VOLATILE_DATA,
                // combat
                ModifierHooks.ARMOR_LOOTING, ModifierHooks.DAMAGE_DEALT, ModifierHooks.WEAPON_LOOTING,
                ModifierHooks.MELEE_DAMAGE, ModifierHooks.MONSTER_MELEE_DAMAGE, ModifierHooks.MELEE_HIT, ModifierHooks.MONSTER_MELEE_HIT,
                // display
                ModifierHooks.DISPLAY_NAME, ModifierHooks.DURABILITY_DISPLAY, ModifierHooks.REQUIREMENTS, ModifierHooks.TOOLTIP,
                // interaction
                ModifierHooks.AOE_HIGHLIGHT, ModifierHooks.BLOCK_INTERACT, ModifierHooks.ENTITY_INTERACT, ModifierHooks.GENERAL_INTERACT,
                ModifierHooks.INVENTORY_TICK, ModifierHooks.ARMOR_INTERACT, ModifierHooks.SLOT_STACK, ModifierHooks.TOOL_USING,
                // mining
                ModifierHooks.BLOCK_BREAK, ModifierHooks.BLOCK_HARVEST, ModifierHooks.BREAK_SPEED, ModifierHooks.HARVEST_ENCHANTMENTS,
                ModifierHooks.REMOVE_BLOCK,
                // ranged
                ModifierHooks.BOW_AMMO, ModifierHooks.LAUNCHER_HIT, ModifierHooks.PROJECTILE_FUSE, ModifierHooks.PROJECTILE_HIT,
                ModifierHooks.PROJECTILE_HIT_CLIENT, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_SHOT,
                ModifierHooks.PROJECTILE_THROWN, ModifierHooks.SCHEDULE_PROJECTILE_TASK,
                // special
                ModifierHooks.BLOCK_TRANSFORM, ModifierHooks.CAPACITY_BAR, ModifierHooks.PLANT_HARVEST, ModifierHooks.SHEAR_ENTITY,
                // TCDEX 自定义
                TcdexHooks.KILLING_HOOK, TcdexHooks.SHIELD_BREAK, TcdexHooks.PLAYER_SHIELD,
                TcdexHooks.PLAYER_SHIELD_BREAK, TcdexHooks.ELEMENTAL_STATE_APPLY, TcdexHooks.ELEMENTAL_KEYWORD,
                TcdexHooks.KINETIC_ATTACK, TcdexHooks.REACTION);
    }

    // ========================================================================
    //  armor
    // ========================================================================

    @Override
    public void onWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
        modifierOnWalk(tool, modifier, living, prevPos, newPos);
    }

    @Override
    public boolean isDamageBlocked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slot, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return modifierIsDamageBlocked(tool, modifier, context, slot, source, amount);
    }

    @Override
    public boolean elytraFlightTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int flightTicks) {
        return modifierElytraFlightTick(tool, modifier, entity, flightTicks);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        modifierOnEquip(tool, modifier, context);
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        modifierOnUnequip(tool, modifier, context);
    }

    @Override
    public void onEquipmentChange(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context, EquipmentSlot slotType) {
        modifierOnEquipmentChange(tool, modifier, context, slotType);
    }

    @Override
    public float modifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source, float amount, boolean isDirectDamage) {
        return modifierModifyDamageTaken(tool, modifier, context, slotType, source, amount, isDirectDamage);
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source, float amount, boolean isDirectDamage) {
        modifierOnAttacked(tool, modifier, context, slotType, source, amount, isDirectDamage);
    }

    @Override
    public float getProtectionModifier(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return modifierGetProtectionModifier(tool, modifier, context, slotType, source, amount);
    }

    // ========================================================================
    //  behavior
    // ========================================================================

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> consumer) {
        modifierAddAttributes(tool, modifier, slot, consumer);
    }

    @Override
    public int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Enchantment enchantment, int level) {
        return modifierUpdateEnchantmentLevel(tool, modifier, enchantment, level);
    }

    @Override
    public void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment, Integer> map) {
        modifierUpdateEnchantments(tool, modifier, map);
    }

    @Override
    public boolean isRepairMaterial(IToolStackView tool, ModifierEntry modifier, MaterialId material) {
        return modifierIsRepairMaterial(tool, modifier, material);
    }

    @Override
    public float getRepairAmount(IToolStackView tool, ModifierEntry modifier, MaterialId material) {
        return modifierGetRepairAmount(tool, modifier, material);
    }

    @Override
    public void processLoot(IToolStackView tool, ModifierEntry modifier, List<ItemStack> list, net.minecraft.world.level.storage.loot.LootContext context) {
        modifierProcessLoot(tool, modifier, list, context);
    }

    @Override
    public float getRepairFactor(IToolStackView tool, ModifierEntry modifier, float factor) {
        return modifierGetRepairFactor(tool, modifier, factor);
    }

    @Override
    public boolean canPerformAction(IToolStackView tool, ModifierEntry modifier, ToolAction toolAction) {
        return modifierCanPerformAction(tool, modifier, toolAction);
    }

    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, LivingEntity holder) {
        return modifierOnDamageTool(tool, modifier, amount, holder);
    }

    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, LivingEntity holder, ItemStack stack) {
        return modifierOnDamageTool(tool, modifier, amount, holder, stack);
    }

    // ========================================================================
    //  build
    // ========================================================================

    @Override
    public float modifyStat(IToolStackView tool, ModifierEntry modifier, LivingEntity living, FloatToolStat stat, float baseValue, float value) {
        return modifierModifyStat(tool, modifier, living, stat, baseValue, value);
    }

    @Override
    public float modifyCraftCount(IToolStackView tool, ModifierEntry modifier, float value) {
        return modifierModifyCraftCount(tool, modifier, value);
    }

    @Override
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        return modifierOnRemoved(tool, modifier);
    }

    @Override
    public void addTraits(IToolContext context, ModifierEntry modifier, ModifierTraitHook.TraitBuilder builder, boolean firstLevel) {
        modifierAddTraits(context, modifier, builder, firstLevel);
    }

    @Override
    public void addRawData(IToolStackView tool, ModifierEntry modifier, RestrictedCompoundTag tag) {
        modifierAddRawData(tool, modifier, tag);
    }

    @Override
    public void removeRawData(IToolStackView tool, Modifier modifier, RestrictedCompoundTag tag) {
        modifierRemoveRawData(tool, modifier, tag);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        modifierAddToolStats(context, modifier, builder);
    }

    @Override
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        return modifierValidate(tool, modifier);
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT data) {
        modifierAddVolatileData(context, modifier, data);
    }

    // ========================================================================
    //  combat
    // ========================================================================

    @Override
    public int updateArmorLooting(IToolStackView tool, ModifierEntry modifier, LootingContext context, EquipmentContext equipmentContext, EquipmentSlot slotType, int looting) {
        return modifierUpdateArmorLooting(tool, modifier, context, equipmentContext, slotType, looting);
    }

    @Override
    public void onDamageDealt(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount, boolean isDirectDamage) {
        modifierOnDamageDealt(tool, modifier, context, slotType, target, source, amount, isDirectDamage);
    }

    @Override
    public int updateLooting(IToolStackView tool, ModifierEntry modifier, LootingContext context, int looting) {
        return modifierUpdateLooting(tool, modifier, context, looting);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        return modifierMeleeDamage(tool, modifier, context, baseDamage, damage);
    }

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        return modifierBeforeMeleeHit(tool, modifier, context, damage, baseKnockback, knockback);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        modifierAfterMeleeHit(tool, modifier, context, damageDealt);
    }

    @Override
    public void failedMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
        modifierFailedMeleeHit(tool, modifier, context, damage);
    }

    @Override
    public void onMonsterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
        modifierOnMonsterMeleeHit(tool, modifier, context, damage);
    }

    // ========================================================================
    //  display
    // ========================================================================

    @Override
    public Component getDisplayName(IToolStackView tool, ModifierEntry modifier, Component name, net.minecraft.core.RegistryAccess registryAccess) {
        return modifierGetDisplayName(tool, modifier, name, registryAccess);
    }

    @Override
    public Boolean showDurabilityBar(IToolStackView tool, ModifierEntry modifier) {
        return modifierShowDurabilityBar(tool, modifier);
    }

    @Override
    public int getDurabilityWidth(IToolStackView tool, ModifierEntry modifier) {
        return modifierGetDurabilityWidth(tool, modifier);
    }

    @Override
    public int getDurabilityRGB(IToolStackView tool, ModifierEntry modifier) {
        return modifierGetDurabilityRGB(tool, modifier);
    }

    @Override
    public List<ModifierEntry> displayModifiers(ModifierEntry modifier) {
        return modifierDisplayModifiers(modifier);
    }

    @Override
    public Component requirementsError(ModifierEntry modifier) {
        return modifierRequirementsError(modifier);
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        modifierAddTooltip(tool, modifier, player, tooltip, tooltipKey, tooltipFlag);
    }

    // ========================================================================
    //  interaction
    // ========================================================================

    @Override
    public boolean shouldHighlight(IToolStackView tool, ModifierEntry modifier, UseOnContext context, BlockPos pos, BlockState state) {
        return modifierShouldHighlight(tool, modifier, context, pos, state);
    }

    @Override
    public InteractionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        return modifierBeforeBlockUse(tool, modifier, context, source);
    }

    @Override
    public InteractionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        return modifierAfterBlockUse(tool, modifier, context, source);
    }

    @Override
    public InteractionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, Player player, Entity target, InteractionHand hand, InteractionSource source) {
        return modifierBeforeEntityUse(tool, modifier, player, target, hand, source);
    }

    @Override
    public InteractionResult afterEntityUse(IToolStackView tool, ModifierEntry modifier, Player player, LivingEntity target, InteractionHand hand, InteractionSource source) {
        return modifierAfterEntityUse(tool, modifier, player, target, hand, source);
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        return modifierOnToolUse(tool, modifier, player, hand, source);
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        modifierOnUsingTick(tool, modifier, entity, timeLeft);
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        modifierOnStoppedUsing(tool, modifier, entity, timeLeft);
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        modifierOnFinishUsing(tool, modifier, entity);
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return modifierGetUseDuration(tool, modifier);
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return modifierGetUseAction(tool, modifier);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        modifierOnInventoryTick(tool, modifier, world, holder, itemSlot, isSelected, isCorrectSlot, stack);
    }

    @Override
    public boolean startInteract(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot slot, TooltipKey key) {
        return modifierStartInteract(tool, modifier, player, slot, key);
    }

    @Override
    public void stopInteract(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot slot) {
        modifierStopInteract(tool, modifier, player, slot);
    }

    @Override
    public boolean overrideStackedOnOther(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.inventory.Slot slot, Player player) {
        return modifierOverrideStackedOnOther(tool, modifier, slot, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(IToolStackView tool, ModifierEntry modifier, ItemStack stack, net.minecraft.world.inventory.Slot slot, Player player, net.minecraft.world.entity.SlotAccess access) {
        return modifierOverrideOtherStackedOnMe(tool, modifier, stack, slot, player, access);
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft, int drawtime, ModifierEntry activeModifier) {
        modifierOnUsingTickActive(tool, modifier, entity, timeLeft, drawtime, activeModifier);
    }

    @Override
    public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft, int drawtime, ModifierEntry activeModifier) {
        modifierBeforeReleaseUsing(tool, modifier, entity, timeLeft, drawtime, activeModifier);
    }

    @Override
    public void afterStopUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft, int drawtime, ModifierEntry activeModifier) {
        modifierAfterStopUsing(tool, modifier, entity, timeLeft, drawtime, activeModifier);
    }

    // ========================================================================
    //  mining
    // ========================================================================

    @Override
    public void afterBlockBreak(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        modifierAfterBlockBreak(tool, modifier, context);
    }

    @Override
    public void startHarvest(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        modifierStartHarvest(tool, modifier, context);
    }

    @Override
    public void finishHarvest(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context, int harvested) {
        modifierFinishHarvest(tool, modifier, context, harvested);
    }

    @Override
    public void onBreakSpeed(IToolStackView tool, ModifierEntry modifier, PlayerEvent.BreakSpeed event, net.minecraft.core.Direction sideHit, boolean isEffective, float miningSpeedModifier) {
        modifierOnBreakSpeed(tool, modifier, event, sideHit, isEffective, miningSpeedModifier);
    }

    @Override
    public float modifyBreakSpeed(IToolStackView tool, ModifierEntry modifier, BreakSpeedContext context, float speed) {
        return modifierModifyBreakSpeed(tool, modifier, context, speed);
    }

    @Override
    public void updateHarvestEnchantments(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context, EquipmentContext equipmentContext, EquipmentSlot slotType, Map<Enchantment, Integer> map) {
        modifierUpdateHarvestEnchantments(tool, modifier, context, equipmentContext, slotType, map);
    }

    @Override
    public Boolean removeBlock(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        return modifierRemoveBlock(tool, modifier, context);
    }

    // ========================================================================
    //  ranged
    // ========================================================================

    @Override
    public ItemStack findAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack standardAmmo, Predicate<ItemStack> ammoPredicate) {
        return modifierFindAmmo(tool, modifier, shooter, standardAmmo, ammoPredicate);
    }

    @Override
    public void shrinkAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack ammo, int needed) {
        modifierShrinkAmmo(tool, modifier, shooter, ammo, needed);
    }

    @Override
    public void onLauncherHitEntity(IToolStackView tool, ModifierEntry modifier, Projectile projectile, LivingEntity shooter, Entity target, LivingEntity attacker, float velocity) {
        modifierOnLauncherHitEntity(tool, modifier, projectile, shooter, target, attacker, velocity);
    }

    @Override
    public void onLauncherHitBlock(IToolStackView tool, ModifierEntry modifier, Projectile projectile, LivingEntity shooter, BlockPos pos) {
        modifierOnLauncherHitBlock(tool, modifier, projectile, shooter, pos);
    }

    @Override
    public void onProjectileFuseFinish(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, ItemStack stack, Projectile projectile, AbstractArrow arrow) {
        modifierOnProjectileFuseFinish(modifiers, persistentData, modifier, stack, projectile, arrow);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, LivingEntity attacker, LivingEntity target) {
        return modifierOnProjectileHitEntity(modifiers, persistentData, modifier, projectile, hit, attacker, target);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, LivingEntity attacker, LivingEntity target, boolean isCritical) {
        return modifierOnProjectileHitEntity(modifiers, persistentData, modifier, projectile, hit, attacker, target, isCritical);
    }

    @Override
    public void onProjectileHitBlock(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, BlockHitResult hit, LivingEntity attacker) {
        modifierOnProjectileHitBlock(modifiers, persistentData, modifier, projectile, hit, attacker);
    }

    @Override
    public boolean onProjectileHitsBlock(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, BlockHitResult hit, LivingEntity attacker) {
        return modifierOnProjectileHitsBlock(modifiers, persistentData, modifier, projectile, hit, attacker);
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        modifierOnProjectileLaunch(tool, modifier, shooter, projectile, arrow, persistentData, primary);
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        modifierOnProjectileLaunch(tool, modifier, shooter, stack, projectile, arrow, persistentData, primary);
    }

    @Override
    public void onProjectileShoot(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
        modifierOnProjectileShoot(tool, modifier, shooter, stack, projectile, arrow, persistentData, primary);
    }

    @Override
    public void scheduleProjectileTask(IToolStackView tool, ModifierEntry modifier, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, Schedule.Scheduler scheduler) {
        modifierScheduleProjectileTask(tool, modifier, stack, projectile, arrow, persistentData, scheduler);
    }

    @Override
    public void onScheduledProjectileTask(IToolStackView tool, ModifierEntry modifier, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, int tick) {
        modifierOnScheduledProjectileTask(tool, modifier, stack, projectile, arrow, persistentData, tick);
    }

    // ========================================================================
    //  special
    // ========================================================================

    @Override
    public void afterTransformBlock(IToolStackView tool, ModifierEntry modifier, UseOnContext context, BlockState state, BlockPos pos, ToolAction action) {
        modifierAfterTransformBlock(tool, modifier, context, state, pos, action);
    }

    @Override
    public int getAmount(IToolStackView tool) {
        return modifierGetAmount(tool);
    }

    @Override
    public int getCapacity(IToolStackView tool, ModifierEntry modifier) {
        return modifierGetCapacity(tool, modifier);
    }

    @Override
    public void setAmount(IToolStackView tool, ModifierEntry modifier, int amount) {
        modifierSetAmount(tool, modifier, amount);
    }

    @Override
    public void addAmount(IToolStackView tool, ModifierEntry modifier, int amount) {
        modifierAddAmount(tool, modifier, amount);
    }

    @Override
    public void removeAmount(IToolStackView tool, ModifierEntry modifier, int amount) {
        modifierRemoveAmount(tool, modifier, amount);
    }

    @Override
    public void afterHarvest(IToolStackView tool, ModifierEntry modifier, UseOnContext context, ServerLevel world, BlockState state, BlockPos pos) {
        modifierAfterHarvest(tool, modifier, context, world, state, pos);
    }

    @Override
    public void afterShearEntity(IToolStackView tool, ModifierEntry modifier, Player player, Entity entity, boolean isSheared) {
        modifierAfterShearEntity(tool, modifier, player, entity, isSheared);
    }

    @Override
    public Vec3 modifySlingAngle(IToolStackView tool, ModifierEntry modifier, LivingEntity player, LivingEntity target, ModifierEntry sling, float velocity, float power, Vec3 angle) {
        return modifierModifySlingAngle(tool, modifier, player, target, sling, velocity, power, angle);
    }

    @Override
    public float modifySlingForce(IToolStackView tool, ModifierEntry modifier, LivingEntity player, LivingEntity target, ModifierEntry sling, float velocity, float power) {
        return modifierModifySlingForce(tool, modifier, player, target, sling, velocity, power);
    }

    @Override
    public void afterSlingLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity player, LivingEntity target, ModifierEntry sling, float velocity, float power, Vec3 angle) {
        modifierAfterSlingLaunch(tool, modifier, player, target, sling, velocity, power, angle);
    }

    // ========================================================================
    //  TCDEX 自定义 hook
    // ========================================================================

    @Override
    public void onKillLivingTarget(IToolStackView tool, net.minecraftforge.event.entity.living.LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        modifierOnKillLivingTarget(tool, event, attacker, target, level);
    }

    @Override
    public float modifyBreakExplosion(IToolStackView tool, ModifierEntry modifier, LivingEntity target, org.tp.tcdex.element.ElementType shieldElement, float damage) {
        return modifierModifyBreakExplosion(tool, modifier, target, shieldElement, damage);
    }

    @Override
    public void onShieldBreak(IToolStackView tool, ModifierEntry modifier, LivingEntity target, org.tp.tcdex.element.ElementType shieldElement, LivingEntity attacker) {
        modifierOnShieldBreak(tool, modifier, target, shieldElement, attacker);
    }

    @Override
    public float modifyAbsorbed(IToolStackView tool, ModifierEntry modifier, Player player, float damageAmount, float absorbed) {
        return modifierModifyAbsorbed(tool, modifier, player, damageAmount, absorbed);
    }

    @Override
    public float modifyRegenRate(IToolStackView tool, ModifierEntry modifier, Player player, float rate) {
        return modifierModifyRegenRate(tool, modifier, player, rate);
    }

    @Override
    public float modifyBreakOverflow(IToolStackView tool, ModifierEntry modifier, Player player, net.minecraft.world.damagesource.DamageSource source, float overflow) {
        return modifierModifyBreakOverflow(tool, modifier, player, source, overflow);
    }

    @Override
    public void onShieldBreak(IToolStackView tool, ModifierEntry modifier, Player player, net.minecraft.world.damagesource.DamageSource source, float overflow) {
        modifierOnShieldBreak(tool, modifier, player, source, overflow);
    }

    @Override
    public float modifyStateStacks(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType element, float stacks) {
        return modifierModifyStateStacks(tool, modifier, element, stacks);
    }

    @Override
    public int modifyStateDuration(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType element, int duration) {
        return modifierModifyStateDuration(tool, modifier, element, duration);
    }

    @Override
    public float modifyKeywordMultiplier(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType keyword, float multiplier) {
        return modifierModifyKeywordMultiplier(tool, modifier, keyword, multiplier);
    }

    @Override
    public float modifyKeywordDamage(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType keyword, float damage) {
        return modifierModifyKeywordDamage(tool, modifier, keyword, damage);
    }

    @Override
    public float modifyKeywordRadius(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType keyword, float radius) {
        return modifierModifyKeywordRadius(tool, modifier, keyword, radius);
    }

    @Override
    public float modifyKineticDamage(IToolStackView tool, ModifierEntry modifier, LivingEntity target, float amount) {
        return modifierModifyKineticDamage(tool, modifier, target, amount);
    }

    @Override
    public float modifyKineticShieldEfficiency(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType shieldElement, float efficiency) {
        return modifierModifyKineticShieldEfficiency(tool, modifier, shieldElement, efficiency);
    }

    @Override
    public float modifyReactionDuration(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float duration) {
        return modifierModifyReactionDuration(tool, modifier, reaction, duration);
    }

    @Override
    public float modifyReactionRadius(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float radius) {
        return modifierModifyReactionRadius(tool, modifier, reaction, radius);
    }

    @Override
    public float modifyReactionIntensity(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float intensity) {
        return modifierModifyReactionIntensity(tool, modifier, reaction, intensity);
    }

    @Override
    public int modifyReactionCooldown(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, int cooldown) {
        return modifierModifyReactionCooldown(tool, modifier, reaction, cooldown);
    }

    @Override
    public float modifyReactionAuraCost(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float auraCost) {
        return modifierModifyReactionAuraCost(tool, modifier, reaction, auraCost);
    }

    @Override
    public float modifyReactionDamage(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float damage) {
        return modifierModifyReactionDamage(tool, modifier, reaction, damage);
    }

    @Override
    public void onReactionTriggered(IToolStackView tool, ModifierEntry modifier, LivingEntity target,
                                    org.tp.tcdex.reaction.ElementReaction reaction, @javax.annotation.Nullable LivingEntity source, float finalIntensity) {
        modifierOnReactionTriggered(tool, modifier, target, reaction, source, finalIntensity);
    }

    // ========================================================================
    //  可覆写方法（全部空实现 / 安全默认值，子类按需覆写）
    // ========================================================================

    // armor
    protected void modifierOnWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
    }

    protected boolean modifierIsDamageBlocked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slot, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    protected boolean modifierElytraFlightTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int flightTicks) {
        return false;
    }

    protected void modifierOnEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
    }

    protected void modifierOnUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
    }

    protected void modifierOnEquipmentChange(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context, EquipmentSlot slotType) {
    }

    protected float modifierModifyDamageTaken(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source, float amount, boolean isDirectDamage) {
        return amount;
    }

    protected void modifierOnAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source, float amount, boolean isDirectDamage) {
    }

    protected float modifierGetProtectionModifier(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return 0;
    }

    // behavior
    protected void modifierAddAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> consumer) {
    }

    protected int modifierUpdateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Enchantment enchantment, int level) {
        return level;
    }

    protected void modifierUpdateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment, Integer> map) {
    }

    protected boolean modifierIsRepairMaterial(IToolStackView tool, ModifierEntry modifier, MaterialId material) {
        return false;
    }

    protected float modifierGetRepairAmount(IToolStackView tool, ModifierEntry modifier, MaterialId material) {
        return 0;
    }

    protected void modifierProcessLoot(IToolStackView tool, ModifierEntry modifier, List<ItemStack> list, net.minecraft.world.level.storage.loot.LootContext context) {
    }

    protected float modifierGetRepairFactor(IToolStackView tool, ModifierEntry modifier, float factor) {
        return factor;
    }

    protected boolean modifierCanPerformAction(IToolStackView tool, ModifierEntry modifier, ToolAction toolAction) {
        return false;
    }

    protected int modifierOnDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, LivingEntity holder) {
        return amount;
    }

    protected int modifierOnDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, LivingEntity holder, ItemStack stack) {
        return amount;
    }

    // build
    protected float modifierModifyStat(IToolStackView tool, ModifierEntry modifier, LivingEntity living, FloatToolStat stat, float baseValue, float value) {
        return value;
    }

    protected float modifierModifyCraftCount(IToolStackView tool, ModifierEntry modifier, float value) {
        return value;
    }

    protected Component modifierOnRemoved(IToolStackView tool, Modifier modifier) {
        return null;
    }

    protected void modifierAddTraits(IToolContext context, ModifierEntry modifier, ModifierTraitHook.TraitBuilder builder, boolean firstLevel) {
    }

    protected void modifierAddRawData(IToolStackView tool, ModifierEntry modifier, RestrictedCompoundTag tag) {
    }

    protected void modifierRemoveRawData(IToolStackView tool, Modifier modifier, RestrictedCompoundTag tag) {
    }

    protected void modifierAddToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
    }

    protected Component modifierValidate(IToolStackView tool, ModifierEntry modifier) {
        return null;
    }

    protected void modifierAddVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT data) {
    }

    // combat
    protected int modifierUpdateArmorLooting(IToolStackView tool, ModifierEntry modifier, LootingContext context, EquipmentContext equipmentContext, EquipmentSlot slotType, int looting) {
        return looting;
    }

    protected void modifierOnDamageDealt(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount, boolean isDirectDamage) {
    }

    protected int modifierUpdateLooting(IToolStackView tool, ModifierEntry modifier, LootingContext context, int looting) {
        return looting;
    }

    protected float modifierMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        return damage;
    }

    protected float modifierBeforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        return knockback;
    }

    protected void modifierAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
    }

    protected void modifierFailedMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
    }

    protected void modifierOnMonsterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage) {
    }

    // display
    protected Component modifierGetDisplayName(IToolStackView tool, ModifierEntry modifier, Component name, net.minecraft.core.RegistryAccess registryAccess) {
        return name;
    }

    protected Boolean modifierShowDurabilityBar(IToolStackView tool, ModifierEntry modifier) {
        return null;
    }

    protected int modifierGetDurabilityWidth(IToolStackView tool, ModifierEntry modifier) {
        return 0;
    }

    protected int modifierGetDurabilityRGB(IToolStackView tool, ModifierEntry modifier) {
        return 0xFFFFFF;
    }

    protected List<ModifierEntry> modifierDisplayModifiers(ModifierEntry modifier) {
        return null;
    }

    protected Component modifierRequirementsError(ModifierEntry modifier) {
        return null;
    }

    protected void modifierAddTooltip(IToolStackView tool, ModifierEntry modifier, Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    }

    // interaction
    protected boolean modifierShouldHighlight(IToolStackView tool, ModifierEntry modifier, UseOnContext context, BlockPos pos, BlockState state) {
        return false;
    }

    protected InteractionResult modifierBeforeBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        return InteractionResult.PASS;
    }

    protected InteractionResult modifierAfterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
        return InteractionResult.PASS;
    }

    protected InteractionResult modifierBeforeEntityUse(IToolStackView tool, ModifierEntry modifier, Player player, Entity target, InteractionHand hand, InteractionSource source) {
        return InteractionResult.PASS;
    }

    protected InteractionResult modifierAfterEntityUse(IToolStackView tool, ModifierEntry modifier, Player player, LivingEntity target, InteractionHand hand, InteractionSource source) {
        return InteractionResult.PASS;
    }

    protected InteractionResult modifierOnToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        return InteractionResult.PASS;
    }

    protected void modifierOnUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
    }

    protected void modifierOnStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
    }

    protected void modifierOnFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
    }

    protected int modifierGetUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 0;
    }

    protected UseAnim modifierGetUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.NONE;
    }

    protected void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
    }

    protected boolean modifierStartInteract(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot slot, TooltipKey key) {
        return false;
    }

    protected void modifierStopInteract(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot slot) {
    }

    protected boolean modifierOverrideStackedOnOther(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.inventory.Slot slot, Player player) {
        return false;
    }

    protected boolean modifierOverrideOtherStackedOnMe(IToolStackView tool, ModifierEntry modifier, ItemStack stack, net.minecraft.world.inventory.Slot slot, Player player, net.minecraft.world.entity.SlotAccess access) {
        return false;
    }

    protected void modifierOnUsingTickActive(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft, int drawtime, ModifierEntry activeModifier) {
    }

    protected void modifierBeforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft, int drawtime, ModifierEntry activeModifier) {
    }

    protected void modifierAfterStopUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft, int drawtime, ModifierEntry activeModifier) {
    }

    // mining
    protected void modifierAfterBlockBreak(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
    }

    protected void modifierStartHarvest(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
    }

    protected void modifierFinishHarvest(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context, int harvested) {
    }

    protected void modifierOnBreakSpeed(IToolStackView tool, ModifierEntry modifier, PlayerEvent.BreakSpeed event, net.minecraft.core.Direction sideHit, boolean isEffective, float miningSpeedModifier) {
    }

    protected float modifierModifyBreakSpeed(IToolStackView tool, ModifierEntry modifier, BreakSpeedContext context, float speed) {
        return speed;
    }

    protected void modifierUpdateHarvestEnchantments(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context, EquipmentContext equipmentContext, EquipmentSlot slotType, Map<Enchantment, Integer> map) {
    }

    protected Boolean modifierRemoveBlock(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        return null;
    }

    // ranged
    protected ItemStack modifierFindAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack standardAmmo, Predicate<ItemStack> ammoPredicate) {
        return null;
    }

    protected void modifierShrinkAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack ammo, int needed) {
    }

    protected void modifierOnLauncherHitEntity(IToolStackView tool, ModifierEntry modifier, Projectile projectile, LivingEntity shooter, Entity target, LivingEntity attacker, float velocity) {
    }

    protected void modifierOnLauncherHitBlock(IToolStackView tool, ModifierEntry modifier, Projectile projectile, LivingEntity shooter, BlockPos pos) {
    }

    protected void modifierOnProjectileFuseFinish(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, ItemStack stack, Projectile projectile, AbstractArrow arrow) {
    }

    protected boolean modifierOnProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, LivingEntity attacker, LivingEntity target) {
        return false;
    }

    protected boolean modifierOnProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, LivingEntity attacker, LivingEntity target, boolean isCritical) {
        return false;
    }

    protected void modifierOnProjectileHitBlock(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, BlockHitResult hit, LivingEntity attacker) {
    }

    protected boolean modifierOnProjectileHitsBlock(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, Projectile projectile, BlockHitResult hit, LivingEntity attacker) {
        return false;
    }

    protected void modifierOnProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
    }

    protected void modifierOnProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
    }

    protected void modifierOnProjectileShoot(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
    }

    protected void modifierScheduleProjectileTask(IToolStackView tool, ModifierEntry modifier, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, Schedule.Scheduler scheduler) {
    }

    protected void modifierOnScheduledProjectileTask(IToolStackView tool, ModifierEntry modifier, ItemStack stack, Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData, int tick) {
    }

    // special
    protected void modifierAfterTransformBlock(IToolStackView tool, ModifierEntry modifier, UseOnContext context, BlockState state, BlockPos pos, ToolAction action) {
    }

    protected int modifierGetAmount(IToolStackView tool) {
        return 0;
    }

    protected int modifierGetCapacity(IToolStackView tool, ModifierEntry modifier) {
        return 0;
    }

    protected void modifierSetAmount(IToolStackView tool, ModifierEntry modifier, int amount) {
    }

    protected void modifierAddAmount(IToolStackView tool, ModifierEntry modifier, int amount) {
        setAmount(tool, modifier, getAmount(tool) + amount);
    }

    protected void modifierRemoveAmount(IToolStackView tool, ModifierEntry modifier, int amount) {
        setAmount(tool, modifier, getAmount(tool) - amount);
    }

    protected void modifierAfterHarvest(IToolStackView tool, ModifierEntry modifier, UseOnContext context, ServerLevel world, BlockState state, BlockPos pos) {
    }

    protected void modifierAfterShearEntity(IToolStackView tool, ModifierEntry modifier, Player player, Entity entity, boolean isSheared) {
    }

    protected Vec3 modifierModifySlingAngle(IToolStackView tool, ModifierEntry modifier, LivingEntity player, LivingEntity target, ModifierEntry sling, float velocity, float power, Vec3 angle) {
        return angle;
    }

    protected float modifierModifySlingForce(IToolStackView tool, ModifierEntry modifier, LivingEntity player, LivingEntity target, ModifierEntry sling, float velocity, float power) {
        return power;
    }

    protected void modifierAfterSlingLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity player, LivingEntity target, ModifierEntry sling, float velocity, float power, Vec3 angle) {
    }

    // TCDEX 自定义 hook
    protected void modifierOnKillLivingTarget(IToolStackView tool, net.minecraftforge.event.entity.living.LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
    }

    protected float modifierModifyBreakExplosion(IToolStackView tool, ModifierEntry modifier, LivingEntity target, org.tp.tcdex.element.ElementType shieldElement, float damage) {
        return damage;
    }

    protected void modifierOnShieldBreak(IToolStackView tool, ModifierEntry modifier, LivingEntity target, org.tp.tcdex.element.ElementType shieldElement, LivingEntity attacker) {
    }

    protected float modifierModifyAbsorbed(IToolStackView tool, ModifierEntry modifier, Player player, float damageAmount, float absorbed) {
        return absorbed;
    }

    protected float modifierModifyRegenRate(IToolStackView tool, ModifierEntry modifier, Player player, float rate) {
        return rate;
    }

    protected float modifierModifyBreakOverflow(IToolStackView tool, ModifierEntry modifier, Player player, net.minecraft.world.damagesource.DamageSource source, float overflow) {
        return overflow;
    }

    protected void modifierOnShieldBreak(IToolStackView tool, ModifierEntry modifier, Player player, net.minecraft.world.damagesource.DamageSource source, float overflow) {
    }

    protected float modifierModifyStateStacks(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType element, float stacks) {
        return stacks;
    }

    protected int modifierModifyStateDuration(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType element, int duration) {
        return duration;
    }

    protected float modifierModifyKeywordMultiplier(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType keyword, float multiplier) {
        return multiplier;
    }

    protected float modifierModifyKeywordDamage(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType keyword, float damage) {
        return damage;
    }

    protected float modifierModifyKeywordRadius(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType keyword, float radius) {
        return radius;
    }

    protected float modifierModifyKineticDamage(IToolStackView tool, ModifierEntry modifier, LivingEntity target, float amount) {
        return amount;
    }

    protected float modifierModifyKineticShieldEfficiency(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.element.ElementType shieldElement, float efficiency) {
        return efficiency;
    }

    protected float modifierModifyReactionDuration(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float duration) {
        return duration;
    }

    protected float modifierModifyReactionRadius(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float radius) {
        return radius;
    }

    protected float modifierModifyReactionIntensity(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float intensity) {
        return intensity;
    }

    protected int modifierModifyReactionCooldown(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, int cooldown) {
        return cooldown;
    }

    protected float modifierModifyReactionAuraCost(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float auraCost) {
        return auraCost;
    }

    protected float modifierModifyReactionDamage(IToolStackView tool, ModifierEntry modifier, org.tp.tcdex.reaction.ElementReaction reaction, float damage) {
        return damage;
    }

    protected void modifierOnReactionTriggered(IToolStackView tool, ModifierEntry modifier, LivingEntity target,
                                               org.tp.tcdex.reaction.ElementReaction reaction, @javax.annotation.Nullable LivingEntity source, float finalIntensity) {
    }
}
