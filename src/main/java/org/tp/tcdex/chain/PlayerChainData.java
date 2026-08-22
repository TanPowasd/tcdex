package org.tp.tcdex.chain;

import org.tp.tcdex.element.ElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家连携数据默认实现。
 */
public class PlayerChainData implements IPlayerChainData {

    private final List<ChainEntry> mainChain = new ArrayList<>();
    private final List<ChainEntry> focusChain = new ArrayList<>();
    private int focusTargetEntityId = -1;
    private float groupOverflow = 0.0f;
    private int detonateCooldown = 0;
    private int chainBuffTicks = 0;

    @Override
    public List<ChainEntry> getMainChain() {
        return Collections.unmodifiableList(mainChain);
    }

    @Override
    public List<ChainEntry> getFocusChain() {
        return Collections.unmodifiableList(focusChain);
    }

    @Override
    public void addElement(ElementType element, long now, float contribution) {
        addToList(mainChain, element, now, contribution);
    }

    @Override
    public void addFocusElement(ElementType element, long now, float contribution) {
        addToList(focusChain, element, now, contribution);
    }

    private void addToList(List<ChainEntry> list, ElementType element, long now, float contribution) {
        if (element == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            ChainEntry entry = list.get(i);
            if (entry.element() == element) {
                list.set(i, new ChainEntry(element, now, entry.contribution() + contribution));
                return;
            }
        }
        list.add(new ChainEntry(element, now, contribution));
    }

    @Override
    public void setFocusTargetEntityId(int entityId) {
        this.focusTargetEntityId = entityId;
    }

    @Override
    public int getFocusTargetEntityId() {
        return focusTargetEntityId;
    }

    @Override
    public void clearMainChain() {
        mainChain.clear();
    }

    @Override
    public void clearFocusChain() {
        focusChain.clear();
    }

    @Override
    public void clearAll() {
        clearMainChain();
        clearFocusChain();
        groupOverflow = 0.0f;
        detonateCooldown = 0;
        chainBuffTicks = 0;
    }

    @Override
    public int getDistinctElementCount() {
        return mainChain.size();
    }

    @Override
    public void addGroupOverflow(float amount) {
        this.groupOverflow = Math.max(0.0f, this.groupOverflow + amount);
    }

    @Override
    public float getGroupOverflow() {
        return groupOverflow;
    }

    @Override
    public void setGroupOverflow(float amount) {
        this.groupOverflow = Math.max(0.0f, amount);
    }

    @Override
    public int getDetonateCooldown() {
        return detonateCooldown;
    }

    @Override
    public void setDetonateCooldown(int ticks) {
        this.detonateCooldown = Math.max(0, ticks);
    }

    @Override
    public void tickCooldown() {
        if (detonateCooldown > 0) {
            detonateCooldown--;
        }
    }

    @Override
    public int getChainBuffTicks() {
        return chainBuffTicks;
    }

    @Override
    public void setChainBuffTicks(int ticks) {
        this.chainBuffTicks = Math.max(0, ticks);
    }

    @Override
    public void tickBuff() {
        if (chainBuffTicks > 0) {
            chainBuffTicks--;
        }
    }

    @Override
    public boolean isChainActive() {
        return !mainChain.isEmpty();
    }

    @Override
    public void removeExpired(long now, int lifetimeTicks) {
        mainChain.removeIf(entry -> now - entry.lastUsedTime() > lifetimeTicks);
        focusChain.removeIf(entry -> now - entry.lastUsedTime() > lifetimeTicks);
    }
}
