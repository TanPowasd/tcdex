package org.tp.tcdex.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.echo.ElementalEchoManager;
import org.tp.tcdex.energy.ElementEnergyManager;
import org.tp.tcdex.transcendence.TranscendenceManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.light.LightLevelManager;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.network.ChainStateSyncPacket;
import org.tp.tcdex.network.PacketHandler;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 连携链核心管理器。
 *
 * <p>负责把元素行为折算成连携贡献、更新主链/焦点链、扫描附近群体连携，
 * 并处理连携引爆、元素风暴、原命崩解、增益和衰减。</p>
 */
public final class ChainManager {

    /** 连携链持续时间（tick），15 秒 */
    public static final int CHAIN_LIFETIME_TICKS = 300;

    /** 群体连携扫描半径 */
    public static final float GROUP_SCAN_RADIUS = 8.0f;

    /** 每个附近不同元素残响贡献的连携层数 */
    public static final float GROUP_DISTINCT_CONTRIBUTION = 0.25f;

    /** 连携引爆半径 */
    public static final float DETONATE_RADIUS = 4.0f;

    /** 连携引爆基础伤害 */
    public static final float DETONATE_BASE_DAMAGE = 5.0f;

    /** 连携总贡献转伤害系数 */
    public static final float DETONATE_CONTRIBUTION_FACTOR = 0.4f;

    /** 原命崩解半径 */
    public static final float CATACLYSM_RADIUS = 8.0f;

    /** 原命崩解基础伤害 */
    public static final float CATACLYSM_BASE_DAMAGE = 6.0f;

    /** 连携引爆后增益时长（tick，10 秒） */
    public static final int CHAIN_BUFF_DURATION = 200;

    /** 连携引爆后的最小冷却（tick） */
    public static final int DETONATE_COOLDOWN = 80;

    /** 扩散时施加的元素状态层数 */
    public static final float DIFFUSION_STACKS = 1.0f;

    /** 扩散时施加的元素状态时长（tick） */
    public static final int DIFFUSION_DURATION = 100;

    private ChainManager() {
    }

    /**
     * 处理一次玩家元素行为。
     *
     * @param player    玩家
     * @param element   元素
     * @param actionType 行为类型
     * @param target    可选：当前攻击目标，用于焦点链
     */
    public static void onElementAction(@Nullable Player player, @Nullable ElementType element,
                                       @Nullable ElementActionType actionType,
                                       @Nullable LivingEntity target) {
        if (player == null || element == null || actionType == null || player.level().isClientSide) {
            return;
        }
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        if (data == null) {
            return;
        }

        long now = player.level().getGameTime();
        float weight = actionType.getWeight();
        data.addElement(element, now, weight);

        if (target != null && target != player && target.isAlive()) {
            data.setFocusTargetEntityId(target.getId());
            data.addFocusElement(element, now, weight);
        }

        addGroupContribution(player, data);
    }

    /**
     * 尝试引爆玩家当前连携链。
     *
     * <p>1-3 种元素触发普通连携引爆；4-5 种额外生成元素风暴；
     * 6 种以上触发原命崩解。</p>
     *
     * @return 是否成功引爆
     */
    public static boolean tryDetonate(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        if (data == null || !data.isChainActive() || data.getDetonateCooldown() > 0) {
            return false;
        }

        List<ChainEntry> chain = new ArrayList<>(data.getMainChain());
        if (chain.isEmpty()) {
            return false;
        }

        List<ElementType> elements = new ArrayList<>();
        float totalContribution = 0.0f;
        for (ChainEntry entry : chain) {
            totalContribution += entry.contribution();
            if (!elements.contains(entry.element())) {
                elements.add(entry.element());
            }
        }
        if (elements.isEmpty()) {
            return false;
        }

        Level level = player.level();
        Vec3 center = player.position();
        if (data.getFocusTargetEntityId() >= 0) {
            var focus = level.getEntity(data.getFocusTargetEntityId());
            if (focus instanceof LivingEntity living && living.isAlive()) {
                center = living.position();
            }
        }

        float fusionMultiplier = getFusionMultiplier(player);
        float totalDamage = (DETONATE_BASE_DAMAGE + totalContribution * DETONATE_CONTRIBUTION_FACTOR) * fusionMultiplier;

        if (elements.size() >= 6) {
            applyCataclysm(level, center, elements);
        } else {
            applyDetonation(level, center, elements, DETONATE_RADIUS, totalDamage);
            if (elements.size() >= 4) {
                ChainAreaManager.spawnStorm(level, center, elements);
            }
        }
        ChainComboEffects.apply(level, center, elements, player, false);
        grantFusionResources(player, elements);

        // 清空资源、施加增益、进入自然冷却
        data.clearMainChain();
        data.clearFocusChain();
        data.setGroupOverflow(0.0f);
        data.setChainBuffTicks(CHAIN_BUFF_DURATION);
        data.setDetonateCooldown(DETONATE_COOLDOWN);
        return true;
    }

    /** 普通连携引爆：混合伤害 + 元素扩散 */
    private static void applyDetonation(Level level, Vec3 center, List<ElementType> elements, float radius, float totalDamage) {
        float perElementDamage = Math.max(1.0f, totalDamage / elements.size());
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof Player))) {
            for (ElementType element : elements) {
                target.hurt(target.damageSources().magic(), perElementDamage);
                IElementalEntity.of(target).addElementState(element, DIFFUSION_STACKS, DIFFUSION_DURATION);
            }
        }
        playDetonateFeedback(level, center, elements, false);
    }

    /** 原命崩解：大范围全元素爆发 + 清除残响 + 生成原命裂隙 */
    private static void applyCataclysm(Level level, Vec3 center, List<ElementType> elements) {
        AABB box = new AABB(center, center).inflate(CATACLYSM_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof Player))) {
            for (ElementType element : elements) {
                target.hurt(target.damageSources().magic(), CATACLYSM_BASE_DAMAGE);
                IElementalEntity.of(target).addElementState(element, DIFFUSION_STACKS, DIFFUSION_DURATION);
            }
        }
        ElementalEchoManager.clearNear(level, BlockPos.containing(center), CATACLYSM_RADIUS);
        ChainAreaManager.spawnRift(level, center, elements);
        playDetonateFeedback(level, center, elements, true);
    }

    private static void playDetonateFeedback(Level level, Vec3 center, List<ElementType> elements, boolean cataclysm) {
        level.playSound(null, center.x, center.y, center.z,
                cataclysm ? SoundEvents.WITHER_BREAK_BLOCK : SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.9F, cataclysm ? 0.7F : 1.3F);
        if (level instanceof ServerLevel serverLevel) {
            for (ElementType element : elements) {
                serverLevel.sendParticles(element.getParticle(),
                        center.x, center.y + 1.0, center.z,
                        cataclysm ? 30 : 12, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    /** 扫描附近敌人身上的不同元素残响，生成群体连携贡献 */
    private static void addGroupContribution(Player player, IPlayerChainData data) {
        Level level = player.level();
        Set<ElementType> nearbyElements = new HashSet<>();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(GROUP_SCAN_RADIUS),
                e -> e != player && e.isAlive())) {
            for (ElementType element : IElementalEntity.of(living).getAllElementStates().keySet()) {
                if (element != null) {
                    nearbyElements.add(element);
                }
            }
        }
        if (!nearbyElements.isEmpty()) {
            data.addGroupOverflow(nearbyElements.size() * GROUP_DISTINCT_CONTRIBUTION);
        }
    }

   
    /** 计算连携/终结与元素能量、超越融合后的伤害倍率 */
    private static float getFusionMultiplier(Player player) {
        float multiplier = 1.0f;
        if (ElementEnergyManager.getEnergy(player) >= ElementEnergyManager.MAX_ENERGY) {
            multiplier += 0.25f;
        }
        if (TranscendenceManager.isReady(player) || TranscendenceManager.isActive(player, player.level().getGameTime())) {
            multiplier += 0.25f;
        }
        return multiplier;
    }

    /** 连携引爆/终结后反哺元素能量与超越能量 */
    private static void grantFusionResources(Player player, List<ElementType> elements) {
        ElementEnergyManager.addEnergy(player, 5.0f);
        ElementType representative = elements.isEmpty() ? null : elements.get(0);
        TranscendenceManager.gainEnergy(player, representative, 2.0f, 2.0f, 1.0f);
    }

    /** 处理客户端发来的连携按键动作 */
    public static boolean handleAction(Player player, ChainActionType action) {
        if (player.level().isClientSide) {
            return false;
        }
        return switch (action) {
            case DETONATE -> tryDetonate(player);
            case FINISHER -> {
                IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
                LivingEntity target = null;
                if (data != null && data.getFocusTargetEntityId() >= 0) {
                    var focus = player.level().getEntity(data.getFocusTargetEntityId());
                    if (focus instanceof LivingEntity living) {
                        target = living;
                    }
                }
                yield target != null && tryFinisher(player, target);
            }
            case SMART -> {
                IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
                LivingEntity target = null;
                if (data != null && data.getFocusTargetEntityId() >= 0) {
                    var focus = player.level().getEntity(data.getFocusTargetEntityId());
                    if (focus instanceof LivingEntity living) {
                        target = living;
                    }
                }
                if (target != null && IElementalEntity.of(target).isBroken()) {
                    yield tryFinisher(player, target);
                }
                yield tryDetonate(player);
            }
        };
    }

    /**
     * 尝试对破绽目标发动命定终结技。
     *
     * <p>要求目标处于破绽状态；根据连携中不同元素数量决定普通终结、
     * 元素风暴或原命崩解，并清空连携资源。</p>
     */
    public static boolean tryFinisher(Player player, LivingEntity target) {
        if (player.level().isClientSide || target == null || target instanceof Player || !target.isAlive()) {
            return false;
        }
        if (!IElementalEntity.of(target).isBroken()) {
            return false;
        }
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        if (data == null || !data.isChainActive() || data.getDetonateCooldown() > 0) {
            return false;
        }

        List<ChainEntry> chain = new ArrayList<>(data.getMainChain());
        List<ElementType> elements = new ArrayList<>();
        float totalContribution = 0.0f;
        for (ChainEntry entry : chain) {
            totalContribution += entry.contribution();
            if (!elements.contains(entry.element())) {
                elements.add(entry.element());
            }
        }
        if (elements.isEmpty()) {
            return false;
        }

        Level level = player.level();
        Vec3 center = target.position();
        float finisherDamage = (DETONATE_BASE_DAMAGE * 2.0f + totalContribution * DETONATE_CONTRIBUTION_FACTOR)
                * getFusionMultiplier(player);
        finisherDamage *= LightLevelManager.getDealtDamageMultiplier(
                LightLevelManager.getPlayerAttackLightLevel(player),
                LightLevelManager.getMonsterLightLevel(target));

        // 以破绽目标为核心造成终结伤害与范围效果
        applyDetonation(level, center, elements, DETONATE_RADIUS + 1.0f, finisherDamage);
        if (elements.size() >= 6) {
            applyCataclysm(level, center, elements);
        } else if (elements.size() >= 4) {
            ChainAreaManager.spawnStorm(level, center, elements);
        }
        ChainComboEffects.apply(level, center, elements, player, true);

        // 终结后消耗破绽窗口
        IElementalEntity.of(target).setBreakTicks(0);
        IElementalEntity.of(target).resetImbalance();

        data.clearMainChain();
        data.clearFocusChain();
        data.setGroupOverflow(0.0f);
        data.setChainBuffTicks(CHAIN_BUFF_DURATION);
        data.setDetonateCooldown(DETONATE_COOLDOWN);
        return true;
    }

    /** 每 tick 维护玩家连携状态 */
    public static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        if (data == null) {
            return;
        }
        long now = player.level().getGameTime();
        data.removeExpired(now, CHAIN_LIFETIME_TICKS);
        data.tickCooldown();
        data.tickBuff();

        // 每 10 tick 同步一次连携状态给客户端 HUD
        if (now % 10 == 0 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new ChainStateSyncPacket(
                            data.getMainChain(),
                            data.getFocusChain(),
                            data.getGroupOverflow(),
                            data.getDetonateCooldown(),
                            data.getChainBuffTicks()));
        }
    }
}
