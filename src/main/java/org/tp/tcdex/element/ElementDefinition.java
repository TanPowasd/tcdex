package org.tp.tcdex.element;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TCDEX 元素完整定义。
 *
 * <p>这是元素体系重构后的核心数据结构：把原来散落在 {@link ElementType} 和各个事件里的
 * 元素参数统一收拢到一处，并支持后续数据驱动 / Add 包注册。</p>
 */
public final class ElementDefinition {

    private final ElementType type;
    private final String id;
    private final String displayName;
    private final ElementCategory category;
    private final int color;
    private final ParticleOptions particle;
    private final float stacksPerHit;
    private final int stateDuration;
    private final float doTPerStack;
    private final float auraPerHit;
    private final boolean pseudo;
    private final boolean reactionParticipant;
    private final List<String> keywords;

    private ElementDefinition(Builder builder) {
        this.type = builder.type;
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.category = builder.category;
        this.color = builder.color;
        this.particle = builder.particle;
        this.stacksPerHit = builder.stacksPerHit;
        this.stateDuration = builder.stateDuration;
        this.doTPerStack = builder.doTPerStack;
        this.auraPerHit = builder.auraPerHit;
        this.pseudo = builder.pseudo;
        this.reactionParticipant = builder.reactionParticipant;
        this.keywords = Collections.unmodifiableList(new ArrayList<>(builder.keywords));
    }

    public static Builder builder(ElementType type, String id, ElementCategory category) {
        return new Builder(type, id, category);
    }

    public ElementType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ElementCategory getCategory() {
        return category;
    }

    public int getColor() {
        return color;
    }

    public ParticleOptions getParticle() {
        return particle;
    }

    public float getStacksPerHit() {
        return stacksPerHit;
    }

    public int getStateDuration() {
        return stateDuration;
    }

    public float getDoTPerStack() {
        return doTPerStack;
    }

    public float getAuraPerHit() {
        return auraPerHit;
    }

    public boolean isPseudo() {
        return pseudo;
    }

    public boolean isReactionParticipant() {
        return reactionParticipant;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public static final class Builder {
        private final ElementType type;
        private final String id;
        private final ElementCategory category;
        private String displayName;
        private int color = 0xFFFFFFFF;
        private ParticleOptions particle = ParticleTypes.CLOUD;
        private float stacksPerHit = 1.0f;
        private int stateDuration = 100;
        private float doTPerStack = 0.0f;
        private float auraPerHit = 1.0f;
        private boolean pseudo = false;
        private boolean reactionParticipant = true;
        private final List<String> keywords = new ArrayList<>();

        private Builder(ElementType type, String id, ElementCategory category) {
            this.type = type;
            this.id = id;
            this.category = category;
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder particle(ParticleOptions particle) {
            this.particle = particle;
            return this;
        }

        public Builder stacksPerHit(float stacksPerHit) {
            this.stacksPerHit = stacksPerHit;
            return this;
        }

        public Builder stateDuration(int stateDuration) {
            this.stateDuration = stateDuration;
            return this;
        }

        public Builder doTPerStack(float doTPerStack) {
            this.doTPerStack = doTPerStack;
            return this;
        }

        public Builder auraPerHit(float auraPerHit) {
            this.auraPerHit = auraPerHit;
            return this;
        }

        public Builder pseudo(boolean pseudo) {
            this.pseudo = pseudo;
            return this;
        }

        public Builder reactionParticipant(boolean reactionParticipant) {
            this.reactionParticipant = reactionParticipant;
            return this;
        }

        public Builder addKeyword(String keyword) {
            this.keywords.add(keyword);
            return this;
        }

        public ElementDefinition build() {
            return new ElementDefinition(this);
        }
    }
}
