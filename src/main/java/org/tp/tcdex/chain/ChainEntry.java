package org.tp.tcdex.chain;

import org.tp.tcdex.element.ElementType;

/**
 * 连携链中的一条元素记录。
 *
 * @param element      元素类型
 * @param lastUsedTime 最近一次使用该元素的服务器世界时间
 * @param contribution 累计贡献权重
 */
public record ChainEntry(ElementType element, long lastUsedTime, float contribution) {
}
