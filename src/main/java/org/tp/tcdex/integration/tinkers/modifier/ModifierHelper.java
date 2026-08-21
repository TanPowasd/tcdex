package org.tp.tcdex.integration.tinkers.modifier;

import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.integration.tinkers.modifier.elemental.ElementalModifier;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * 词条判定辅助：词条前置依赖检查（"词条 A 需要词条 B"）。
 *
 * <p>典型场景：<b>电弧增幅</b>需要工具上存在<b>元素充能</b>词条且固化的元素为<b>电弧</b>
 * （否则增强 Jolt 的词条毫无效果）。词条在三个层面使用本类：
 * <ul>
 *   <li>{@code modifierValidate}：不满足依赖时返回提示（词条工作台拒绝添加）</li>
 *   <li>运行时方法开头：不满足依赖时返回默认值/跳过（防命令强加）</li>
 *   <li>{@code modifierAddTooltip}：未满足依赖时显示红色需求提示</li>
 * </ul></p>
 */
public final class ModifierHelper {

    private ModifierHelper() {
    }

    /** 工具上是否包含指定类型的词条（按实例类型匹配，兼容子类） */
    public static boolean hasModifier(IToolStackView tool, Class<? extends Modifier> type) {
        if (tool == null || type == null) {
            return false;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (type.isInstance(entry.getModifier())) {
                return true;
            }
        }
        return false;
    }

    /** 工具上是否包含指定 id 的词条 */
    public static boolean hasModifier(IToolStackView tool, ModifierId id) {
        if (tool == null || id == null) {
            return false;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (id.equals(entry.getId())) {
                return true;
            }
        }
        return false;
    }

    /** 是否带元素充能词条（任意元素） */
    public static boolean hasElementalCharge(IToolStackView tool) {
        return hasModifier(tool, ElementalModifier.class);
    }

    /**
     * 读取元素充能固化的元素（无元素充能词条/尚未固化返回 null）。
     * 固化写回工具持久数据后客户端同样可读（用于 tooltip 显示）。
     */
    @Nullable
    public static ElementType getElementalChargeElement(IToolStackView tool) {
        if (tool == null) {
            return null;
        }
        return ElementalModifier.parseElement(tool.getPersistentData().getString(ElementalModifier.ELEMENT_KEY));
    }

    /** 工具是否带元素充能词条且固化为指定元素（词条依赖判定，如"需要元素充能（电弧）"） */
    public static boolean hasElementalCharge(IToolStackView tool, ElementType element) {
        return hasElementalCharge(tool) && getElementalChargeElement(tool) == element;
    }
}
