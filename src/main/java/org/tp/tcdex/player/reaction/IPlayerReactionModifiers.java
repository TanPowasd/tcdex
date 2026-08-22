package org.tp.tcdex.player.reaction;

import java.util.Set;

/**
 * 玩家反应词条能力。
 *
 * <p>默认所有反应词条对玩家开启；可以通过该能力禁用特定反应词条。</p>
 */
public interface IPlayerReactionModifiers {

    /** 是否拥有/启用某个反应词条（默认 true） */
    boolean hasModifier(String reactionId);

    /** 启用某个反应词条（默认就是启用，主要用于重新启用） */
    void addModifier(String reactionId);

    /** 禁用某个反应词条 */
    void removeModifier(String reactionId);

    /** 获取当前被禁用的反应词条 */
    Set<String> getDisabledModifiers();

    /** 重置为全部启用 */
    void clear();
}
