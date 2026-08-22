package org.tp.tcdex.chain;

import org.tp.tcdex.element.ElementType;

import java.util.List;

/**
 * 玩家连携数据接口。
 *
 * <p>连携链用于记录玩家最近使用的元素行为，后续供“连携引爆 / 终结技”系统使用。</p>
 */
public interface IPlayerChainData {

    /** 获取玩家主链（只读视图） */
    List<ChainEntry> getMainChain();

    /** 获取当前目标准焦点链（只读视图） */
    List<ChainEntry> getFocusChain();

    /** 向主链添加一次元素行为 */
    void addElement(ElementType element, long now, float contribution);

    /** 向焦点链添加一次元素行为 */
    void addFocusElement(ElementType element, long now, float contribution);

    /** 设置当前焦点目标实体 id（-1 表示无） */
    void setFocusTargetEntityId(int entityId);

    /** 获取当前焦点目标实体 id */
    int getFocusTargetEntityId();

    /** 清空主链 */
    void clearMainChain();

    /** 清空焦点链 */
    void clearFocusChain();

    /** 清空全部连携数据 */
    void clearAll();

    /** 获取主链中不同元素数量 */
    int getDistinctElementCount();

    /** 群体连携额外层数 */
    void addGroupOverflow(float amount);

    /** 获取群体连携额外层数 */
    float getGroupOverflow();

    /** 设置群体连携额外层数 */
    void setGroupOverflow(float amount);

    /** 获取连携引爆剩余冷却 tick */
    int getDetonateCooldown();

    /** 设置连携引爆冷却 tick */
    void setDetonateCooldown(int ticks);

    /** 每 tick 递减引爆冷却 */
    void tickCooldown();

    /** 获取连携增益剩余 tick */
    int getChainBuffTicks();

    /** 设置连携增益剩余 tick */
    void setChainBuffTicks(int ticks);

    /** 每 tick 递减连携增益 */
    void tickBuff();

    /** 主链是否仍有有效元素 */
    boolean isChainActive();

    /** 移除超过持续时间的过期连携记录 */
    void removeExpired(long now, int lifetimeTicks);
}
