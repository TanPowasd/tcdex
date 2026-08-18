package org.tp.tcdex.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import org.tp.tcdex.Tcdex;

/**
 * 生成 assets/tcdex/lang/{en_us,zh_cn}.json。
 * 语言条目集中于此，避免手写 JSON 与代码脱节。
 */
public class ModLanguageProvider extends LanguageProvider {

    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, Tcdex.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        if ("zh_cn".equals(locale)) {
            addZhCn();
        } else {
            addEnUs();
        }
    }

    private void addEnUs() {
        add("item.tcdex.my_love", "I love GFF");
        add("item.tcdex.light_essence", "Light Essence");
        add("itemGroup.tcdex_tab", "TCDEX");

        add("modifier.tcdex.eager_edge", "Eager Edge");
        add("modifier.tcdex.elemental", "Elemental Charge");
        add("modifier.tcdex.elemental.flavor", "Energy flows, element is fate");
        add("modifier.tcdex.elemental.description", "Grants a random element (Solar/Arc/Void/Stasis/Strand) when applied, locked forever. Attacks convert to elemental damage and trigger keywords: Solar burns over time and ignites at max stacks; Stasis slows, freezes and shatters for bonus damage; Void explodes and weakens on hit, heals on kill; Arc chains lightning and blinds, buffs on kill; Strand weakens enemy damage, suspends at max stacks, grants armor on kill.");
        add("modifier.tcdex.elemental.element.solar", "Solar");
        add("modifier.tcdex.elemental.element.arc", "Arc");
        add("modifier.tcdex.elemental.element.void", "Void");
        add("modifier.tcdex.elemental.element.stasis", "Stasis");
        add("modifier.tcdex.elemental.element.strand", "Strand");
        add("modifier.tcdex.elemental.element.prism", "Prism");
        add("modifier.tcdex.elemental.tooltip.element", "Element: %s");
        add("modifier.tcdex.elemental.tooltip.uncharged", "Element: uncharged (assigned when kept in inventory)");

        add("modifier.tcdex.prism_resonance", "Prism Resonance");
        add("modifier.tcdex.prism_resonance.flavor", "All light bends to the prism");
        add("modifier.tcdex.prism_resonance.description", "Attacks convert to Prism damage: wear every elemental shield at matching 2x efficiency, permanently break Prism shields (no regen), and apply the Refract mark on hit (25% splash). The exclusive source of Prism damage. Exclusive with Elemental Charge.");

        add("modifier.tcdex.exclusive", "Cannot be combined with %s");

        add("effect.tcdex.devour", "Devour");
        add("effect.tcdex.war_banner", "War Banner");
        add("effect.tcdex.amplified", "Amplified");

        add("modifier.tcdex.all_permitted", "All Permitted");
        add("modifier.tcdex.all_permitted.flavor", "Nothing is true, everything is permitted");
        add("modifier.tcdex.all_permitted.description", "Right-click to switch between [Xu] and [Yun] forms. Xu: steal target armor and attack on hit (5 stacks/5s), accumulate Forbidden. Yun: consume Forbidden exponentially (1/2/4/8/16) for true damage ignoring armor, combo boosts damage. At 100 Forbidden, auto-overload AOE (4 blocks), then enter Sin (-50% damage); atone with 5 Xu-form hits.");
        add("modifier.tcdex.combat_echo", "Combat Echo");
        add("modifier.tcdex.combat_echo.flavor", "One blow, endless echoes");
        add("modifier.tcdex.combat_echo.description", "After a melee hit, deal an additional 50% of the damage dealt");

        add("modifier.tcdex.synthetic_hands", "Synthoceps");
        add("modifier.tcdex.synthetic_hands.flavor", "The more the merrier");
        add("modifier.tcdex.synthetic_hands.description", "When surrounded by 3+ hostiles, melee damage +200%; melee kills restore 2 health.");

        add("modifier.tcdex.burning_fists", "Burning Fists");
        add("modifier.tcdex.burning_fists.flavor", "The worm's hunger burns in every blow");
        add("modifier.tcdex.burning_fists.description", "Melee kills ignite Burning Fists: +40% melee damage per stack (max 5), decaying over time. From level 2, grants bonus weapon damage (+2, +4 at level 3).");

        add("hud.tcdex.eager_cooldown", "Eager Cooldown");
        add("hud.tcdex.all_permitted.yun", "All Permitted");
        add("hud.tcdex.all_permitted.xu", "Nothing is True");
        add("hud.tcdex.all_permitted.sin", "Sin");
        add("hud.tcdex.all_permitted.combo", "Combo x%s");

        add("death.attack.tcdex.pure", "%1$s was annihilated by pure forbidden power");
        add("death.attack.tcdex.kinetic", "%1$s was pierced by kinetic force");
        add("death.attack.tcdex.solar", "%1$s was burned away by Solar");
        add("death.attack.tcdex.scorch", "%1$s was reduced to ash by scorching");
        add("death.attack.tcdex.arc", "%1$s was pierced by Arc");
        add("death.attack.tcdex.void", "%1$s was consumed by Void");
        add("death.attack.tcdex.stasis", "%1$s was shattered by Stasis");
        add("death.attack.tcdex.strand", "%1$s was torn apart by Strand");
        add("death.attack.tcdex.prism", "%1$s was shattered by prismatic light");

        add("tooltip.tcdex.light_level", "Light: %s");

        add("command.tcdex.setlight.success", "Set main hand item light level to %s");
        add("command.tcdex.setlight.not_tool", "Main hand item must be a Tinkers tool or armor");
        add("command.tcdex.setlooklight.success", "Set %s light level to %s");
        add("command.tcdex.setlooklight.no_target", "No entity in view direction");
        add("command.tcdex.setlooklight.not_living", "Target must be a living creature and cannot be a player");
        add("command.tcdex.debug.enabled", "Light level debug output enabled");
        add("command.tcdex.debug.disabled", "Light level debug output disabled");
        add("command.tcdex.debug.element.enabled", "Elemental debug output enabled");
        add("command.tcdex.debug.element.disabled", "Elemental debug output disabled");
        add("command.tcdex.setelement.success", "Set main hand weapon Elemental Charge to %s");
        add("command.tcdex.setelement.invalid", "Unknown element: %s (choices: solar/arc/void/stasis/strand)");
        add("command.tcdex.setelement.no_modifier", "Main hand weapon has no Elemental Charge modifier");
        add("command.tcdex.setelement.prism_disabled", "Prism cannot be obtained via Elemental Charge (boss-exclusive)");
    }

    private void addZhCn() {
        add("item.tcdex.my_love", "此处应该表达些什么");
        add("item.tcdex.light_essence", "光之精华");
        add("itemGroup.tcdex_tab", "此处应该有些文字");

        add("modifier.tcdex.eager_edge", "急切刀锋");
        add("modifier.tcdex.elemental", "元素充能");
        add("modifier.tcdex.elemental.flavor", "能量已注入，元素即命运");
        add("modifier.tcdex.elemental.description", "打上后随机获得一种元素能力（烈日/电弧/虚空/冰影/缚丝），一旦确定无法改变。攻击转化为对应元素伤害，并触发元素关键词：烈日持续灼烧并在满层时引爆；冰影渐进减速、冻结后受击粉碎增伤；虚空受击爆炸增伤、击杀回血；电弧连锁闪电并致盲、击杀获得强化；缚丝削弱敌方伤害、满层悬挂、击杀获得织甲减伤。");
        add("modifier.tcdex.elemental.element.solar", "烈日");
        add("modifier.tcdex.elemental.element.arc", "电弧");
        add("modifier.tcdex.elemental.element.void", "虚空");
        add("modifier.tcdex.elemental.element.stasis", "冰影");
        add("modifier.tcdex.elemental.element.strand", "缚丝");
        add("modifier.tcdex.elemental.element.prism", "棱镜");
        add("modifier.tcdex.elemental.tooltip.element", "元素: %s");
        add("modifier.tcdex.elemental.tooltip.uncharged", "元素: 未充能（收入物品栏后确定）");

        add("modifier.tcdex.prism_resonance", "棱镜共鸣");
        add("modifier.tcdex.prism_resonance.flavor", "万光归棱");
        add("modifier.tcdex.prism_resonance.description", "攻击转化为棱镜伤害：对所有元素护盾按匹配效率（×2）磨损，可永久打破棱镜盾（不再回复）；命中施加折射标记（25% 溅射）。棱镜伤害的专属来源，与元素充能互斥。");

        add("modifier.tcdex.exclusive", "与 %s 互斥，无法同时添加");

        add("effect.tcdex.devour", "吞噬");
        add("effect.tcdex.war_banner", "战争旗帜");
        add("effect.tcdex.amplified", "增幅");

        add("modifier.tcdex.all_permitted", "万般皆允");
        add("modifier.tcdex.all_permitted.flavor", "万物皆虚，万事皆允");
        add("modifier.tcdex.all_permitted.description", "右键切换【虚/允】双形态。虚形态：命中偷取目标护甲与攻击（5层/5秒），积累禁忌；允形态：等比消耗禁忌（1/2/4/8/16）造成无视护甲的真伤，连击提升伤害。禁忌满100自动超载AOE（4格），事后陷入罪业（伤害减半），虚形态命中5次赎罪解除。");
        add("modifier.tcdex.combat_echo", "战斗回响");
        add("modifier.tcdex.combat_echo.flavor", "一击未尽，余响不绝");
        add("modifier.tcdex.combat_echo.description", "近战命中后，追加一次本击实际伤害 50% 的伤害");

        add("modifier.tcdex.synthetic_hands", "合成感受器");
        add("modifier.tcdex.synthetic_hands.flavor", "敌众我寡，愈战愈勇");
        add("modifier.tcdex.synthetic_hands.description", "被 3 个或更多敌对生物包围时，近战伤害 +200%；近战击杀回复 2 点生命。");

        add("modifier.tcdex.burning_fists", "燃烧之拳");
        add("modifier.tcdex.burning_fists.flavor", "虫神的饥渴，燃烧于每一拳");
        add("modifier.tcdex.burning_fists.description", "近战击杀点燃燃烧之拳：每层近战伤害 +40%（最多 5 层），加成随时间衰减；等级 2 起提供额外武器攻击力（+2，等级 3 +4）。");

        add("hud.tcdex.eager_cooldown", "急切冷却");
        add("hud.tcdex.all_permitted.yun", "万事皆允");
        add("hud.tcdex.all_permitted.xu", "万物皆虚");
        add("hud.tcdex.all_permitted.sin", "罪业");
        add("hud.tcdex.all_permitted.combo", "连击 ×%s");

        add("death.attack.tcdex.pure", "%1$s 被纯粹的禁忌之力湮灭");
        add("death.attack.tcdex.kinetic", "%1$s 被动能武器贯穿");
        add("death.attack.tcdex.solar", "%1$s 被烈日灼烧殆尽");
        add("death.attack.tcdex.scorch", "%1$s 在灼烧中化为灰烬");
        add("death.attack.tcdex.arc", "%1$s 被电弧贯穿");
        add("death.attack.tcdex.void", "%1$s 被虚空吞噬");
        add("death.attack.tcdex.stasis", "%1$s 被冰影冻结粉碎");
        add("death.attack.tcdex.strand", "%1$s 被缚丝缠绕撕裂");
        add("death.attack.tcdex.prism", "%1$s 被棱镜之光折射湮灭");

        add("tooltip.tcdex.light_level", "光等: %s");

        add("command.tcdex.setlight.success", "已将主手装备光等设置为 %s");
        add("command.tcdex.setlight.not_tool", "主手物品必须是匠魂工具或盔甲");
        add("command.tcdex.setlooklight.success", "已将 %s 的光等设置为 %s");
        add("command.tcdex.setlooklight.no_target", "视角方向没有找到生物");
        add("command.tcdex.setlooklight.not_living", "目标必须是生物，且不能是玩家");
        add("command.tcdex.debug.enabled", "光等调试输出已开启");
        add("command.tcdex.debug.disabled", "光等调试输出已关闭");
        add("command.tcdex.debug.element.enabled", "元素调试输出已开启");
        add("command.tcdex.debug.element.disabled", "元素调试输出已关闭");
        add("command.tcdex.setelement.success", "已将主手武器的元素充能指定为 %s");
        add("command.tcdex.setelement.invalid", "未知元素: %s（可选: solar/arc/void/stasis/strand）");
        add("command.tcdex.setelement.no_modifier", "主手武器没有元素充能词条");
        add("command.tcdex.setelement.prism_disabled", "棱镜无法通过元素充能获得（Boss 专属）");
    }
}
