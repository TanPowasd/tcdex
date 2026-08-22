package org.tp.tcdex.player.reaction;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;

import javax.annotation.Nullable;

/**
 * 玩家反应词条 Capability 注册与存储。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerReactionModifiersCapability {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "reaction_modifiers");

    public static final Capability<IPlayerReactionModifiers> PLAYER_REACTION_MODIFIERS =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private PlayerReactionModifiersCapability() {
    }

    public static LazyOptional<IPlayerReactionModifiers> get(Player player) {
        return player.getCapability(PLAYER_REACTION_MODIFIERS);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new Provider());
        }
    }

    private static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final PlayerReactionModifiers instance = new PlayerReactionModifiers();
        private final LazyOptional<IPlayerReactionModifiers> optional = LazyOptional.of(() -> instance);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER_REACTION_MODIFIERS ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (String id : instance.getDisabledModifiers()) {
                list.add(StringTag.valueOf(id));
            }
            tag.put("modifiers", list);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.clear();
            ListTag list = nbt.getList("modifiers", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                instance.addModifier(list.getString(i));
            }
        }
    }
}
