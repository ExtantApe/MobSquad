package com.extantape.mobsquad;

import com.extantape.mobsquad.coax.Coaxing;
import com.extantape.mobsquad.world.Vittles;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Levels;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.store.Store;
import com.extantape.mobsquad.world.BunkhouseMenu;
import com.extantape.mobsquad.world.Bunkhouses;
import com.extantape.mobsquad.world.Dinnerbell;
import com.extantape.mobsquad.world.Posse;
import com.extantape.mobsquad.world.Safety;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobSquadPlugin extends JavaPlugin {
    private Squads squads;
    private Bunkhouses bunkhouses;
    private Dinnerbell dinnerbell;
    private Coaxing coaxing;
    private Vittles vittles;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        squads = new Squads(new Store(getDataFolder().toPath().resolve("players")), getLogger(), Settings.read(getConfig(), getLogger()));
        Posse posse = new Posse(this, o -> {});
        squads.posse(posse);
        bunkhouses = new Bunkhouses(this);
        dinnerbell = new Dinnerbell(this, squads);
        Coaxing coaxing = new Coaxing(squads);
        this.coaxing = coaxing;
        vittles = new Vittles(this, squads);
        vittles.register();
        var pm = getServer().getPluginManager();
        pm.registerEvents(new Safety(squads), this);
        pm.registerEvents(new BunkhouseMenu(squads, bunkhouses), this);
        pm.registerEvents(dinnerbell, this);
        pm.registerEvents(coaxing, this);
        pm.registerEvents(vittles, this);
        pm.registerEvents(new Listener() {
            @EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                e.getPlayer().discoverRecipe(vittles.key());
            }

            /** A critter left behind by a crash is never saved, but if one turns up, it goes. */
            @EventHandler
            public void onLoad(EntitiesLoadEvent e) {
                for (Entity en : e.getEntities()) if (posse.isCritter(en) && posse.of(en) == null) en.remove();
            }
        }, this);
        getServer().getScheduler().runTaskTimer(this, posse, Posse.FOLLOW_TICKS, Posse.FOLLOW_TICKS);
        getServer().getScheduler().runTaskTimer(this, coaxing, Coaxing.PERIOD, Coaxing.PERIOD);
        getServer().getScheduler().runTaskTimer(this, squads::saveAll, 20L * 300, 20L * 300);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> e.registrar().register(
            Commands.literal("mobsquad")
                .requires(src -> src.getSender().hasPermission("mobsquad.use"))
                .executes(this::help)
                .then(Commands.literal("list").executes(this::list))
                .then(Commands.literal("hints").executes(this::hints))
                .then(Commands.literal("dinnerbell").executes(this::giveBell))
                .then(Commands.literal("bunkhouse").requires(src -> src.getSender().hasPermission("mobsquad.admin"))
                    .then(Commands.literal("add").executes(c -> bunkhouse(c, true)))
                    .then(Commands.literal("remove").executes(c -> bunkhouse(c, false))))
                .then(Commands.literal("give").requires(src -> src.getSender().hasPermission("mobsquad.admin"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("critter", StringArgumentType.word()).suggests((c, b) -> {
                            for (Species s : Species.values()) if (s.id().startsWith(b.getRemainingLowerCase())) b.suggest(s.id());
                            return b.buildFuture();
                        }).executes(this::give))))
                .then(Commands.literal("vittles").requires(src -> src.getSender().hasPermission("mobsquad.admin"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(c -> giveVittles(c, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(c -> giveVittles(c, c.getArgument("amount", Integer.class))))))
                .then(Commands.literal("xp").requires(src -> src.getSender().hasPermission("mobsquad.admin"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1_000_000)).executes(this::xp))))
                .then(Commands.literal("reload").requires(src -> src.getSender().hasPermission("mobsquad.admin")).executes(this::reload))
                .build(),
            "Your critters, the dinnerbell, and admin tools for Bunkhouses.", List.of("ms")));
    }

    @Override
    public void onDisable() {
        if (coaxing != null) coaxing.stop();
        if (squads == null) return;
        if (squads.posse() != null) squads.posse().backEveryone();
        squads.saveAll();
    }

    private static void say(CommandContext<CommandSourceStack> c, String text, NamedTextColor color) {
        c.getSource().getSender().sendMessage(Component.text(text, color));
    }

    private int help(CommandContext<CommandSourceStack> c) {
        say(c, "MobSquad: coax a mob and it waits in your Bunkhouse. Right-click a Bunkhouse to send critters out and call them back.", NamedTextColor.GOLD);
        say(c, "/mobsquad list shows your critters. /mobsquad dinnerbell gives you a bell that calls them all home.", NamedTextColor.GRAY);
        say(c, "/mobsquad hints tells you how to coax each critter. Vittles perk up a tuckered-out critter on the spot.", NamedTextColor.GRAY);
        return Command.SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> c) {
        if (!(c.getSource().getExecutor() instanceof Player p)) {
            say(c, "Only a player has critters.", NamedTextColor.YELLOW);
            return 0;
        }
        Wrangler w = squads.get(p);
        if (w == null) {
            say(c, "Your Bunkhouse list can't be read right now. Ask an admin to check the server log.", NamedTextColor.RED);
            return 0;
        }
        int level = squads.wranglinLevel(p);
        say(c, "Wranglin' level " + level + ", up to " + Levels.posse(level) + " out at once. " + w.critters.size()
            + (w.critters.size() == 1 ? " critter:" : " critters" + (w.critters.isEmpty() ? "." : ":")), NamedTextColor.GOLD);
        long now = System.currentTimeMillis();
        for (Critter cr : w.critters) {
            String state = squads.posse().isOut(cr.id()) ? "out" : cr.resting(now) ? "resting" : "ready";
            say(c, "  " + cr.name() + " (" + cr.species().title().toLowerCase(Locale.ROOT) + ", level " + cr.level() + "), " + state, NamedTextColor.GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int hints(CommandContext<CommandSourceStack> c) {
        int level = c.getSource().getExecutor() instanceof Player p ? squads.wranglinLevel(p) : 100;
        say(c, "How to coax them (Wranglin' level " + level + "):", NamedTextColor.GOLD);
        for (Species s : Species.values()) {
            boolean open = Levels.unlocked(s.tier, level);
            say(c, "  " + s.title() + " (" + s.tier.title + "): " + (open ? s.hint : "from Wranglin' level " + s.tier.level + "."),
                open ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int giveBell(CommandContext<CommandSourceStack> c) {
        if (!(c.getSource().getExecutor() instanceof Player p)) {
            say(c, "Only a player can carry a dinnerbell.", NamedTextColor.YELLOW);
            return 0;
        }
        for (var slot : p.getInventory().getContents()) if (dinnerbell.is(slot)) {
            say(c, "You already have a dinnerbell.", NamedTextColor.YELLOW);
            return 0;
        }
        for (var left : p.getInventory().addItem(dinnerbell.make()).values()) p.getWorld().dropItem(p.getLocation(), left);
        say(c, "Here's your dinnerbell. Right-click it to call your critters home.", NamedTextColor.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private int bunkhouse(CommandContext<CommandSourceStack> c, boolean add) {
        if (!(c.getSource().getExecutor() instanceof Player p)) {
            say(c, "Run this as a player looking at a vault.", NamedTextColor.YELLOW);
            return 0;
        }
        Block b = p.getTargetBlockExact(6);
        if (b == null || b.getType() != Material.VAULT) {
            say(c, "Look at a vault block within 6 blocks first.", NamedTextColor.YELLOW);
            return 0;
        }
        if (add == bunkhouses.is(b)) {
            say(c, add ? "That vault is already a Bunkhouse." : "That vault isn't a Bunkhouse.", NamedTextColor.YELLOW);
            return 0;
        }
        if (add) bunkhouses.mark(b);
        else bunkhouses.unmark(b);
        say(c, add ? "That vault is a Bunkhouse now. Players right-click it to see their own critters." : "That vault is a plain vault again.", NamedTextColor.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private int give(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Species s = Species.byId(c.getArgument("critter", String.class));
        if (s == null) {
            say(c, "No such critter. Press Tab for the list.", NamedTextColor.YELLOW);
            return 0;
        }
        Player p = c.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(c.getSource()).getFirst();
        if (squads.get(p) == null) {
            say(c, p.getName() + "'s Bunkhouse list can't be read right now; see the server log.", NamedTextColor.RED);
            return 0;
        }
        squads.add(p, Critter.fresh(s));
        say(c, "[MobSquad] gave " + p.getName() + " a " + s.title().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private int giveVittles(CommandContext<CommandSourceStack> c, int amount) throws CommandSyntaxException {
        Player p = c.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(c.getSource()).getFirst();
        for (var left : p.getInventory().addItem(vittles.make(amount)).values()) p.getWorld().dropItem(p.getLocation(), left);
        say(c, "[MobSquad] gave " + p.getName() + " " + amount + " vittles.", NamedTextColor.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private int xp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Player p = c.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(c.getSource()).getFirst();
        Wrangler w = squads.get(p);
        if (w == null) {
            say(c, p.getName() + "'s Bunkhouse list can't be read right now; see the server log.", NamedTextColor.RED);
            return 0;
        }
        w.wranglinXp = c.getArgument("amount", Integer.class);
        squads.save(w);
        say(c, "[MobSquad] " + p.getName() + " has " + w.wranglinXp + " Wranglin' experience, level " + Levels.level(w.wranglinXp) + ".", NamedTextColor.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> c) {
        reloadConfig();
        squads.settings(Settings.read(getConfig(), getLogger()));
        say(c, "MobSquad reloaded.", NamedTextColor.GREEN);
        return Command.SINGLE_SUCCESS;
    }
}
