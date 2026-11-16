package com.extantape.mobsquad.hook;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.item.ItemContext;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.registry.NamespacedRegistry;
import dev.aurelium.auraskills.api.skill.CustomSkill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import java.io.File;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Wranglin' as an AuraSkills skill (brief, "How the plugins fit"). Only loaded when AuraSkills is installed, so the
 * rest of MobSquad never touches its classes. The skill's settings are in {@code plugins/MobSquad/auraskills}.
 * <p>MobSquad sets the level rather than adding experience: AuraSkills 2.4.0 loses experience that other plugins add to
 * their own skills when it levels them up, and its level curve differs from MobSquad's.
 */
public final class AuraSkillsWranglin implements WranglinSkill {
    private final AuraSkillsApi api;
    private final CustomSkill skill;

    private AuraSkillsWranglin(AuraSkillsApi api, CustomSkill skill) {
        this.api = api;
        this.skill = skill;
    }

    public static AuraSkillsWranglin register(JavaPlugin plugin) {
        File dir = new File(plugin.getDataFolder(), "auraskills");
        for (String f : new String[] {"auraskills/skills.yml", "auraskills/sources/wranglin.yml"})
            if (!new File(plugin.getDataFolder(), f).exists()) plugin.saveResource(f, false);
        AuraSkillsApi api = AuraSkillsApi.get();
        NamespacedRegistry registry = api.useRegistry("mobsquad", dir);
        CustomSkill skill = CustomSkill.builder(NamespacedId.of("mobsquad", "wranglin"))
            .displayName("Wranglin'")
            .description("Coax mobs into critters. Higher levels let more critters out at once and open new tiers of mobs.")
            .item(ItemContext.builder().material("lead").pos("4,5").build())
            .build();
        registry.registerSkill(skill);
        return new AuraSkillsWranglin(api, skill);
    }

    @Override
    public void show(Player p, int level) {
        SkillsUser u = api.getUser(p.getUniqueId());
        if (u == null || !u.isLoaded() || u.getSkillLevel(skill) == level) return;
        u.setSkillLevel(skill, level);
        u.setSkillXp(skill, 0);
    }
}
