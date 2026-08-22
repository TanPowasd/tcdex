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
        add("advancements.tcdex.first_enter.title", "Guardian Masters Their Own Destiny");
        add("advancements.tcdex.first_enter.description", "Eyes up, Guardian. Your destiny begins now.");
        add("advancements.tcdex.enter_end.title", "May the Light Be With You");
        add("advancements.tcdex.enter_end.description", "The End awaits, Guardian. Light your way.");

        add("item.tcdex.my_love", "I love GFF");
        add("item.tcdex.light_essence", "Light Essence");
        add("item.tcdex.guardian_emblem", "Guardian Emblem");
        add("item.tcdex.artifact_flower", "Artifact Flower");
        add("item.tcdex.artifact_plume", "Artifact Plume");
        add("item.tcdex.artifact_sands", "Artifact Sands");
        add("item.tcdex.artifact_goblet", "Artifact Goblet");
        add("item.tcdex.artifact_circlet", "Artifact Circlet");
        add("item.tcdex.element_energy_orb", "Element Energy Orb");
        add("item.tcdex.light_orb", "Light Orb");
        add("item.tcdex.artifact.slot", "Slot: %s");
        add("item.tcdex.artifact.slot.artifact_flower", "Flower");
        add("item.tcdex.artifact.slot.artifact_plume", "Plume");
        add("item.tcdex.artifact.slot.artifact_sands", "Sands");
        add("item.tcdex.artifact.slot.artifact_goblet", "Goblet");
        add("item.tcdex.artifact.slot.artifact_circlet", "Circlet");
        add("itemGroup.tcdex_tab", "TCDEX (Genshin x Destiny 2)");

        add("modifier.tcdex.eager_edge", "Eager Edge");
        add("modifier.tcdex.elemental", "Elemental Charge");
        add("modifier.tcdex.elemental.flavor", "Energy flows, element is fate");
        add("modifier.tcdex.elemental.description", "Grants a random element (Solar/Arc/Void/Stasis/Strand/Sinkstar/Mistflow) when applied, locked forever. Attacks convert to elemental damage and trigger keywords: Solar burns over time and ignites at max stacks; Stasis slows, freezes and shatters for bonus damage; Void explodes and weakens on hit, heals on kill; Arc chains lightning and blinds, buffs on kill; Strand weakens enemy damage, suspends at max stacks, grants armor on kill.");
        add("modifier.tcdex.elemental.element.solar", "Solar");
        add("modifier.tcdex.elemental.element.arc", "Arc");
        add("modifier.tcdex.elemental.element.void", "Void");
        add("modifier.tcdex.elemental.element.stasis", "Stasis");
        add("modifier.tcdex.elemental.element.strand", "Strand");
        add("modifier.tcdex.elemental.element.moon", "Moon");
        add("modifier.tcdex.elemental.element.prism", "Prism");
        add("modifier.tcdex.elemental.element.sinkstar", "Meteor");
        add("modifier.tcdex.elemental.element.mistflow", "Aetherflow");
        add("modifier.tcdex.elemental.element.tide", "Water");
        add("modifier.tcdex.elemental.tooltip.element", "Element: %s");
        add("modifier.tcdex.elemental.tooltip.uncharged", "Element: uncharged (assigned when kept in inventory)");

        add("modifier.tcdex.five_forces", "Five Forces");
        add("modifier.tcdex.five_forces.flavor", "Five paths, one will");
        add("modifier.tcdex.five_forces.description", "Each attack randomly converts kinetic damage into one of the five elements (Solar/Arc/Void/Stasis/Strand, weights follow config; Prism excluded), applying that element's keyword state on hit. Works for melee and ranged. Exclusive with Elemental Charge and Prism Resonance.");

        add("modifier.tcdex.prism_resonance", "Prism Resonance");
        add("modifier.tcdex.prism_resonance.flavor", "All light bends to the prism");
        add("modifier.tcdex.prism_resonance.description", "Attacks convert to Prism damage: wear every elemental shield at matching 2x efficiency, permanently break Prism shields (no regen), and apply the Refract mark on hit (25% splash). The exclusive source of Prism damage. Exclusive with Elemental Charge.");

        add("modifier.tcdex.exclusive", "Cannot be combined with %s");

        add("effect.tcdex.devour", "Devour");
        add("effect.tcdex.war_banner", "War Banner");
        add("effect.tcdex.amplified", "Amplified");

        add("modifier.tcdex.war_banner", "War Banner");
        add("modifier.tcdex.war_banner.flavor", "Raise the banner. Rally the Light.");
        add("modifier.tcdex.war_banner.description", "Kills raise the War Banner: each kill adds a stack (max 4, 8s refreshed on kill). While the banner stands, nearby players (8 blocks, including yourself) deal +8% damage per stack and heal 0.5 per second per stack.");

        add("modifier.tcdex.all_permitted", "All Permitted");
        add("modifier.tcdex.all_permitted.flavor", "Nothing is true, everything is permitted");
        add("modifier.tcdex.all_permitted.description", "Right-click to switch between [Xu] and [Yun] forms. Xu: steal target armor and attack on hit (5 stacks/5s), accumulate Forbidden. Yun: consume Forbidden exponentially (1/2/4/8/16) for true damage ignoring armor, combo boosts damage. At 100 Forbidden, auto-overload AOE (4 blocks), then enter Sin (-50% damage); atone with 5 Xu-form hits.");

        add("modifier.tcdex.elemental_mastery", "Elemental Mastery");
        add("modifier.tcdex.elemental_mastery.flavor", "Every element sings in your hands");
        add("modifier.tcdex.elemental_mastery.description", "Provides Elemental Mastery to the player. Each level grants +20 Mastery, which strengthens element reactions: damage, duration, radius, cooldown and aura cost.");
        add("modifier.tcdex.elemental_mastery.tooltip", "Elemental Mastery +%s");

        add("modifier.tcdex.combat_echo", "Combat Echo");
        add("modifier.tcdex.combat_echo.flavor", "One blow, endless echoes");
        add("modifier.tcdex.combat_echo.description", "After a melee hit, deal an additional 50% of the damage dealt");

        add("modifier.tcdex.synthetic_hands", "Synthoceps");
        add("modifier.tcdex.synthetic_hands.flavor", "The more the merrier");
        add("modifier.tcdex.synthetic_hands.description", "When surrounded by 3+ hostiles, melee damage +200%; melee kills restore 2 health.");

        add("modifier.tcdex.burning_fists", "Burning Fists");
        add("modifier.tcdex.burning_fists.flavor", "The worm's hunger burns in every blow");
        add("modifier.tcdex.burning_fists.description", "Melee kills ignite Burning Fists: +40% melee damage per stack (max 5), decaying over time. From level 2, grants bonus weapon damage (+2, +4 at level 3).");

        add("modifier.tcdex.arc_amplifier", "Arc Amplifier");
        add("modifier.tcdex.arc_amplifier.flavor", "Every strike summons the storm");
        add("modifier.tcdex.arc_amplifier.description", "Requires Elemental Charge (Arc). Empowers Arc Jolt chain lightning: chain damage +50% and chain radius +1 block.");

        add("modifier.tcdex.requires.elemental_arc", "Requires Elemental Charge (Arc)");

        add("modifier.tcdex.burst_barrier", "Burst Barrier");
        add("modifier.tcdex.burst_barrier.flavor", "When the wall falls, rage pours out");
        add("modifier.tcdex.burst_barrier.description", "When your shield breaks, half of the overflow damage is blocked, and a shockwave deals 4 damage to enemies within 3 blocks (players excluded).");

        add("modifier.tcdex.kinetic_tremors", "Kinetic Tremors");
        add("modifier.tcdex.kinetic_tremors.flavor", "The earth remembers every blow");
        add("modifier.tcdex.kinetic_tremors.description", "Kinetic weapons (no elemental modifier) only. Melee hits on the same target build stacks (max 5); at full stacks, an earthquake erupts beneath the target dealing 5 kinetic damage to all enemies within 3 blocks and knocking them back. Stacks decay after 4s without hitting the same target. Exclusive with Elemental Charge and Prism Resonance.");
        add("modifier.tcdex.kinetic_siphon", "Kinetic Siphon");
        add("modifier.tcdex.kinetic_siphon.flavor", "Every kill feeds the light");
        add("modifier.tcdex.kinetic_siphon.description", "Kinetic weapons (no elemental modifier) only. Kills with this weapon restore 2 player shield points. Exclusive with Elemental Charge and Prism Resonance.");

        add("hud.tcdex.eager_cooldown", "Eager Cooldown");
        add("hud.tcdex.transcendence", "Transcendence");
        add("hud.tcdex.transcendence.charge", "Light %d / Dark %d");
        add("hud.tcdex.transcendence.ready", "Transcendence Ready");
        add("key.tcdex.transcendence", "Activate Transcendence");
        add("key.tcdex.element_burst", "Activate Element Burst");
        add("key.tcdex.chain_smart", "Smart Chain Action");
        add("key.tcdex.chain_detonate", "Detonate Chain");
        add("key.tcdex.chain_finisher", "Chain Finisher");
        add("key.categories.tcdex", "TCDEX");
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
        add("death.attack.tcdex.moon", "%1$s was eclipsed by the Moon");
        add("death.attack.tcdex.sinkstar", "%1$s was crushed by a falling star");
        add("death.attack.tcdex.mistflow", "%1$s was scattered by the mistflow");
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
        add("command.tcdex.debug.reaction.enabled", "Reaction debug output enabled");
        add("command.tcdex.debug.reaction.disabled", "Reaction debug output disabled");
        add("command.tcdex.setelement.success", "Set main hand weapon Elemental Charge to %s");
        add("command.tcdex.setelement.invalid", "Unknown element: %s (choices: solar/arc/void/stasis/strand/moon/sinkstar/mistflow/tide/prism)");
        add("command.tcdex.setelement.no_modifier", "Main hand weapon has no Elemental Charge modifier");
        add("command.tcdex.setelement.prism_disabled", "Prism cannot be obtained via Elemental Charge (boss-exclusive)");
    }

    private void addZhCn() {
        add("advancements.tcdex.first_enter.title", "守护者掌握自己的命运");
        add("advancements.tcdex.first_enter.description", "抬头，守护者。你的命运从此刻开始。");
        add("advancements.tcdex.enter_end.title", "愿光能与你同在");
        add("advancements.tcdex.enter_end.description", "末地在等待，守护者。以光引路。");

        add("item.tcdex.my_love", "此处应该表达些什么");
        add("item.tcdex.light_essence", "光之精华");
        add("item.tcdex.guardian_emblem", "守护者徽记");
        add("item.tcdex.artifact_flower", "圣遗物·花");
        add("item.tcdex.artifact_plume", "圣遗物·羽");
        add("item.tcdex.artifact_sands", "圣遗物·沙");
        add("item.tcdex.artifact_goblet", "圣遗物·杯");
        add("item.tcdex.artifact_circlet", "圣遗物·冠");
        add("item.tcdex.element_energy_orb", "元素能量球");
        add("item.tcdex.light_orb", "光能微粒");
        add("item.tcdex.artifact.slot", "部位: %s");
        add("item.tcdex.artifact.slot.artifact_flower", "花");
        add("item.tcdex.artifact.slot.artifact_plume", "羽");
        add("item.tcdex.artifact.slot.artifact_sands", "沙");
        add("item.tcdex.artifact.slot.artifact_goblet", "杯");
        add("item.tcdex.artifact.slot.artifact_circlet", "冠");
        add("itemGroup.tcdex_tab", "TCDEX（原命2）");

        add("modifier.tcdex.eager_edge", "急切刀锋");
        add("modifier.tcdex.elemental", "元素充能");
        add("modifier.tcdex.elemental.flavor", "能量已注入，元素即命运");
        add("modifier.tcdex.elemental.description", "打上后随机获得一种元素能力（烈日/电弧/虚空/冰影/缚丝/沉星/岚流），一旦确定无法改变。攻击转化为对应元素伤害，并触发元素关键词：烈日持续灼烧并在满层时引爆；冰影渐进减速、冻结后受击粉碎增伤；虚空受击爆炸增伤、击杀回血；电弧连锁闪电并致盲、击杀获得强化；缚丝削弱敌方伤害、满层悬挂、击杀获得织甲减伤；沉星与岚流参与 TCDEX 元素反应。");
        add("modifier.tcdex.elemental.element.solar", "烈日");
        add("modifier.tcdex.elemental.element.arc", "电能");
        add("modifier.tcdex.elemental.element.void", "虚空");
        add("modifier.tcdex.elemental.element.stasis", "冰影");
        add("modifier.tcdex.elemental.element.strand", "缚丝");
        add("modifier.tcdex.elemental.element.moon", "月");
        add("modifier.tcdex.elemental.element.prism", "棱镜");
        add("modifier.tcdex.elemental.element.sinkstar", "落星");
        add("modifier.tcdex.elemental.element.mistflow", "罡流");
        add("modifier.tcdex.elemental.element.tide", "水");
        add("modifier.tcdex.elemental.tooltip.element", "元素: %s");
        add("modifier.tcdex.elemental.tooltip.uncharged", "元素: 未充能（收入物品栏后确定）");

        add("modifier.tcdex.five_forces", "五项之力");
        add("modifier.tcdex.five_forces.flavor", "五道力量，一意贯之");
        add("modifier.tcdex.five_forces.description", "每次攻击将动能伤害随机转化为一种元素伤害（烈日/电弧/虚空/冰影/缚丝，权重遵循配置，棱镜除外），命中同时施加对应元素状态触发关键词。近战与远程均生效。与元素充能、棱镜共鸣互斥。");

        add("modifier.tcdex.prism_resonance", "棱镜共鸣");
        add("modifier.tcdex.prism_resonance.flavor", "万光归棱");
        add("modifier.tcdex.prism_resonance.description", "攻击转化为棱镜伤害：对所有元素护盾按匹配效率（×2）磨损，可永久打破棱镜盾（不再回复）；命中施加折射标记（25% 溅射）。棱镜伤害的专属来源，与元素充能互斥。");

        add("modifier.tcdex.exclusive", "与 %s 互斥，无法同时添加");

        add("effect.tcdex.devour", "吞噬");
        add("effect.tcdex.war_banner", "战争旗帜");
        add("effect.tcdex.amplified", "增幅");

        add("modifier.tcdex.war_banner", "战争旗帜");
        add("modifier.tcdex.war_banner.flavor", "举起旗帜，集结光能");
        add("modifier.tcdex.war_banner.description", "击杀扬旗：持有本词条击杀任意敌人扬起战争旗帜（叠层上限 4，8 秒无击杀落地）。旗帜期间 8 格内玩家（含自己）每层伤害 +8%、每秒治疗 0.5。");

        add("modifier.tcdex.all_permitted", "万般皆允");
        add("modifier.tcdex.all_permitted.flavor", "万物皆虚，万事皆允");
        add("modifier.tcdex.all_permitted.description", "右键切换【虚/允】双形态。虚形态：命中偷取目标护甲与攻击（5层/5秒），积累禁忌；允形态：等比消耗禁忌（1/2/4/8/16）造成无视护甲的真伤，连击提升伤害。禁忌满100自动超载AOE（4格），事后陷入罪业（伤害减半），虚形态命中5次赎罪解除。");

        add("modifier.tcdex.elemental_mastery", "元素精通");
        add("modifier.tcdex.elemental_mastery.flavor", "万元素皆为吾之臂膀");
        add("modifier.tcdex.elemental_mastery.description", "为玩家提供元素精通属性。每级 +20 精通，强化元素反应：伤害、持续时间、范围、冷却与附着消耗。");
        add("modifier.tcdex.elemental_mastery.tooltip", "元素精通 +%s");

        add("modifier.tcdex.combat_echo", "战斗回响");
        add("modifier.tcdex.combat_echo.flavor", "一击未尽，余响不绝");
        add("modifier.tcdex.combat_echo.description", "近战命中后，追加一次本击实际伤害 50% 的伤害");

        add("modifier.tcdex.synthetic_hands", "合成感受器");
        add("modifier.tcdex.synthetic_hands.flavor", "敌众我寡，愈战愈勇");
        add("modifier.tcdex.synthetic_hands.description", "被 3 个或更多敌对生物包围时，近战伤害 +200%；近战击杀回复 2 点生命。");

        add("modifier.tcdex.burning_fists", "燃烧之拳");
        add("modifier.tcdex.burning_fists.flavor", "虫神的饥渴，燃烧于每一拳");
        add("modifier.tcdex.burning_fists.description", "近战击杀点燃燃烧之拳：每层近战伤害 +40%（最多 5 层），加成随时间衰减；等级 2 起提供额外武器攻击力（+2，等级 3 +4）。");

        add("modifier.tcdex.arc_amplifier", "电弧增幅");
        add("modifier.tcdex.arc_amplifier.flavor", "每一击都召来风暴");
        add("modifier.tcdex.arc_amplifier.description", "需要元素充能（电弧）。强化电弧 Jolt 连锁闪电：连锁伤害 +50%，连锁半径 +1 格。");

        add("modifier.tcdex.requires.elemental_arc", "需要元素充能（电弧）");

        add("modifier.tcdex.burst_barrier", "爆裂屏障");
        add("modifier.tcdex.burst_barrier.flavor", "当防线破碎，愤怒倾泻而出");
        add("modifier.tcdex.burst_barrier.description", "护盾被打穿时：溢出伤害减半（格挡 50%），同时释放冲击波，对 3 格内敌人造成 4 点伤害（不含玩家）。");

        add("modifier.tcdex.kinetic_tremors", "动能震颤");
        add("modifier.tcdex.kinetic_tremors.flavor", "大地记得每一击");
        add("modifier.tcdex.kinetic_tremors.description", "仅动能武器（无元素词条）可用。近战连续命中同一目标叠层（最多 5 层），满层时目标脚下爆发地震：3 格内敌人（不含玩家）受到 5 点动能伤害并击退。4 秒未继续命中当前目标则清层。与元素充能、棱镜共鸣互斥。");
        add("modifier.tcdex.kinetic_siphon", "动能虹吸");
        add("modifier.tcdex.kinetic_siphon.flavor", "每一次击杀都滋养着光");
        add("modifier.tcdex.kinetic_siphon.description", "仅动能武器（无元素词条）可用。本武器击杀生物时回复 2 点玩家护盾。与元素充能、棱镜共鸣互斥。");

        add("hud.tcdex.eager_cooldown", "急切冷却");
        add("hud.tcdex.transcendence", "超越");
        add("hud.tcdex.transcendence.charge", "光 %d / 暗 %d");
        add("hud.tcdex.transcendence.ready", "超越就绪");
        add("key.tcdex.transcendence", "激活超越");
        add("key.tcdex.element_burst", "释放元素爆发");
        add("key.tcdex.chain_smart", "智能连携");
        add("key.tcdex.chain_detonate", "连携引爆");
        add("key.tcdex.chain_finisher", "命定终结");
        add("key.categories.tcdex", "TCDEX");
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
        add("death.attack.tcdex.moon", "%1$s 被月蚀吞噬");
        add("death.attack.tcdex.sinkstar", "%1$s 被落星压碎");
        add("death.attack.tcdex.mistflow", "%1$s 被岚流卷散");
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
        add("command.tcdex.debug.reaction.enabled", "元素反应调试输出已开启");
        add("command.tcdex.debug.reaction.disabled", "元素反应调试输出已关闭");
        add("command.tcdex.setelement.success", "已将主手武器的元素充能指定为 %s");
        add("command.tcdex.setelement.invalid", "未知元素: %s（可选: solar/arc/void/stasis/strand/moon/sinkstar/mistflow/tide/prism）");
        add("command.tcdex.setelement.no_modifier", "主手武器没有元素充能词条");
        add("command.tcdex.setelement.prism_disabled", "棱镜无法通过元素充能获得（Boss 专属）");
    }
}
