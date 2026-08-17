package org.tp.tcdex.light;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import org.tp.tcdex.ModItems;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.TcdexF;
import org.tp.tcdex.debug.TcdexDebug;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.ElementStatus;
import org.tp.tcdex.modifier.elemental.ElementalModifier;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LightLevelEvents {

    /** 怪物生成时附加基础光等 */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (entity instanceof LivingEntity living && LightLevelManager.isMonster(living)) {
            var data = living.getPersistentData();
            if (!data.contains(LightLevelManager.MONSTER_BASE_LIGHT_TAG)) {
                data.putInt(LightLevelManager.MONSTER_BASE_LIGHT_TAG,
                        LightLevelManager.rollMonsterSpawnLight(event.getLevel(), living.blockPosition(), living));
            }
        }
    }

    /** 根据玩家/怪物光等差值修正最终伤害 */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        // 怪物攻击玩家
        if (target instanceof Player targetPlayer && !(attacker instanceof Player)) {
            int playerArmorLight = LightLevelManager.getPlayerArmorLightLevel(targetPlayer);
            int monsterLight = LightLevelManager.getMonsterLightLevel(attacker);
            float multiplier = LightLevelManager.getTakenDamageMultiplier(monsterLight, playerArmorLight);
            float originalDamage = event.getAmount();
            event.setAmount(originalDamage * multiplier);
            if (LightLevelManager.isDebugEnabled()) {
                targetPlayer.sendSystemMessage(Component.literal("[光等调试] 怪物攻击玩家"));
                targetPlayer.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "攻击者: %s | 目标: %s", attacker.getDisplayName().getString(), targetPlayer.getDisplayName().getString())));
                targetPlayer.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "怪物光等: %d | 玩家护甲光等: %d | 玩家防御光等: %d",
                        monsterLight, playerArmorLight, playerArmorLight)));
                targetPlayer.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "伤害修正: %.2f | 原始伤害: %.2f | 实际伤害: %.2f",
                        multiplier, originalDamage, event.getAmount())));
            }
            return;
        }

        // 玩家攻击怪物
        if (attacker instanceof Player attackerPlayer && !(target instanceof Player)) {
            int playerArmorLight = LightLevelManager.getPlayerArmorLightLevel(attackerPlayer);
            int playerWeaponLight = LightLevelManager.getPlayerWeaponLightLevel(attackerPlayer);
            int playerAttackLight = LightLevelManager.getPlayerAttackLightLevel(attackerPlayer);
            int monsterLight = LightLevelManager.getMonsterLightLevel(target);
            float multiplier = LightLevelManager.getDealtDamageMultiplier(playerAttackLight, monsterLight);
            float originalDamage = event.getAmount();
            event.setAmount(originalDamage * multiplier);
            if (LightLevelManager.isDebugEnabled()) {
                attackerPlayer.sendSystemMessage(Component.literal("[光等调试] 玩家攻击怪物"));
                attackerPlayer.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "攻击者: %s | 目标: %s", attackerPlayer.getDisplayName().getString(), target.getDisplayName().getString())));
                attackerPlayer.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "护甲光等: %d | 武器光等: %d | 攻击光等: %d",
                        playerArmorLight, playerWeaponLight, playerAttackLight)));
                attackerPlayer.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "怪物光等: %d | 伤害修正: %.2f", monsterLight, multiplier)));
                attackerPlayer.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "原始伤害: %.2f | 实际伤害: %.2f", originalDamage, event.getAmount())));
            }
        }
    }

    /** 光之精华灌注：主手/副手分别为精华和匠魂装备时，消耗精华提升光等 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean mainIsEssence = mainHand.getItem() == ModItems.LIGHT_ESSENCE.get();
        boolean offIsEssence = offHand.getItem() == ModItems.LIGHT_ESSENCE.get();

        if (mainIsEssence && LightLevelManager.isTinkersToolOrArmor(offHand)) {
            LightLevelManager.addInfusionLevel(offHand, 1);
            mainHand.shrink(1);
            event.setCanceled(true);
            return;
        }

        if (offIsEssence && LightLevelManager.isTinkersToolOrArmor(mainHand)) {
            LightLevelManager.addInfusionLevel(mainHand, 1);
            offHand.shrink(1);
            event.setCanceled(true);
        }
    }

    /** 注册 /tcdex setlight 指令：强制设置主手匠魂装备的光等 */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tcdex")
                        .then(Commands.literal("setlight")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> {
                                            Player player = ctx.getSource().getPlayerOrException();
                                            ItemStack stack = player.getMainHandItem();
                                            if (!LightLevelManager.isTinkersToolOrArmor(stack)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.tcdex.setlight.not_tool"));
                                                return 0;
                                            }
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            LightLevelManager.setLightLevel(stack, value);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.tcdex.setlight.success", value), true);
                                            return 1;
                                        }))
                        )
                        .then(Commands.literal("setlooklight")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> {
                                            Player player = ctx.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            List<HitResult> hits = TcdexF.Radiographic_detection_GetEntity(player, 64);
                                            if (hits == null || hits.isEmpty() || !(hits.get(0) instanceof EntityHitResult entityHit)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.tcdex.setlooklight.no_target"));
                                                return 0;
                                            }
                                            Entity target = entityHit.getEntity();
                                            if (!(target instanceof LivingEntity living) || living instanceof Player) {
                                                ctx.getSource().sendFailure(Component.translatable("command.tcdex.setlooklight.not_living"));
                                                return 0;
                                            }
                                            LightLevelManager.setMonsterLightLevel(living, value);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.tcdex.setlooklight.success", target.getDisplayName(), value), true);
                                            return 1;
                                        }))
                        )
                        .then(Commands.literal("getlight")
                                .executes(ctx -> {
                                    Player player = ctx.getSource().getPlayerOrException();
                                    ItemStack mainHand = player.getMainHandItem();
                                    int mainLight = LightLevelManager.getLightLevel(mainHand);
                                    int armorLight = LightLevelManager.getPlayerArmorLightLevel(player);
                                    int weaponLight = LightLevelManager.getPlayerWeaponLightLevel(player);
                                    int attackLight = LightLevelManager.getPlayerAttackLightLevel(player);

                                    player.sendSystemMessage(Component.literal("[光等查询] 主手物品光等: " + mainLight));
                                    player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                                            "护甲光等: %d | 武器光等: %d | 攻击光等: %d",
                                            armorLight, weaponLight, attackLight)));

                                    List<HitResult> hits = TcdexF.Radiographic_detection_GetEntity(player, 64);
                                    if (hits != null && !hits.isEmpty() && hits.get(0) instanceof EntityHitResult entityHit
                                            && entityHit.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
                                        int entityLight = LightLevelManager.getMonsterLightLevel(living);
                                        player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                                                "目标生物: %s | 光等: %d",
                                                living.getDisplayName().getString(), entityLight)));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("debug")
                                .then(Commands.literal("on")
                                        .executes(ctx -> {
                                            LightLevelManager.setDebugEnabled(true);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.tcdex.debug.enabled"), true);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("off")
                                        .executes(ctx -> {
                                            LightLevelManager.setDebugEnabled(false);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.tcdex.debug.disabled"), true);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("element")
                                        .then(Commands.literal("on")
                                                .executes(ctx -> {
                                                    TcdexDebug.setElementalEnabled(true);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.tcdex.debug.element.enabled"), true);
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("off")
                                                .executes(ctx -> {
                                                    TcdexDebug.setElementalEnabled(false);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.tcdex.debug.element.disabled"), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("element")
                                .executes(ctx -> {
                                    Player player = ctx.getSource().getPlayerOrException();
                                    // 查看准星目标的护盾与元素状态
                                    List<HitResult> hits = TcdexF.Radiographic_detection_GetEntity(player, 64);
                                    if (hits == null || hits.isEmpty() || !(hits.get(0) instanceof EntityHitResult entityHit)
                                            || !(entityHit.getEntity() instanceof LivingEntity living) || living instanceof Player) {
                                        ctx.getSource().sendFailure(Component.translatable("command.tcdex.setlooklight.no_target"));
                                        return 0;
                                    }

                                    IElementalEntity data = IElementalEntity.of(living);
                                    // 护盾信息
                                    ElementType shield = data.getShieldElement();
                                    float shieldAmount = data.getShieldAmount();
                                    if (shield != null && shieldAmount > 0) {
                                        player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                                                "[元素] %s | 护盾: %s (%.1f / %.1f)",
                                                living.getDisplayName().getString(), shield.getId(), shieldAmount,
                                                living.getMaxHealth() * 0.5f)));
                                    } else {
                                        player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                                                "[元素] %s | 无护盾", living.getDisplayName().getString())));
                                    }
                                    // 元素状态
                                    Map<ElementType, ElementStatus> states = data.getAllElementStates();
                                    if (states.isEmpty()) {
                                        player.sendSystemMessage(Component.literal("  元素状态: 无"));
                                    } else {
                                        player.sendSystemMessage(Component.literal("  元素状态:"));
                                        for (Map.Entry<ElementType, ElementStatus> entry : states.entrySet()) {
                                            player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                                                    "    %s: 层数 %.0f | 剩余 %d tick",
                                                    entry.getKey().getId(), entry.getValue().stacks, entry.getValue().duration)));
                                        }
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("setelement")
                                .then(Commands.argument("element", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (ElementType type : ElementType.values()) {
                                                builder.suggest(type.getId());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            Player player = ctx.getSource().getPlayerOrException();
                                            String elementId = StringArgumentType.getString(ctx, "element");
                                            ElementType element = ElementalModifier.parseElement(elementId);
                                            if (element == null) {
                                                ctx.getSource().sendFailure(Component.translatable("command.tcdex.setelement.invalid", elementId));
                                                return 0;
                                            }
                                            ItemStack stack = player.getMainHandItem();
                                            if (stack.isEmpty() || !(stack.getItem() instanceof slimeknights.tconstruct.library.tools.item.IModifiable)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.tcdex.setlight.not_tool"));
                                                return 0;
                                            }
                                            slimeknights.tconstruct.library.tools.nbt.ToolStack tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
                                            boolean hasElemental = false;
                                            for (slimeknights.tconstruct.library.modifiers.ModifierEntry entry : tool.getModifierList()) {
                                                if (entry.getModifier() instanceof ElementalModifier) {
                                                    hasElemental = true;
                                                    break;
                                                }
                                            }
                                            if (!hasElemental) {
                                                ctx.getSource().sendFailure(Component.translatable("command.tcdex.setelement.no_modifier"));
                                                return 0;
                                            }
                                            // 指定并固化元素（覆盖原随机结果，之后不可再变）
                                            tool.getPersistentData().putString(ElementalModifier.ELEMENT_KEY, element.getId());
                                            tool.updateStack(stack);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.tcdex.setelement.success", element.getId()), true);
                                            return 1;
                                        })
                                )
                        )
        );
    }

    /** 客户端：匠魂物品 Tooltip 显示光等 */
    @Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onRenderHud(RenderGuiEvent.Post event) {
            if (!LightLevelManager.isHudEnabled()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) {
                return;
            }
            Player player = mc.player;
            int armorLight = LightLevelManager.getPlayerArmorLightLevel(player);
            int weaponLight = LightLevelManager.getPlayerWeaponLightLevel(player);
            int attackLight = LightLevelManager.getPlayerAttackLightLevel(player);

            Font font = mc.font;
            GuiGraphics graphics = event.getGuiGraphics();
            var window = event.getWindow();
            // 放在物品栏右侧，并缩小显示
            int x = window.getGuiScaledWidth() / 2 + 95;
            int y = window.getGuiScaledHeight() - 26;
            float scale = 0.6f;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.drawString(font, "[TCDEX 光等]", 0, 0, 0xFFFFFF);
            graphics.drawString(font, String.format(Locale.ROOT, "护甲: %d | 武器: %d", armorLight, weaponLight), 0, 10, 0xFFFFFF);
            graphics.drawString(font, String.format(Locale.ROOT, "攻击光等: %d", attackLight), 0, 20, 0xFFFFFF);
            graphics.pose().popPose();
        }

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (!LightLevelManager.isTinkersToolOrArmor(stack)) {
                return;
            }
            int light = LightLevelManager.getLightLevel(stack);
            event.getToolTip().add(Component.translatable("tooltip.tcdex.light_level", light));
        }

        @SubscribeEvent
        public static void onRenderNameTag(RenderNameTagEvent event) {
            Entity entity = event.getEntity();
            if (entity instanceof LivingEntity living && !(living instanceof Player) && LightLevelManager.isMonster(living)) {
                int light = LightLevelManager.getMonsterLightLevel(living);
                Component suffix = Component.literal(" [光等 " + light + "]");
                event.setContent(event.getContent().copy().append(suffix));
            }
        }
    }
}
