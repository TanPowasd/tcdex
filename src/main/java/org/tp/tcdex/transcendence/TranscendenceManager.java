package org.tp.tcdex.transcendence;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;

/**
 * 超越（Transcendence）管理器——命运2 Prismatic 子职业核心机制的玩家基础实现。
 *
 * <p><b>不是词条</b>：光/暗双能量槽是<b>所有玩家的基础能力</b>（命运2 语义：
 * Prismatic 是子职业，不依赖武器词条）。任何玩家的攻击/击杀都会积累能量，
 * 双槽均满后手动激活爆发（默认按键 V，客户端按键包服务端激活）。</p>
 *
 * <p>能量规则：
 * <ul>
 *   <li>光系攻击（烈日/电弧/虚空）→ 光之能量；暗系攻击（冰影/缚丝）→ 暗之能量</li>
 *   <li>棱镜攻击（棱镜共鸣）→ 双槽各半（光暗融合）；动能/五项之力 → 双槽少量</li>
 *   <li>命中 +2（棱镜双各 +2，动能双各 +1）；击杀 +8（棱镜双各 +4，动能双各 +2），槽上限 100</li>
 * </ul>
 * <b>激活（Transcendence 爆发 10 秒）</b>：近战/远程伤害 +30%、元素状态施加层数 ×2；
 * 激活后双槽清零重新积累。</p>
 */
public final class TranscendenceManager {

    // ===== 玩家 persistentData Key（CompoundTag，String 键） =====

    /** 光之能量（0-100） */
    public static final String LIGHT_KEY = "tcdex_trans_light";
    /** 暗之能量（0-100） */
    public static final String DARK_KEY = "tcdex_trans_dark";
    /** 超越激活到期 gameTime（0 = 未激活） */
    public static final String ACTIVE_UNTIL_KEY = "tcdex_trans_active_until";

    // ===== 数值 =====

    /** 能量槽上限 */
    public static final float MAX_ENERGY = 100.0f;
    /** 命中：光/暗系元素能量获取 */
    public static final float HIT_GAIN = 2.0f;
    /** 命中：棱镜（双槽各） */
    public static final float HIT_GAIN_PRISM = 2.0f;
    /** 命中：动能/五项之力（双槽各） */
    public static final float HIT_GAIN_OTHER = 1.0f;
    /** 击杀：光/暗系元素能量获取 */
    public static final float KILL_GAIN = 8.0f;
    /** 击杀：棱镜（双槽各） */
    public static final float KILL_GAIN_PRISM = 4.0f;
    /** 击杀：动能/五项之力（双槽各） */
    public static final float KILL_GAIN_OTHER = 2.0f;
    /** 超越激活时长（tick，200 = 10 秒） */
    public static final int ACTIVE_DURATION = 200;
    /** 超越期间伤害倍率 */
    public static final float DAMAGE_MULTIPLIER = 1.3f;
    /** 超越期间元素状态层数倍率 */
    public static final float STATE_MULTIPLIER = 2.0f;

    private TranscendenceManager() {
    }

    // ===== 双槽读写 =====

    /** 光之能量（0-100） */
    public static float getLightEnergy(Player player) {
        return player.getPersistentData().getFloat(LIGHT_KEY);
    }

    /** 暗之能量（0-100） */
    public static float getDarkEnergy(Player player) {
        return player.getPersistentData().getFloat(DARK_KEY);
    }

    /** 超越是否激活中 */
    public static boolean isActive(Player player, long now) {
        return player.getPersistentData().getLong(ACTIVE_UNTIL_KEY) > now;
    }

    /** 超越剩余 tick */
    public static int getActiveTicks(Player player, long now) {
        return (int) Math.max(0, player.getPersistentData().getLong(ACTIVE_UNTIL_KEY) - now);
    }

    /** 双槽是否已满（可激活） */
    public static boolean isReady(Player player) {
        return getLightEnergy(player) >= MAX_ENERGY && getDarkEnergy(player) >= MAX_ENERGY;
    }

    // ===== 能量积累 =====

    /**
     * 按攻击元素积累能量：光系（烈日/电弧/虚空）→ 光槽；暗系（冰影/缚丝）→ 暗槽；
     * 棱镜 → 双槽各半；动能/五项之力（元素不定）→ 双槽少量。钳制到上限。
     *
     * @param attackElement 本次攻击的元素（null = 动能/五项之力）
     * @param elementGain   光/暗系单槽获取量
     * @param prismGain     棱镜双槽各获取量
     * @param otherGain     动能双槽各获取量
     */
    public static void gainEnergy(Player player, ElementType attackElement,
                                  float elementGain, float prismGain, float otherGain) {
        if (attackElement == ElementType.PRISM) {
            addEnergy(player, LIGHT_KEY, prismGain);
            addEnergy(player, DARK_KEY, prismGain);
        } else if (attackElement != null) {
            boolean light = attackElement == ElementType.SOLAR || attackElement == ElementType.ARC || attackElement == ElementType.VOID;
            addEnergy(player, light ? LIGHT_KEY : DARK_KEY, elementGain);
        } else {
            // 动能 / 五项之力（每次随机元素，事件侧无法判定 → 视为光暗混合，少量双加）
            addEnergy(player, LIGHT_KEY, otherGain);
            addEnergy(player, DARK_KEY, otherGain);
        }
    }

    private static void addEnergy(Player player, String key, float amount) {
        if (amount <= 0) {
            return;
        }
        player.getPersistentData().putFloat(key, Math.min(MAX_ENERGY, player.getPersistentData().getFloat(key) + amount));
    }

    // ===== 激活 =====

    /**
     * 尝试激活 Transcendence：双槽均满且未激活 → 激活爆发（双槽清零 + 演出），返回 true。
     * 服务端调用（按键包/指令）。
     */
    public static boolean tryActivate(Player player, long now) {
        if (isActive(player, now)) {
            return false; // 激活中不重复触发
        }
        if (!isReady(player)) {
            return false; // 双槽未满
        }
        player.getPersistentData().putLong(ACTIVE_UNTIL_KEY, now + ACTIVE_DURATION);
        player.getPersistentData().putFloat(LIGHT_KEY, 0.0f);
        player.getPersistentData().putFloat(DARK_KEY, 0.0f);

        // 演出：光暗双色粒子 + 激活音效
        net.minecraft.world.level.Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.2F);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW, player.getX(), player.getY() + 1.5, player.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.5, player.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
        }
        return true;
    }

    // ===== 爆发增强（全局生效，近战/远程统一） =====

    /** 超越激活时伤害 ×1.3（作用于玩家造成的所有伤害，含破盾磨损） */
    public static float applyDamageMultiplier(LivingEntity attacker, float damage) {
        if (attacker instanceof Player player && isActive(player, attacker.level().getGameTime())) {
            return damage * DAMAGE_MULTIPLIER;
        }
        return damage;
    }

    /** 超越激活时元素状态层数 ×2（"双元素能量回复"→ 关键词积累加速） */
    public static float applyStateMultiplier(LivingEntity attacker, float stacks) {
        if (attacker instanceof Player player && isActive(player, attacker.level().getGameTime())) {
            return stacks * STATE_MULTIPLIER;
        }
        return stacks;
    }
}
