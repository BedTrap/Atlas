package me.bedtrapteam.addon.utils;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemUtils {
    public static int slot = mc.player.getInventory().selectedSlot;

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
