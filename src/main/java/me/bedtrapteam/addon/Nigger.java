package me.bedtrapteam.addon;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import me.bedtrapteam.addon.modules.*;
import me.bedtrapteam.addon.modules.hud.NotifyHud;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.utils.misc.Placeholders;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.lang.invoke.MethodHandles;

public class Nigger extends MeteorAddon {
    public static final String ADDON = "n1gger++";
    public static final String VERSION = " 0.2";
    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;

    public static final Category Category = new Category("N1GGER++", Items.WITHER_SKELETON_SKULL.getDefaultStack());

    @Override
	public void onInitialize() {
		// Required when using @EventHandler
		MeteorClient.EVENT_BUS.registerLambdaFactory("me.bedtrapteam.addon", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

		// Modules
        add_module(
            new AutoCityPlus(),
            new AutoEz(),
            new AutoLogin(),
            new AutoMinecart(),
            new BedBomb(),
            new BTSurround(),
            new BurrowAlert(),
            new CevBreaker(),
            new CSGO(),
            new Derp(),
            new ElytraHelper(),
            new FunnyAura(),
            new InstantSneak(),
            new MultiTask(),
            new NewAutoTotem(),
            new NewChunks(),
            new NotifySettings(),
            new OneTap(),
            new PacketFly(),
            new PingSpoof(),
            new PistonAura(),
            new Strafe(),
            new SuperKnockback(),
            new Surround(),
            new TNTAura(),
            new VHCrystalAura()
        );

        // Hud
        HUD hud = Modules.get().get(HUD.class);
        hud.elements.add(new NotifyHud(hud));

        // Discord
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        instance.Discord_Initialize("904809529324212235", handlers, true, null);

        rpc.startTimestamp = System.currentTimeMillis() / 1000L;
        rpc.largeImageKey = "n1gger";
        String largeText = ADDON + VERSION;
        rpc.largeImageText = largeText;
        rpc.details = Placeholders.apply("/ n1gger++ /");
        rpc.state = Placeholders.apply("/ PvP Addon /");

        instance.Discord_UpdatePresence(rpc);
        instance.Discord_RunCallbacks();

        // Discord Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            instance.Discord_ClearPresence();
            instance.Discord_Shutdown();
        }));
	}

    public static void add_module(Module... module) {
        for (Module module1 : module) {
            Modules.get().add(module1);
        }
    }

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(Category);
	}
}
