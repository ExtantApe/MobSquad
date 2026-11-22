package com.extantape.mobsquad.api;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Growth;
import com.extantape.mobsquad.model.Species;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** MobSquad's own answer to {@link MobSquadApi}. */
public final class Api implements MobSquadApi {
    /** The skeleton's arrow, before growth; kept equal to Fights' arrow. */
    static final double ARROW = 2.0;
    private final Squads squads;

    public Api(Squads squads) {
        this.squads = squads;
    }

    @Override
    public List<CritterInfo> critters(Player player) {
        if (player == null || !player.isOnline()) return List.of();
        long now = System.currentTimeMillis();
        List<CritterInfo> list = new ArrayList<>();
        for (Critter c : squads.get(player).critters) list.add(info(c, now, squads.coatData()));
        return List.copyOf(list);
    }

    static CritterInfo info(Critter c, long now, com.extantape.mobsquad.model.CoatData coats) {
        int coat = com.extantape.mobsquad.model.Coats.worn(c);
        return new CritterInfo(c.id(), c.species().id(), c.species().entity, c.name(), c.level(), c.resting(now), fights(c.species()),
            coat, coat == 0 ? "" : coats.name(coat));
    }

    static boolean fights(Species s) {
        return s != Species.ALLAY;
    }

    @Override
    public boolean hasCritterOut(Player player) {
        return player != null && !squads.posse().of(player.getUniqueId()).isEmpty();
    }

    @Override
    public void sendHome(Player player) {
        if (player != null) squads.posse().backAll(player.getUniqueId());
    }

    @Override
    public boolean enabledIn(World world) {
        return world != null && squads.settings().enabledIn(world.getName());
    }

    @Override
    public void keepOff(String worldName) {
        if (worldName != null && !worldName.isBlank()) squads.keepOff(worldName);
    }

    @Override
    public CritterStats stats(String species, int level) {
        return statsFor(Species.byId(species), level);
    }

    static CritterStats statsFor(Species s, int level) {
        if (s == null) return new CritterStats(1, 0, 1);
        double base = !fights(s) ? 0 : s == Species.SKELETON ? ARROW : s.attack;
        return new CritterStats(Growth.health(level), base * Growth.attack(level), Growth.speed(level));
    }
}
