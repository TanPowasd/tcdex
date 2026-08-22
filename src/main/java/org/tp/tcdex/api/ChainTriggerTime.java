package org.tp.tcdex.api;

/**
 * 通用元素链反应触发时机。
 */
public enum ChainTriggerTime {
    /** 元素链发生变化时 */
    CHAIN_CHANGE,
    /** 连携引爆时 */
    DETONATE,
    /** 命定终结时 */
    FINISHER
}
