package com.extantape.mobsquad.coax;

import com.extantape.mobsquad.model.Species;
import java.util.List;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Fox;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * The nine launch puzzles. The creeper standoff, the enderman's no-staring rule and the chicken's seed trail are the
 * brief's; the other six are the plugins lead's proposals (2026-11-15), waiting for the owner's say.
 */
public final class Puzzles {
    private Puzzles() {}

    public static List<Puzzle> all() {
        return List.of(new ChickenTrail(), new AxolotlSwim(), new BeeStillness(), new FoxCrouch(), new AllayTune(),
            new CreeperStandoff(), new EndermanNoStaring(), new SkeletonFairShot(), new GolemFixUp());
    }

    static boolean is(ItemStack it, Material m) {
        return it != null && it.getType() == m;
    }

    /**
     * Whether a player is crouching. Bots can't crouch where the tests can see it, so a test server started with
     * {@code -Dmobsquad.test.sneak-tag=true} also counts a player with the scoreboard tag {@code mobsquad-test-sneak}.
     * Without that flag the tag means nothing.
     */
    static final boolean TEST_SNEAK = Boolean.getBoolean("mobsquad.test.sneak-tag");

    static boolean crouching(Player p) {
        return p.isSneaking() || TEST_SNEAK && p.getScoreboardTags().contains("mobsquad-test-sneak");
    }

    static void use(Player p, ItemStack hand) {
        if (p.getGameMode() != GameMode.CREATIVE) hand.subtract();
    }

    // ---------- Yard ----------

    /** Hold seeds, right-click, and walk it ten blocks within 40 seconds without running or putting the seeds away. */
    public static final class ChickenTrail implements Puzzle {
        public static final double DISTANCE = 10, RUNNING = 1.35;
        static final Set<Material> SEEDS = Set.of(Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS,
            Material.PUMPKIN_SEEDS, Material.TORCHFLOWER_SEEDS, Material.PITCHER_POD);

        public Species species() { return Species.CHICKEN; }
        public boolean starts(ItemStack hand) { return hand != null && SEEDS.contains(hand.getType()); }
        public String intro() { return "The chicken's interested. Walk it " + (int) DISTANCE + " blocks on a trail of seeds. Don't run."; }

        public Attempt begin(Player p, Mob mob) {
            var start = mob.getLocation();
            return new Attempt(p, mob, 40) {
                protected String tick() {
                    if (player.isSprinting() || player.isInsideVehicle() || player.isGliding() || player.isFlying() || moved() > RUNNING)
                        return "Too fast. The chicken lost interest.";
                    if (!starts(player.getInventory().getItemInMainHand())) return "You put the seeds away, so the chicken lost interest.";
                    if (mob.getLocation().distance(player.getLocation()) > 2.5) mob.getPathfinder().moveTo(player.getLocation(), 1.0);
                    return mob.getLocation().distance(start) >= DISTANCE ? DONE : null;
                }

                protected String timeUp() { return "The chicken got bored. Try again, a little quicker."; }
            };
        }
    }

    /** Proposal. A bucket of tropical fish while you're both in water, then 15 seconds swimming with it. */
    public static final class AxolotlSwim implements Puzzle {
        public Species species() { return Species.AXOLOTL; }
        public boolean starts(ItemStack hand) { return is(hand, Material.TROPICAL_FISH_BUCKET); }
        public boolean uses() { return false; } // a bucket of fish is a lot to ask; it's only shown
        public String intro() { return "The axolotl wants to play. Stay in the water with it for 15 seconds."; }

        public String refuse(Player p, Mob mob) {
            return p.isInWater() && mob.isInWater() ? null : "You both need to be in the water for that.";
        }

        public Attempt begin(Player p, Mob mob) {
            return new Attempt(p, mob, 30) {
                protected String tick() {
                    if (!player.isInWater()) return "You left the water. The axolotl swam off.";
                    if (mob.getLocation().distance(player.getLocation()) > 3) mob.getPathfinder().moveTo(player.getLocation(), 1.0);
                    return seconds() >= 15 ? DONE : null;
                }

                protected double reach() { return 6; }
            };
        }
    }

    /** Proposal. Show it a flower, then stand still for 10 seconds while it looks you over. Bees like calm. */
    public static final class BeeStillness implements Puzzle {
        public Species species() { return Species.BEE; }
        public boolean starts(ItemStack hand) { return hand != null && Tag.FLOWERS.isTagged(hand.getType()); }
        public String intro() { return "The bee's curious. Stand still for 10 seconds while it looks you over."; }

        public String refuse(Player p, Mob mob) {
            return ((Bee) mob).getAnger() > 0 ? "That bee's too cross to listen right now." : null;
        }

        public Attempt begin(Player p, Mob mob) {
            var spot = p.getLocation();
            return new Attempt(p, mob, 20) {
                protected String tick() {
                    if (player.getLocation().distance(spot) > 1) return "You moved. The bee buzzed off. Start over.";
                    double a = seconds() * 2.5;
                    mob.getPathfinder().moveTo(spot.clone().add(Math.cos(a) * 1.5, 1, Math.sin(a) * 1.5), 1.0);
                    return seconds() >= 10 ? DONE : null;
                }

                protected String event(Event e) {
                    return e instanceof EntityDamageByEntityEvent d && d.getDamager().equals(player) ? "You swung at it. The bee buzzed off." : null;
                }

                protected void end() { ((Bee) mob).setAnger(0); }
            };
        }
    }

    // ---------- Holler ----------

    /** Proposal. Crouch and offer berries, then stay crouched within reach for 15 seconds. Foxes spook easily. */
    public static final class FoxCrouch implements Puzzle {
        public Species species() { return Species.FOX; }
        public boolean starts(ItemStack hand) { return is(hand, Material.SWEET_BERRIES) || is(hand, Material.GLOW_BERRIES); }
        public String intro() { return "The fox is sniffing the berries. Stay crouched near it for 15 seconds."; }

        public String refuse(Player p, Mob mob) {
            Fox f = (Fox) mob;
            if (f.getFirstTrustedPlayer() != null || f.getSecondTrustedPlayer() != null) return "That fox already trusts somebody.";
            return crouching(p) ? null : "Foxes spook easily. Crouch first.";
        }

        public Attempt begin(Player p, Mob mob) {
            return new Attempt(p, mob, 30) {
                protected String tick() {
                    if (!crouching(player)) return "You stood up. The fox spooked. Start over.";
                    if (mob.getLocation().distance(player.getLocation()) > 3) mob.getPathfinder().moveTo(player.getLocation(), 0.8);
                    return seconds() >= 15 ? DONE : null;
                }
            };
        }
    }

    /** Proposal. Give it an amethyst shard, then play a note block near it three times within 30 seconds. */
    public static final class AllayTune implements Puzzle {
        public Species species() { return Species.ALLAY; }
        public boolean starts(ItemStack hand) { return is(hand, Material.AMETHYST_SHARD); }
        public String intro() { return "The allay's humming. Play a note block near it three times."; }

        public Attempt begin(Player p, Mob mob) {
            return new Attempt(p, mob, 30) {
                int notes;

                protected String tick() { return notes >= 3 ? DONE : null; }

                protected String event(Event e) {
                    if (e instanceof NotePlayEvent n && n.getBlock().getLocation().add(0.5, 0.5, 0.5).distance(mob.getLocation()) <= 8) notes++;
                    return null;
                }

                protected double reach() { return 12; }
            };
        }
    }

    // ---------- Yonder ----------

    /**
     * The creeper standoff (brief). Gunpowder starts it. The creeper swells three times: crouch through every swell,
     * and give it gunpowder each time it calms. It can never go off during the standoff.
     */
    public static final class CreeperStandoff implements Puzzle {
        static final int ROUNDS = 3;
        static final double SWELL = 2.0, CALM = 4.0; // seconds swelling, then seconds to give gunpowder

        public Species species() { return Species.CREEPER; }
        public boolean starts(ItemStack hand) { return is(hand, Material.GUNPOWDER); }
        public String intro() { return "Momma said never look a creeper in the eye. Crouch while it swells, and give it gunpowder each time it calms."; }

        public Attempt begin(Player p, Mob mob) {
            Creeper c = (Creeper) mob;
            c.setTarget(null);
            return new Attempt(p, mob, 40) {
                int round;
                boolean swelling = true, fed;
                double phase;

                protected String tick() {
                    phase += Coaxing.PERIOD / 20.0;
                    c.setTarget(null);
                    c.getPathfinder().stopPathfinding();
                    if (swelling) {
                        c.setFuseTicks(Math.min(c.getMaxFuseTicks() - 2, (int) (phase / SWELL * (c.getMaxFuseTicks() - 2))));
                        if (!crouching(player)) return "You stood up mid-swell. The creeper backed off. Start over.";
                        if (phase >= SWELL) { swelling = false; fed = false; phase = 0; c.setFuseTicks(0); }
                    } else {
                        c.setFuseTicks(0);
                        if (fed) {
                            round++;
                            if (round >= ROUNDS) return DONE;
                            swelling = true;
                            phase = 0;
                        } else if (phase >= CALM) return "It calmed down and you didn't give it gunpowder. Start over.";
                    }
                    return null;
                }

                protected String click(ItemStack hand) {
                    if (!is(hand, Material.GUNPOWDER)) return null;
                    if (swelling) return "Too soon. Wait for it to calm. Start over.";
                    if (!fed) { fed = true; use(player, hand); }
                    return null;
                }

                protected void end() { c.setFuseTicks(0); c.setIgnited(false); c.setTarget(null); }

                protected double reach() { return 6; }
            };
        }
    }

    /**
     * The enderman's no-staring rule (brief). Offer it a block it could carry, then stay near it for 20 seconds
     * without once looking it in the face. It won't turn on you during the puzzle.
     */
    public static final class EndermanNoStaring implements Puzzle {
        public Species species() { return Species.ENDERMAN; }
        public boolean starts(ItemStack hand) { return hand != null && Tag.ENDERMAN_HOLDABLE.isTagged(hand.getType()); }
        public String intro() { return "The enderman's thinking it over. Stay near it for 20 seconds, and don't look it in the eye."; }

        /** The same test the game uses for an enderman being stared at. */
        public static boolean staring(Player p, Enderman e) {
            Vector toEyes = e.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
            double d = toEyes.length();
            if (d > 64 || d < 0.01) return false;
            return p.getEyeLocation().getDirection().normalize().dot(toEyes.normalize()) > 1 - 0.025 / d && p.hasLineOfSight(e);
        }

        public Attempt begin(Player p, Mob mob) {
            Enderman e = (Enderman) mob;
            return new Attempt(p, mob, 40) {
                protected String tick() {
                    e.getPathfinder().stopPathfinding();
                    e.setTarget(null);
                    if (ticks > 4 && staring(player, e)) return "You looked it in the eye. Start over.";
                    return seconds() >= 20 ? DONE : null;
                }

                protected void end() { e.setTarget(null); }
            };
        }
    }

    /** Proposal. Hand it an arrow, then hit a target block three times with your own arrows within 30 seconds. A fair shot. */
    public static final class SkeletonFairShot implements Puzzle {
        public Species species() { return Species.SKELETON; }
        public boolean starts(ItemStack hand) { return is(hand, Material.ARROW); }
        public String intro() { return "The skeleton respects a fair shot. Hit a target block three times while it watches."; }

        public Attempt begin(Player p, Mob mob) {
            return new Attempt(p, mob, 30) {
                int hits;

                protected String tick() {
                    mob.setTarget(null);
                    mob.getPathfinder().stopPathfinding();
                    return hits >= 3 ? DONE : null;
                }

                protected String event(Event ev) {
                    if (ev instanceof ProjectileHitEvent h && h.getEntity() instanceof Arrow a && player.equals(a.getShooter())) {
                        Block b = h.getHitBlock();
                        if (b != null && b.getType() == Material.TARGET && b.getLocation().distance(mob.getLocation()) <= 24) hits++;
                    }
                    if (ev instanceof EntityDamageByEntityEvent d && d.getEntity().equals(mob)) return "You hit the skeleton. That's no fair shot.";
                    return null;
                }

                protected void end() { mob.setTarget(null); }

                protected double reach() { return 24; }
            };
        }
    }

    // ---------- Backcountry ----------

    /** Proposal. Only a golem a player built. Patch it up with four iron ingots, then give it a poppy, within a minute. */
    public static final class GolemFixUp implements Puzzle {
        public Species species() { return Species.IRON_GOLEM; }
        public boolean starts(ItemStack hand) { return is(hand, Material.IRON_INGOT); }
        public boolean uses() { return false; } // the first ingot is counted, and used, as the first patch
        public String intro() { return "The golem lets you look it over. Four iron ingots to patch it up, then a poppy."; }

        public String refuse(Player p, Mob mob) {
            return ((IronGolem) mob).isPlayerCreated() ? null : "A village's golem belongs to the village.";
        }

        public Attempt begin(Player p, Mob mob) {
            Attempt a = new Attempt(p, mob, 60) {
                int ingots;
                boolean poppy;

                protected String tick() { return poppy ? DONE : null; }

                protected String click(ItemStack hand) {
                    if (is(hand, Material.IRON_INGOT) && ingots < 4) { ingots++; use(player, hand); }
                    else if (is(hand, Material.POPPY)) {
                        if (ingots < 4) return "Patch it up first: " + (4 - ingots) + " more iron. Start over.";
                        poppy = true;
                        use(player, hand);
                    }
                    return null;
                }

                protected String event(Event e) {
                    return e instanceof EntityDamageByEntityEvent d && d.getEntity().equals(mob) ? "You hurt the golem. Start over." : null;
                }

                protected double reach() { return 6; }
            };
            a.click(p.getInventory().getItemInMainHand());
            return a;
        }
    }
}
