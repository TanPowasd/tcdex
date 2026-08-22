package org.tp.tcdex.chain;

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
 * 玩家连携数据 Capability。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerChainCapability {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "player_chain");

    public static final Capability<IPlayerChainData> PLAYER_CHAIN =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private PlayerChainCapability() {
    }

    public static LazyOptional<IPlayerChainData> get(Player player) {
        return player.getCapability(PLAYER_CHAIN);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new Provider());
        }
    }

    private static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final PlayerChainData instance = new PlayerChainData();
        private final LazyOptional<IPlayerChainData> optional = LazyOptional.of(() -> instance);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER_CHAIN ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (ChainEntry entry : instance.getMainChain()) {
                list.add(StringTag.valueOf(entry.element().getId() + ":" + entry.lastUsedTime() + ":" + entry.contribution()));
            }
            tag.put("mainChain", list);
            tag.putInt("focusTarget", instance.getFocusTargetEntityId());
            tag.putFloat("groupOverflow", instance.getGroupOverflow());
            tag.putInt("detonateCooldown", instance.getDetonateCooldown());
            tag.putInt("chainBuffTicks", instance.getChainBuffTicks());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.clearAll();
            ListTag list = nbt.getList("mainChain", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getString(i);
                String[] parts = raw.split(":");
                if (parts.length >= 3) {
                    org.tp.tcdex.element.ElementType element = org.tp.tcdex.element.ElementManager.parseElement(parts[0]);
                    long time = Long.parseLong(parts[1]);
                    float contribution = Float.parseFloat(parts[2]);
                    if (element != null) {
                        instance.addElement(element, time, contribution);
                    }
                }
            }
            instance.setFocusTargetEntityId(nbt.getInt("focusTarget"));
            instance.setGroupOverflow(nbt.getFloat("groupOverflow"));
            instance.setDetonateCooldown(nbt.getInt("detonateCooldown"));
            instance.setChainBuffTicks(nbt.getInt("chainBuffTicks"));
        }
    }
}
