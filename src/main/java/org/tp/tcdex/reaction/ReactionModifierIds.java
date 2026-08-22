package org.tp.tcdex.reaction;

/**
 * 反应词条 ID 生成工具。
 */
public final class ReactionModifierIds {

    private ReactionModifierIds() {
    }

    /** 根据反应定义生成唯一词条 ID，例如 solar_stasis、moon_void_sinkstar */
    public static String forReaction(ElementReaction reaction) {
        if (reaction == null || reaction.getAuraElement() == null || reaction.getTriggerElement() == null) {
            return "";
        }
        StringBuilder id = new StringBuilder();
        id.append(reaction.getAuraElement().getId());
        id.append('_').append(reaction.getTriggerElement().getId());
        if (reaction.getCatalystElement() != null) {
            id.append('_').append(reaction.getCatalystElement().getId());
        }
        return id.toString();
    }
}
