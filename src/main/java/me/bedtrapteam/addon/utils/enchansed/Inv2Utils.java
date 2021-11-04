/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.entity.player.PlayerInventory
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.GenericContainerScreenHandler
 *  net.minecraft.screen.slot.SlotActionType
 *  net.minecraft.screen.PlayerScreenHandler
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler
 */
package me.bedtrapteam.addon.utils.enchansed;

import java.util.Objects;

import me.bedtrapteam.addon.utils.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Inv2Utils {
    public static boolean CantClick() {
        assert (mc.player != null);
        return mc.player.currentScreenHandler instanceof CreativeInventoryScreen.CreativeScreenHandler;
    }

    public static boolean CanClickOffhand() {
        assert (mc.player != null);
        return !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler);
    }

    private static void ClickSlot(ScreenHandler handler, int id, int data, SlotActionType type) {
        assert (mc.interactionManager != null);
        mc.interactionManager.clickSlot(handler.syncId, id, data, type, (PlayerEntity) mc.player);
    }

    public static void SwapById(ScreenHandler handler, int id, int idx2) {
        Inv2Utils.ClickSlot(handler, id, idx2, SlotActionType.SWAP);
    }

    public static void SwapById(int id, int idx2) {
        assert (mc.player != null);
        Inv2Utils.SwapById(mc.player.currentScreenHandler, id, idx2);
    }

    public static void Swap(int idx1, int idx2) {
        Inv2Utils.SwapById(Inv2Utils.Idx2Id(idx1), idx2);
    }

    public static void SwapWithOffhand(int idx) {
        assert (mc.player != null);
        Inv2Utils.SwapById((ScreenHandler) mc.player.playerScreenHandler, 45, idx);
    }

    public static void SwapWithOffhandById(int id) {
        Inv2Utils.SwapById(id, 40);
    }

    public static void SwapWithOffhand2(int idx) {
        if (Inv2Utils.CanClickOffhand()) {
            Inv2Utils.SwapWithOffhand(idx);
        } else {
            Inv2Utils.SwapWithOffhandById(Inv2Utils.Idx2Id(idx));
        }
    }

    public static void SwapHands() {
        Inv2Utils.SwapWithOffhandById(Inv2Utils.Idx2Id(Utils.SelectedSlotSafe()));
    }

    public static void SwapHands2() {
        Inv2Utils.SwapWithOffhand(Utils.SelectedSlotSafe());
    }

    public static void SwapWithArmorById(int id, int a) {
        Inv2Utils.SwapById(id, 36 + a);
    }

    public static void SwapWithArmor(int idx, int a) {
        Inv2Utils.SwapWithArmorById(Inv2Utils.Idx2Id(idx), a);
    }

    public static void ClickById(ScreenHandler handler, int id) {
        Inv2Utils.ClickSlot(handler, id, 0, SlotActionType.PICKUP);
    }

    public static void ClickById(int id) {
        assert (mc.player != null);
        Inv2Utils.ClickById(mc.player.currentScreenHandler, id);
    }

    public static void Click(int idx) {
        Inv2Utils.ClickById(Inv2Utils.Idx2Id(idx));
    }

    public static void ClickOffhand() {
        assert (mc.player != null);
        Inv2Utils.ClickById((ScreenHandler) mc.player.playerScreenHandler, 45);
    }

    public static void ShiftClickById(int id, ScreenHandler handler) {
        Inv2Utils.ClickSlot(handler, id, 0, SlotActionType.QUICK_MOVE);
    }

    public static void ShiftClickById(int id) {
        assert (mc.player != null);
        Inv2Utils.ShiftClickById(id, mc.player.currentScreenHandler);
    }

    public static void ShiftClick(int idx) {
        Inv2Utils.ShiftClickById(Inv2Utils.Idx2Id(idx));
    }

    public static int Idx2Id(int idx, ScreenHandler handler) {
        if (handler instanceof PlayerScreenHandler) {
            if (Inv2Utils.IsHotbar(idx)) {
                return idx + 36;
            }
            if (Inv2Utils.IsMain(idx)) {
                return idx;
            }
        } else {
            if (Inv2Utils.IsHotbar(idx)) {
                return idx + handler.slots.size() - 9;
            }
            if (Inv2Utils.IsMain(idx)) {
                return idx + handler.slots.size() - 45;
            }
        }
        return -1;
    }

    public static int Idx2Id(int idx) {
        assert (mc.player != null);
        return Inv2Utils.Idx2Id(idx, mc.player.currentScreenHandler);
    }

    private static boolean IsHotbar(int idx) {
        return PlayerInventory.isValidHotbarIndex((int)idx);
    }

    private static boolean IsMain(int i) {
        return i >= 9 && i < 36;
    }

    public static boolean ClickBlank() {
        if (mc.player == null) {
            return false;
        }
        if (mc.currentScreen != null) {
            return false;
        }
        if (mc.player.getInventory().getMainHandStack().isEmpty()) {
            return false;
        }
        int blank = Inv2Utils.GetBlank();
        if (blank != -1) {
            Inv2Utils.Click(blank);
            return true;
        }
        return false;
    }

    public static int amountInInv(Item item) {
        int quantity = 0;
        for (int i = 0; i < 45; ++i) {
            assert (mc.player != null);
            ItemStack stackInSlot = mc.player.getInventory().getStack(i);
            if (stackInSlot.getItem() != item) continue;
            quantity += stackInSlot.getCount();
        }
        return quantity;
    }

    public static int amount(Item item) {
        int quantity = 0;
        for (int i = 0; i < Objects.requireNonNull(mc.player).currentScreenHandler.slots.size(); ++i) {
            ItemStack stackInSlot = mc.player.getInventory().getStack(i);
            if (stackInSlot.getItem() != item) continue;
            quantity += stackInSlot.getCount();
        }
        return quantity;
    }

    public static int FindItemInInv(Item item) {
        assert (mc.player != null);
        for (int i = 0; i < 36; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() != item) continue;
            return i;
        }
        return -1;
    }

    public static int FindItemInHotbar(Item item) {
        assert (mc.player != null);
        for (int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() != item) continue;
            return i;
        }
        return -1;
    }

    public static boolean HotbarHasBlank() {
        assert (mc.player != null);
        for (int i = 0; i < 9; ++i) {
            if (!mc.player.getInventory().getStack(i).isEmpty()) continue;
            return true;
        }
        return false;
    }

    public static int GetBlank() {
        assert (mc.player != null);
        for (int i = 0; i < 45; ++i) {
            if (!mc.player.getInventory().getStack(i).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    public static void updateSlot(int newSlot) {
        mc.player.getInventory().selectedSlot = newSlot;
    }

    private static void ClickSlot(int id, int button, SlotActionType action)
    {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, action, mc.player);
    }

    public static void ClickSlot(int id) {ClickSlot(id, 0, SlotActionType.PICKUP);}

    public static void SwapSlot(int id, int button) {ClickSlot(id, button, SlotActionType.SWAP);}

    public static int GetFirstHotbarSlotId()
    {
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) return 36;
        return mc.player.currentScreenHandler.slots.size() - 9;
    }
}

