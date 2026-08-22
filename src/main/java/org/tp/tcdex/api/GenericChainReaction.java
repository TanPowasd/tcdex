package org.tp.tcdex.api;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 通用元素链反应定义。
 */
public final class GenericChainReaction {

    private final String id;
    private final Predicate<GenericChainContext> matcher;
    private final Set<ChainTriggerTime> triggerTimes;
    private final int priority;
    @Nullable
    private final GenericChainEffect effect;
    @Nullable
    private final Consumer<GenericChainContext> callback;

    private GenericChainReaction(Builder builder) {
        this.id = builder.id;
        this.matcher = builder.matcher;
        this.triggerTimes = Set.copyOf(builder.triggerTimes);
        this.priority = builder.priority;
        this.effect = builder.effect;
        this.callback = builder.callback;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public Predicate<GenericChainContext> getMatcher() {
        return matcher;
    }

    public Set<ChainTriggerTime> getTriggerTimes() {
        return triggerTimes;
    }

    public int getPriority() {
        return priority;
    }

    @Nullable
    public GenericChainEffect getEffect() {
        return effect;
    }

    @Nullable
    public Consumer<GenericChainContext> getCallback() {
        return callback;
    }

    public static final class Builder {
        private final String id;
        private Predicate<GenericChainContext> matcher = ctx -> true;
        private final Set<ChainTriggerTime> triggerTimes = EnumSet.allOf(ChainTriggerTime.class);
        private int priority = 0;
        private GenericChainEffect effect;
        private Consumer<GenericChainContext> callback;

        private Builder(String id) {
            this.id = id;
        }

        public Builder matches(Predicate<GenericChainContext> matcher) {
            this.matcher = matcher;
            return this;
        }

        public Builder triggers(ChainTriggerTime time) {
            this.triggerTimes.clear();
            this.triggerTimes.add(time);
            return this;
        }

        public Builder triggers(ChainTriggerTime... times) {
            this.triggerTimes.clear();
            for (ChainTriggerTime time : times) {
                this.triggerTimes.add(time);
            }
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder effect(GenericChainEffect effect) {
            this.effect = effect;
            return this;
        }

        public Builder callback(Consumer<GenericChainContext> callback) {
            this.callback = callback;
            return this;
        }

        public GenericChainReaction build() {
            return new GenericChainReaction(this);
        }
    }
}
