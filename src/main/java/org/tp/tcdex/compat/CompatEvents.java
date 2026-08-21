package org.tp.tcdex.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.energy.ElementEnergyManager;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ElementReactionEvents;

/**
 * 铁魔法（irons_spellbooks）软联动：法术命中元素化。
 *
 * <p>铁魔法法术弹射物命中目标时，按弹射物实体 id 关键词映射为 TCDEX 元素，
 * 对目标（玩家/生物均可）施加对应元素状态（层数按怪物系数缩放，标记型保底 1 层）——
 * 灼烧/减速冻结/Jolt/Weaken 等关键词联动自动生效。</p>
 *
 * <p>仅按实体注册名 namespace + 关键词匹配，不引用铁魔法任何类；未安装时自然不生效。</p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CompatEvents {

    /** 铁魔法是否加载（类加载时快照，事件内不再重复查询） */
    private static final boolean IRONS_LOADED = ModList.get().isLoaded("irons_spellbooks");

    /** 层数缩放（同怪物元素攻击，保守） */
    private static final float SPELL_STACK_SCALE = 0.4f;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onSpellHit(LivingHurtEvent event) {
        if (!IRONS_LOADED || event.isCanceled() || event.getEntity().level().isClientSide) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null) {
            return;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
        if (key == null || !"irons_spellbooks".equals(key.getNamespace())) {
            return;
        }
        ElementType element = matchSpellElement(key.getPath());
        if (element == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        // 先尝试元素反应，再施加本次法术元素附着
        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity source = sourceEntity instanceof LivingEntity living ? living : null;
        ElementReactionEvents.tryTriggerReaction(target, element, source);

        float stacks = Math.max(1.0f, element.getStacksPerHit() * SPELL_STACK_SCALE);
        IElementalEntity.of(target).addElementState(element, stacks, element.getStateDuration());

        // 玩家受到元素伤害时获得少量元素能量
        if (target instanceof Player player) {
            ElementEnergyManager.onPlayerDamagedByElement(player, element);
        }
    }

    /** 法术实体 id → TCDEX 元素（关键词子串匹配，铁魔法各版本 id 差异鲁棒） */
    private static ElementType matchSpellElement(String path) {
        String p = path.toLowerCase();
        if (p.contains("fire") || p.contains("flame") || p.contains("burn") || p.contains("magma")) {
            return ElementType.SOLAR;
        }
        if (p.contains("ice") || p.contains("frost") || p.contains("snow")) {
            return ElementType.STASIS;
        }
        if (p.contains("lightning") || p.contains("electro") || p.contains("volt")) {
            return ElementType.ARC;
        }
        if (p.contains("void") || p.contains("evocation") || p.contains("shadow") || p.contains("wither")) {
            return ElementType.VOID;
        }
        return null;
    }
}
