package org.tp.tcdex.api;

/**
 * 光等伤害修正提供器。
 *
 * <p>附属 mod 可以通过 {@link TcdexAPI#registerDamageModifierProvider(IDamageModifierProvider)}
 * 修改最终的光等伤害倍率，例如添加自定义 Buff、副本压制等。</p>
 */
public interface IDamageModifierProvider {

    /**
     * 修改“玩家攻击怪物”时的伤害倍率。
     *
     * @param multiplier     当前倍率
     * @param attackerLight  攻击方光等
     * @param defenderLight  防守方光等
     * @return 修改后的倍率
     */
    default float modifyDealtDamage(float multiplier, int attackerLight, int defenderLight) {
        return multiplier;
    }

    /**
     * 修改“怪物攻击玩家”时的伤害倍率。
     *
     * @param multiplier     当前倍率
     * @param attackerLight  攻击方光等
     * @param defenderLight  防守方光等
     * @return 修改后的倍率
     */
    default float modifyTakenDamage(float multiplier, int attackerLight, int defenderLight) {
        return multiplier;
    }
}
