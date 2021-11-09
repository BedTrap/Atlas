package me.bedtrapteam.addon;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import me.bedtrapteam.addon.modules.atlas.combat.*;
import me.bedtrapteam.addon.modules.atlas.misc.*;
import me.bedtrapteam.addon.modules.hud.NotifyHud;
import me.bedtrapteam.addon.modules.konas.*;
import me.bedtrapteam.addon.utils._Checker;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.utils.misc.Placeholders;
import net.minecraft.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Atlas extends MeteorAddon {
    public static final String ADDON = "Atlas";
    public static final String VERSION = " 0.2";
    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;

    public static final Category Combat = new Category("Combato", Items.WITHER_ROSE.getDefaultStack());
    public static final Category Misc = new Category("El Misco", Items.POPPY.getDefaultStack());
    public static final Category Konas = new Category("Konas", Items.AMETHYST_SHARD.getDefaultStack());

    @Override
    public void onInitialize() {
        // Auth
        _Checker.Start();

        // Modules
        add_to_atlas(
            // Combat
            new AutoCityRewrite(),
            new AutoEz(),
            new AutoMinecart(),
            new AutoTotemRewrite(),
            new BedBomb(),
            new BTSurround(),
            new CevBreaker(),
            new FunnyAura(),
            new MultiTask(),
            new PistonAura(),
            new SuperKnockback(),
            new SurroundRewrite(),
            new TNTAura(),
            new VHAutoCrystal(),
            // Misc
            new AutoLogin(),
            new ChestExplorer(),
            new CSGO(),
            new Derp(),
            new ElytraHelper(),
            new HandAnimations(),
            new InstantSneak(),
            new NewChunks(),
            new NotifySettings(),
            new PacketFly(),
            new PingSpoof(),
            new Strafe(),
            new ThirdHand()
        );

        add_to_konas(
            new KAntiSpam(),
            new KAntiSurround(),
            new KAutoCrystal(),
            new KHoleFill(),
            new KOffhand(),
            new KSelfFill(),
            new KSpeed(),
            new KSprint(),
            new KSurround()
        );

        // Hud
        HUD hud = Modules.get().get(HUD.class);
        hud.elements.add(new NotifyHud(hud));

        // Auth again
        _Checker.Check();

        // Discord
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        instance.Discord_Initialize("904809529324212235", handlers, true, null);

        rpc.startTimestamp = System.currentTimeMillis() / 1000L;
        rpc.largeImageKey = "strong";
        String largeText = ADDON + VERSION;
        rpc.largeImageText = largeText;
        rpc.details = Placeholders.apply("> " + mc.getSession().getUsername());
        rpc.state = Placeholders.apply("> We do a little ownage.");

        instance.Discord_UpdatePresence(rpc);
        instance.Discord_RunCallbacks();

        // Discord Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Config.get().save();
            instance.Discord_ClearPresence();
            instance.Discord_Shutdown();
        }));
    }

    public static void add_to_atlas(Module... module) {
        for (Module module1 : module) {
            Modules.get().add(module1);
        }
    }

    public static void add_to_konas(Module... module) {
        for (Module module1 : module) {
            Modules.get().add(module1);
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Combat);
        Modules.registerCategory(Misc);
        Modules.registerCategory(Konas);
    }
}
