package me.eureka.kiriyaga.addon;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import me.eureka.kiriyaga.addon.modules.*;
import me.eureka.kiriyaga.addon.enchansed_utils.Block2Utils;
import meteordevelopment.meteorclient.MeteorAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Placeholders;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.lang.invoke.MethodHandles;

public class Nigger extends MeteorAddon {
    public static final String ADDON = "n1gger++";
    public static final String VERSION = " 0.1";
    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;

    public static final Identifier HITSOUND_ID = new Identifier("nigger:hitsound");
    public static SoundEvent HITSOUND = new SoundEvent(HITSOUND_ID);


    public static final Category Category = new Category("N1GGER++", Items.WITHER_SKELETON_SKULL.getDefaultStack());

    @Override
    public void onInitialize() {
        // Meteor Addon System
        MeteorClient.EVENT_BUS.registerLambdaFactory("me.murphy.addon", (lookupInMethod, klass) ->
            (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        // Sounds
        Registry.register(Registry.SOUND_EVENT, HITSOUND_ID, HITSOUND);

        //Block2Utils.init();

        add_module(
            new AutoEz(),
            new AutoLogin(),
            new BedBomb(),
            new CevBreaker(),
            new CSGO(),
            new ElytraHelper(),
            new NewChunks(),
            new OneTap(),
            new PistonAura(),
            new Surround()
        );

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
