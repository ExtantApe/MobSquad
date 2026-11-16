package com.extantape.mobsquad.world;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Growth;
import com.extantape.mobsquad.model.Species;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Each launch critter's special (brief, "Launch critters"), and the experience critters earn. Specials grow at the
 * milestones in {@link Growth}. None of them hurts a player or changes a block a player couldn't change: the bee's
 * crops grow through the same event as natural growth, so anything that stops growth stops it, and the enderman only
 * fetches a block after a block-break check as its owner passes, so claims and protections decide.
 */
public final class Specials implements Runnable, Listener {
    public static final long PERIOD = 20;
    /** How long a hostile mob remembers which critters hit it, for experience when it's beaten. */
    static final long HELP_MS = 30_000;
    /** A dropped item waits this long before the fox fetches it, so you can still give it to a friend. */
    static final int FETCH_WAIT_TICKS = 100;
    static final long ARM_MS = 10_000;

    private final Plugin plugin;
    private final Squads squads;
    private final NamespacedKey fetchFor, fetched, speedy;
    /** Critters busy with a special right now, and what they're doing. */
    private final Map<UUID, Job> jobs = new HashMap<>();
    /** Which critters hit a hostile mob, and when. */
    private final Map<UUID, Map<UUID, Long>> helpers = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    /** Owners who right-clicked their enderman: the critter, and until when the next swing picks a block. */
    private final Map<UUID, Armed> armed = new HashMap<>();
    private long ticks;

    sealed interface Job permits Fetch, Find, Carry, PlayDead {}
    record Fetch(Item item, long until) implements Job {}
    record Find(Location ore, long until) implements Job {}
    record Carry(long until) implements Job {}
    record PlayDead(long until) implements Job {}
    record Armed(UUID critter, long since, long until) {}

    public Specials(Plugin plugin, Squads squads) {
        this.plugin = plugin;
        this.squads = squads;
        this.fetchFor = new NamespacedKey(plugin, "fetch-for");
        this.fetched = new NamespacedKey(plugin, "fetched");
        this.speedy = new NamespacedKey(plugin, "speedy");
    }

    private Posse posse() {
        return squads.posse();
    }

    private static int stage(Posse.Out o) {
        return Growth.stage(o.critter().level());
    }

    private boolean ready(UUID critter, String what, long ms) {
        String key = critter + what;
        long now = System.currentTimeMillis();
        Long next = cooldowns.get(key);
        if (next != null && next > now) return false;
        cooldowns.put(key, now + ms);
        return true;
    }

    // ----- Every second: the always-on specials, and the jobs.

    @Override
    public void run() {
        ticks += PERIOD;
        long now = System.currentTimeMillis();
        jobs.entrySet().removeIf(e -> !posse().isOut(e.getKey()));
        armed.values().removeIf(a -> a.until < now || !posse().isOut(a.critter));
        helpers.values().forEach(m -> m.values().removeIf(t -> now - t > HELP_MS));
        helpers.values().removeIf(Map::isEmpty);
        boolean minute = ticks % 1200 == 0;
        for (Posse.Out o : List.copyOf(posse().all())) {
            Player p = Bukkit.getPlayer(o.owner());
            if (p == null || !o.mob().isValid()) continue;
            switch (o.critter().species()) {
                case CHICKEN -> {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 50, 0, true, false, true));
                    speedyEgg(o, p, now);
                }
                case SKELETON -> shareArrows(o, p, now);
                case AXOLOTL -> axolotl(o, p);
                case BEE -> bee(o);
                case FOX -> fox(o, p);
                default -> {}
            }
        }
        if (minute) for (Player p : Bukkit.getOnlinePlayers()) squads.minuteOut(p, posse().of(p.getUniqueId()));
        squads.keepSidekicks();
    }

    /** When a critter's next gift is due (the chicken's egg, the skeleton's arrows), by critter. */
    private final Map<UUID, Long> nextGift = new HashMap<>();
    /** The first gift comes this long after a critter goes out. */
    static final long FIRST_GIFT_MS = 30_000;

    private boolean giftDue(Posse.Out o, long now, long everyMs) {
        Long due = nextGift.get(o.critter().id());
        if (due == null) { nextGift.put(o.critter().id(), now + FIRST_GIFT_MS); return false; }
        if (now < due) return false;
        nextGift.put(o.critter().id(), now + everyMs);
        return true;
    }

    /**
     * From level 25, the chicken lays a speedy egg every 5 minutes, a minute sooner at each later milestone. Using one
     * gives you Speed II for 10 seconds, Speed III from level 75. Only its owner can pick it up.
     */
    private void speedyEgg(Posse.Out o, Player p, long now) {
        int s = stage(o);
        if (s < 1 || !giftDue(o, now, (300 - 60L * (s - 1)) * 1000)) return;
        ItemStack egg = new ItemStack(Material.EGG);
        egg.editMeta(m -> {
            m.customName(Component.text("Speedy egg", NamedTextColor.YELLOW).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            m.lore(List.of(Component.text("Right-click for a burst of speed.", NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(speedy, PersistentDataType.INTEGER, s >= 3 ? 2 : 1);
        });
        Item dropped = p.getWorld().dropItem(p.getLocation(), egg); // at your feet, so nobody has to hunt for it
        dropped.setOwner(p.getUniqueId());
        o.mob().getWorld().playSound(o.mob().getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() != Material.EGG || !hand.hasItemMeta()) return;
        Integer amp = hand.getItemMeta().getPersistentDataContainer().get(speedy, PersistentDataType.INTEGER);
        if (amp == null) return;
        event.setCancelled(true); // eaten for speed, never thrown
        Player p = event.getPlayer();
        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) hand.subtract();
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, amp, false, true, true));
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.6f, 1.6f);
    }

    /**
     * From level 25, the skeleton shares arrows when you carry a bow or crossbow: two every 2 minutes, one more and 20
     * seconds sooner at each later milestone.
     */
    private void shareArrows(Posse.Out o, Player p, long now) {
        int s = stage(o);
        if (s < 1 || !giftDue(o, now, (120 - 20L * (s - 1)) * 1000)) return;
        if (!p.getInventory().contains(Material.BOW) && !p.getInventory().contains(Material.CROSSBOW)) return;
        give(p, new ItemStack(Material.ARROW, 1 + s));
        p.sendActionBar(Component.text(o.critter().name() + " shares " + (1 + s) + " arrows.", NamedTextColor.GRAY));
    }

    /** Water breathing and regeneration within 6 blocks, 2 more at each milestone; regeneration II from level 75. */
    private void axolotl(Posse.Out o, Player p) {
        int s = stage(o);
        double r = 6 + 2 * s;
        if (!p.getWorld().equals(o.mob().getWorld()) || p.getLocation().distanceSquared(o.mob().getLocation()) > r * r) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 50, 0, true, false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, s >= 3 ? 1 : 0, true, false, true));
    }

    /** Every second, one crop within 3 blocks grows a step, plus one more crop and one more block of reach per milestone. */
    private void bee(Posse.Out o) {
        int s = stage(o), r = 3 + s;
        Block at = o.mob().getLocation().getBlock();
        List<Block> crops = new ArrayList<>();
        for (int x = -r; x <= r; x++)
            for (int y = -3; y <= 1; y++)
                for (int z = -r; z <= r; z++) {
                    Block b = at.getRelative(x, y, z);
                    if (!Tag.CROPS.isTagged(b.getType()) || b.getType() == Material.PITCHER_CROP) continue;
                    if (b.getBlockData() instanceof Ageable a && a.getAge() < a.getMaximumAge()) crops.add(b);
                }
        java.util.Collections.shuffle(crops);
        for (Block b : crops.subList(0, Math.min(crops.size(), 1 + s))) {
            Ageable a = (Ageable) b.getBlockData().clone();
            a.setAge(a.getAge() + 1);
            BlockState next = b.getState();
            next.setBlockData(a);
            if (!new BlockGrowEvent(b, next).callEvent()) continue;
            next.update(true, false);
            b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 3, 0.3, 0.2, 0.3);
        }
    }

    /** Items the owner dropped or left behind, within 6 blocks of the fox and 2 more per milestone. */
    private void fox(Posse.Out o, Player p) {
        if (jobs.containsKey(o.critter().id())) return;
        double r = 6 + 2 * stage(o);
        for (Entity e : o.mob().getNearbyEntities(r, r / 2, r)) {
            if (!(e instanceof Item item) || !item.isValid() || item.getTicksLived() < FETCH_WAIT_TICKS) continue;
            if (item.getPersistentDataContainer().has(fetched)) continue;
            if (item.getLocation().distanceSquared(p.getLocation()) < 2.5 * 2.5) continue;
            String tagged = item.getPersistentDataContainer().get(fetchFor, PersistentDataType.STRING);
            if (!p.getUniqueId().equals(item.getThrower()) && !p.getUniqueId().toString().equals(tagged)) continue;
            jobs.put(o.critter().id(), new Fetch(item, System.currentTimeMillis() + 15_000));
            return;
        }
    }

    /**
     * One step of a critter's special. Returns true when it's busy, so it doesn't follow or fight this step. Called by
     * the posse before following.
     */
    boolean step(Posse.Out o, Player p) {
        Job job = jobs.get(o.critter().id());
        if (job == null) return false;
        Mob m = o.mob();
        long now = System.currentTimeMillis();
        boolean done = switch (job) {
            case Fetch f -> {
                if (now > f.until() || !f.item().isValid()) yield true;
                if (m.getLocation().distanceSquared(f.item().getLocation()) > 1.8 * 1.8) {
                    m.getPathfinder().moveTo(f.item().getLocation(), 1.4);
                    yield false;
                }
                Item item = f.item();
                item.getPersistentDataContainer().set(fetched, PersistentDataType.BYTE, (byte) 1);
                item.teleport(p.getLocation());
                item.setPickupDelay(0);
                p.playSound(p.getLocation(), Sound.ENTITY_FOX_AMBIENT, 0.6f, 1.2f);
                yield true;
            }
            case Find f -> {
                if (now > f.until() || !f.ore().getWorld().equals(m.getWorld())) yield true;
                m.getPathfinder().moveTo(f.ore().clone().add(0.5, 1.2, 0.5), 1.2);
                p.spawnParticle(Particle.WAX_ON, f.ore().clone().add(0.5, 0.5, 0.5), 6, 0.4, 0.4, 0.4, 0);
                yield false;
            }
            case Carry c -> now > c.until();
            case PlayDead d -> {
                m.getPathfinder().stopPathfinding();
                if (now <= d.until()) yield false;
                if (m instanceof Axolotl a) a.setPlayingDead(false);
                yield true;
            }
        };
        if (done) jobs.remove(o.critter().id());
        return !done;
    }

    // ----- The allay and the enderman: right-click your own critter.

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Posse.Out o = posse().of(event.getRightClicked());
        if (o == null || !o.owner().equals(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        Player p = event.getPlayer();
        switch (o.critter().species()) {
            case ALLAY -> showOre(o, p, p.getInventory().getItemInMainHand().getType());
            case ENDERMAN -> {
                armed.put(p.getUniqueId(), new Armed(o.critter().id(), System.currentTimeMillis(), System.currentTimeMillis() + ARM_MS));
                p.sendActionBar(Component.text(o.critter().name() + " is watching. Swing at the block you want.", NamedTextColor.LIGHT_PURPLE));
            }
            default -> {}
        }
    }

    /** Shown one ore, the allay flies to the nearest matching block within 8 blocks of you, 2 more per milestone. */
    void showOre(Posse.Out o, Player p, Material shown) {
        Set<Material> family = Ores.family(shown);
        if (family == null) {
            p.sendActionBar(Component.text("Hold an ore, a raw ore or a gem and right-click " + o.critter().name() + ".", NamedTextColor.YELLOW));
            return;
        }
        if (!ready(o.critter().id(), "find", 3000)) return;
        int r = 8 + 2 * stage(o);
        Block from = p.getLocation().getBlock(), best = null;
        double bestD = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++)
            for (int y = -r; y <= r; y++)
                for (int z = -r; z <= r; z++) {
                    Block b = from.getRelative(x, y, z);
                    if (!family.contains(b.getType())) continue;
                    double d = x * x + y * y + z * z;
                    if (d < bestD) { bestD = d; best = b; }
                }
        String name = Ores.name(shown);
        if (best == null) {
            p.sendMessage(Component.text(o.critter().name() + " looked all around and found no " + name + " close by.", NamedTextColor.YELLOW));
            return;
        }
        jobs.put(o.critter().id(), new Find(best.getLocation(), System.currentTimeMillis() + 20_000));
        p.sendMessage(Component.text(o.critter().name() + " found " + name + " about " + Math.round(Math.sqrt(bestD)) + " blocks away. Follow it!", NamedTextColor.AQUA));
        p.playSound(o.mob().getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.2f);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSwing(PlayerAnimationEvent event) {
        fetchBlock(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_AIR) return;
        if (fetchBlock(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreakWhileArmed(BlockBreakEvent event) {
        if (event instanceof FetchCheck) return;
        if (armed.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); // the swing was for the enderman
    }

    /** A block-break check as the owner, told apart from a real break so it isn't refused for being armed. */
    static final class FetchCheck extends BlockBreakEvent {
        FetchCheck(Block b, Player p) {
            super(b, p);
            setDropItems(false);
        }
    }

    /**
     * The enderman fetches the block its owner swings at, within 8 blocks of the owner and 4 more per milestone, if it's
     * a block an enderman can carry and the owner could break it there.
     */
    boolean fetchBlock(Player p) {
        Armed a = armed.get(p.getUniqueId());
        long now = System.currentTimeMillis();
        if (a == null) return false;
        if (now - a.since < 250) return true; // the right-click's own swing
        armed.remove(p.getUniqueId());
        if (a.until < now) return false;
        Posse.Out o = posse().byId(a.critter);
        if (o == null || !(o.mob() instanceof Enderman e)) return false;
        int r = 8 + 4 * stage(o);
        Block b = p.getTargetBlockExact(r);
        String who = o.critter().name();
        if (b == null) {
            p.sendActionBar(Component.text("That's too far for " + who + ".", NamedTextColor.YELLOW));
            return true;
        }
        Material item = b.getBlockData().getPlacementMaterial();
        if (!Tag.ENDERMAN_HOLDABLE.isTagged(b.getType()) || !item.isItem() || item.isAir()) {
            p.sendActionBar(Component.text(who + " can't carry that. Try dirt, sand, a flower or a pumpkin.", NamedTextColor.YELLOW));
            return true;
        }
        if (!new FetchCheck(b, p).callEvent()) {
            p.sendActionBar(Component.text("You can't dig there, so " + who + " won't either.", NamedTextColor.YELLOW));
            return true;
        }
        jobs.put(o.critter().id(), new Carry(System.currentTimeMillis() + 1500));
        e.setCarriedBlock(b.getBlockData());
        Location back = e.getLocation();
        posse().moveCritter(e, b.getLocation().add(0.5, 1, 0.5));
        b.setType(Material.AIR, true);
        b.getWorld().spawnParticle(Particle.PORTAL, b.getLocation().add(0.5, 0.5, 0.5), 30, 0.3, 0.5, 0.3);
        Location spot = b.getLocation().add(0.5, 0.5, 0.5);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // the block is the owner's even if the enderman went home meanwhile, but only in the world it came from, so
            // nothing crosses world groups (Multiverse-Inventories); otherwise it drops where the block was
            boolean here = p.isOnline() && p.getWorld().equals(spot.getWorld());
            if (here) give(p, new ItemStack(item));
            else spot.getWorld().dropItem(spot, new ItemStack(item));
            if (!e.isValid()) return;
            e.setCarriedBlock(null);
            posse().moveCritter(e, here && p.getWorld().equals(e.getWorld()) ? p.getLocation() : back);
        }, 20L);
        return true;
    }

    private static void give(Player p, ItemStack stack) {
        for (ItemStack left : p.getInventory().addItem(stack).values()) p.getWorld().dropItem(p.getLocation(), left);
    }

    // ----- Hurt critters, and the golem's guard.

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCritterHurt(EntityDamageEvent event) {
        Posse.Out o = posse().of(event.getEntity());
        if (o == null || !(event.getEntity() instanceof LivingEntity l)) return;
        double left = l.getHealth() - event.getFinalDamage();
        if (left <= 0) return; // tuckered out, nothing to do
        double max = l.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        int s = stage(o);
        if (o.critter().species() == Species.AXOLOTL && left < max / 2 && l instanceof Axolotl ax && ready(o.critter().id(), "dead", 60_000 - 10_000L * s)) {
            ax.setPlayingDead(true);
            ax.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, s >= 2 ? 1 : 0, true, false));
            jobs.put(o.critter().id(), new PlayDead(System.currentTimeMillis() + 5000));
            for (Entity near : ax.getNearbyEntities(16, 8, 16)) if (near instanceof Mob m && ax.equals(m.getTarget())) m.setTarget(null);
        }
        if (o.critter().species() == Species.ENDERMAN && left < max * 0.4 && ready(o.critter().id(), "blink", 30_000 - 5_000L * s)) {
            Player p = Bukkit.getPlayer(o.owner());
            if (p == null || !p.getWorld().equals(l.getWorld())) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!l.isValid()) return;
                l.getWorld().spawnParticle(Particle.PORTAL, l.getLocation().add(0, 1, 0), 40, 0.4, 1, 0.4);
                posse().moveCritter(l, p.getLocation());
            });
        }
    }

    /** Nothing goes after an axolotl that's playing dead. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        Posse.Out o = posse().of(event.getTarget());
        if (o != null && jobs.get(o.critter().id()) instanceof PlayDead) event.setCancelled(true);
    }

    /** The golem takes a quarter of a hostile mob's hit on its owner within 8 blocks, a tenth more per milestone. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOwnerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        Entity from = event.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Entity s ? s : event.getDamager();
        if (!posse().fights().fair(from)) return;
        for (Posse.Out o : posse().of(p.getUniqueId())) {
            if (o.critter().species() != Species.IRON_GOLEM || !o.mob().isValid()) continue;
            if (!o.mob().getWorld().equals(p.getWorld()) || o.mob().getLocation().distanceSquared(p.getLocation()) > 64) continue;
            double share = guardShare(stage(o)), dmg = event.getDamage();
            event.setDamage(dmg * (1 - share));
            o.mob().damage(dmg * share, DamageSource.builder(DamageType.MOB_ATTACK).withCausingEntity(from).withDirectEntity(from).build());
            return;
        }
    }

    public static double guardShare(int stage) {
        return 0.25 + 0.1 * stage;
    }

    // ----- Experience: hostile mobs a critter helped beat.

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCritterHit(EntityDamageByEntityEvent event) {
        Entity from = event.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Entity s ? s : event.getDamager();
        Posse.Out o = posse().of(from);
        if (o == null || !posse().fights().fair(event.getEntity())) return;
        helpers.computeIfAbsent(event.getEntity().getUniqueId(), k -> new HashMap<>()).put(o.critter().id(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Map<UUID, Long> who = helpers.remove(dead.getUniqueId());
        if (who != null) {
            double max = dead.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            long xp = Math.max(1, Math.min(100, Math.round(max)));
            long now = System.currentTimeMillis();
            for (var e : who.entrySet()) {
                Posse.Out o = posse().byId(e.getKey());
                if (o != null && now - e.getValue() <= HELP_MS) squads.critterXp(o, xp);
            }
        }
        Player killer = dead.getKiller();
        if (killer == null || !hasFox(killer)) return;
        Location at = dead.getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Entity e : at.getWorld().getNearbyEntities(at, 1.5, 1.5, 1.5))
                if (e instanceof Item i && i.getTicksLived() <= 3) i.getPersistentDataContainer().set(fetchFor, PersistentDataType.STRING, killer.getUniqueId().toString());
        });
    }

    /** Drops from blocks you broke count as yours to fetch. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrops(BlockDropItemEvent event) {
        if (!hasFox(event.getPlayer())) return;
        for (Item i : event.getItems()) i.getPersistentDataContainer().set(fetchFor, PersistentDataType.STRING, event.getPlayer().getUniqueId().toString());
    }

    private boolean hasFox(Player p) {
        for (Posse.Out o : posse().of(p.getUniqueId())) if (o.critter().species() == Species.FOX) return true;
        return false;
    }

    void forget(UUID critter) {
        jobs.remove(critter);
        nextGift.remove(critter);
        cooldowns.keySet().removeIf(k -> k.startsWith(critter.toString()));
        armed.values().removeIf(a -> a.critter.equals(critter));
    }

    /** Ores the allay knows, grouped so a raw ore or a gem finds its ore in stone, deepslate or the Nether. */
    static final class Ores {
        private static final Map<Material, Set<Material>> BY_ITEM = new EnumMap<>(Material.class);
        private static final Map<Material, String> NAMES = new EnumMap<>(Material.class);

        private static void add(String name, Material[] items, Material... blocks) {
            Set<Material> set = new HashSet<>(List.of(blocks));
            for (Material m : items) { BY_ITEM.put(m, set); NAMES.put(m, name); }
            for (Material m : blocks) { BY_ITEM.put(m, set); NAMES.put(m, name); }
        }

        static {
            add("coal ore", new Material[] {Material.COAL}, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE);
            add("iron ore", new Material[] {Material.RAW_IRON}, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
            add("copper ore", new Material[] {Material.RAW_COPPER}, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
            add("gold ore", new Material[] {Material.RAW_GOLD}, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE);
            add("redstone ore", new Material[] {Material.REDSTONE}, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE);
            add("lapis ore", new Material[] {Material.LAPIS_LAZULI}, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE);
            add("diamond ore", new Material[] {Material.DIAMOND}, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE);
            add("emerald ore", new Material[] {Material.EMERALD}, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);
            add("quartz ore", new Material[] {Material.QUARTZ}, Material.NETHER_QUARTZ_ORE);
            add("ancient debris", new Material[] {Material.NETHERITE_SCRAP}, Material.ANCIENT_DEBRIS);
        }

        static Set<Material> family(Material shown) {
            return BY_ITEM.get(shown);
        }

        static String name(Material shown) {
            return NAMES.getOrDefault(shown, "ore");
        }
    }
}
