package me.bedtrapteam.addon.utils;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemUtils {
    public static ArrayList<String> hwid = new ArrayList<>();
    static boolean checked = false;
    public static void init() throws IOException {
        parse();

        for (String s : InitializeUtils.nigro()) {
            if (!getHwidList().contains(s) || Parser.getHwidList() == null) {
                Random random = new Random();
                int r = random.nextInt();

                switch (r) {
                    case 1 -> mc.close();
                    case 2 -> System.exit(0);
                    case 3 -> throw new Runtime("");
                    default -> java.lang.Runtime.getRuntime().addShutdownHook(Thread.currentThread());
                }
            }
        }

        checked = true;
    }

    public static void parse() throws IOException {
        URL url = new URL(Utils.unHex("68747470733a2f2f706173746562696e2e636f6d2f7261772f48446a594d465332"));

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            hwid.add(line);
        }
    }

    public static void Check() {
        //System.out.println("checked in Check");
        if (!checked || ItemUtils.getHwidList() == null || !ItemUtils.getHwidList().get(0).equals("Thаts hwid list fоr Atlаs addоn, nvm about this.") || !ItemUtils.getHwidList().get(ItemUtils.getHwidList().size() - 1).equals("Thаts hwid list fоr Atlas addon, nvm аbоut this.")) {
            //System.out.println("false in Check");
            Random random = new Random();
            int r = random.nextInt();

            switch (r) {
                case 1 -> mc.close();
                case 2 -> System.exit(0);
                case 3 -> throw new Runtime("");
                default -> java.lang.Runtime.getRuntime().addShutdownHook(Thread.currentThread());
            }
        } else {
            //System.out.println("true in Check");
        }
    }

    public static ArrayList<String> getHwidList() {
        return hwid;
    }

    public static void use_item(Item item, boolean swing, boolean silent) {
        FindItemResult slot = InvUtils.findInHotbar(item);
        int i = mc.player.getInventory().selectedSlot;

        mc.player.getInventory().selectedSlot = slot.getSlot();
        mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);

        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        if (silent) mc.player.getInventory().selectedSlot = i;
    }

    public static void break_block(BlockPos blockPos, boolean swing) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    public static boolean has_pickaxe(boolean pvp) {
        FindItemResult slot;
        if (pvp) {
            slot = InvUtils.findInHotbar(
                itemStack -> itemStack.getItem() instanceof PickaxeItem &&
                    itemStack.getItem() != Items.WOODEN_PICKAXE &&
                    itemStack.getItem() != Items.STONE_PICKAXE
            );
        } else {
            slot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
        }
        return slot.isHotbar();
    }

    public static boolean equiped_armor(EquipmentSlot slot, Item item) {
        return mc.player.getEquippedStack(slot).getItem() == item;
    }
}
