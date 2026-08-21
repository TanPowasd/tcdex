package org.tp.tcdex.artifact;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

/**
 * 圣遗物物品：可装备到 Curios 的 5 个圣遗物槽位。
 *
 * <p>属性以 NBT 存储，第一版支持生命/攻击/防御/精通/充能/暴击/元素伤害/护盾/治疗/光等。</p>
 */
public class ArtifactItem extends Item implements ICurioItem {

    private final ArtifactSlot slot;

    public ArtifactItem(ArtifactSlot slot) {
        super(new Item.Properties().stacksTo(1));
        this.slot = slot;
    }

    public ArtifactSlot getSlot() {
        return slot;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slot.getId().equals(slotContext.identifier());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        CompoundTag tag = stack.getOrCreateTag();
        UUID modUuid = UUID.nameUUIDFromBytes(("tcdex_artifact_" + slot.getId()).getBytes());

        addAttr(map, Attributes.MAX_HEALTH, tag, ArtifactStat.MAX_HEALTH, modUuid);
        addAttr(map, Attributes.ATTACK_DAMAGE, tag, ArtifactStat.ATTACK_DAMAGE, modUuid);
        addAttr(map, Attributes.ARMOR, tag, ArtifactStat.ARMOR, modUuid);
        return map;
    }

    private static void addAttr(Multimap<Attribute, AttributeModifier> map, Attribute attribute,
                                CompoundTag tag, ArtifactStat stat, UUID uuid) {
        float value = getStat(tag, stat);
        if (value != 0) {
            map.put(attribute, new AttributeModifier(uuid, "tcdex_artifact_" + stat.getId(),
                    value, AttributeModifier.Operation.ADDITION));
        }
    }

    // ===== NBT 工具 =====

    public static void setStat(ItemStack stack, ArtifactStat stat, float value) {
        stack.getOrCreateTag().putFloat(stat.getId(), value);
    }

    public static float getStat(ItemStack stack, ArtifactStat stat) {
        return getStat(stack.getOrCreateTag(), stat);
    }

    public static float getStat(CompoundTag tag, ArtifactStat stat) {
        return tag.getFloat(stat.getId());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tcdex.artifact.slot", Component.translatable("item.tcdex.artifact.slot." + slot.getId())));
        for (ArtifactStat stat : ArtifactStat.values()) {
            float value = getStat(stack, stat);
            if (value != 0) {
                tooltip.add(Component.literal("  " + stat.getId() + ": " + value));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
