package org.tp.tcdex.player.reaction;

import java.util.HashSet;
import java.util.Set;

/**
 * 玩家反应词条默认实现：默认全部开启，只记录被禁用的词条。
 */
public class PlayerReactionModifiers implements IPlayerReactionModifiers {

    private final Set<String> disabled = new HashSet<>();

    @Override
    public boolean hasModifier(String reactionId) {
        return reactionId == null || reactionId.isEmpty() || !disabled.contains(reactionId);
    }

    @Override
    public void addModifier(String reactionId) {
        if (reactionId != null) {
            disabled.remove(reactionId);
        }
    }

    @Override
    public void removeModifier(String reactionId) {
        if (reactionId != null && !reactionId.isEmpty()) {
            disabled.add(reactionId);
        }
    }

    @Override
    public Set<String> getDisabledModifiers() {
        return new HashSet<>(disabled);
    }

    @Override
    public void clear() {
        disabled.clear();
    }
}
