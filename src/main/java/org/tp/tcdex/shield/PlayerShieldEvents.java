package org.tp.tcdex.shield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.network.PacketHandler;
import org.tp.tcdex.network.PlayerStateSyncPacket;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家护盾事件：受击扣盾 / 攻击记录 / 每 tick 回复 / 定期同步客户端。
 *
 * <p>跳过 TCDEX 类型伤害（元素/动能/纯粹）——元素转化会 cancel 原伤害并 rehurt，
 * 避免护盾对同一击重复吸收。</p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerShieldEvents {

    /** 状态变化待同步的玩家（每 10 tick 统一发包） */
    private static final Set<UUID> DIRTY = new HashSet<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        // 跳过 TCDEX 类型伤害（防元素转化 rehurt 重复吸收）
        if (ModDamageSources.isElementDamage(event.getSource())
                || event.getSource().is(ModDamageSources.KINETIC_DAMAGE_TYPE)
                || event.getSource().is(ModDamageSources.PURE_DAMAGE_TYPE)) {
            return;
        }

        long now = event.getEntity().level().getGameTime();

        // 攻击者记录（玩家攻击重置脱战计时）
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof Player attacker) {
            PlayerShieldManager.markAttack(attacker, now);
        }

        // 玩家受击：重置脱战计时并优先扣护盾
        if (event.getEntity() instanceof Player target) {
            PlayerShieldManager.markHurt(target, now);
            if (PlayerShieldManager.isEnabled() && PlayerShieldManager.getShield(target) > 0) {
                float overflow = PlayerShieldManager.absorbDamage(target, event.getAmount());
                if (overflow <= 0) {
                    event.setCanceled(true); // 护盾全吸收
                } else {
                    event.setAmount(overflow); // 溢出部分继续结算到生命
                }
                DIRTY.add(target.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        long now = player.level().getGameTime();

        // 脱战自动回复护盾
        PlayerShieldManager.tickRegen(player, now);
        if (PlayerShieldManager.getShield(player) < PlayerShieldManager.getMaxShield(player)) {
            DIRTY.add(player.getUUID());
        }
        // HUD 状态存在即同步（急切 buff/冷却、万般皆允、元素状态）
        if (hasHudStates(player, now)) {
            DIRTY.add(player.getUUID());
        }

        // 每 10 tick 向客户端同步一次（状态变化时）
        if (now % 10 == 0 && DIRTY.remove(player.getUUID())) {
            syncState(player);
        }
    }

    /** 是否有需要 HUD 显示的状态 */
    private static boolean hasHudStates(Player player, long now) {
        CompoundTag data = player.getPersistentData();
        if (data.getLong("tcdex_eager_buff_until") > now || data.getLong("tcdex_eager_cooldown_until") > now) {
            return true;
        }
        if (!IElementalEntity.of(player).getAllElementStates().isEmpty()) {
            return true;
        }
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof IModifiable) {
            slimeknights.tconstruct.library.tools.nbt.ModDataNBT toolData = ToolStack.from(mainHand).getPersistentData();
            if (!toolData.getString(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "ap_mode")).isEmpty()
                    || toolData.getInt(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "ap_sin_timer")) > 0
                    || toolData.getInt(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "ap_combo")) > 0) {
                return true;
            }
        }
        return false;
    }

    /** 收集全部 HUD 状态并发给客户端 */
    private static void syncState(Player player) {
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();

        // 急切刀锋（玩家 persistentData）
        int eagerBuff = (int) Math.max(0, data.getLong("tcdex_eager_buff_until") - now);
        int eagerCooldown = (int) Math.max(0, data.getLong("tcdex_eager_cooldown_until") - now);

        // 万般皆允（主手工具持久 NBT）
        byte apMode = 0;
        float apForbidden = 0;
        int apSin = 0;
        int apCombo = 0;
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof IModifiable) {
            slimeknights.tconstruct.library.tools.nbt.ModDataNBT toolData = ToolStack.from(mainHand).getPersistentData();
            String mode = toolData.getString(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "ap_mode"));
            if ("yun".equals(mode)) {
                apMode = 2;
            } else if ("xu".equals(mode)) {
                apMode = 1;
            }
            apForbidden = toolData.getFloat(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "ap_forbidden"));
            apSin = toolData.getInt(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "ap_sin_timer"));
            apCombo = toolData.getInt(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "ap_combo"));
        }

        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                new PlayerStateSyncPacket(
                        PlayerShieldManager.getShield(player),
                        PlayerShieldManager.getMaxShield(player),
                        eagerBuff, eagerCooldown,
                        apMode, apForbidden, apSin, apCombo,
                        IElementalEntity.of(player).getAllElementStates()));
    }
}
