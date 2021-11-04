/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.util.Util
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.BlockState
 *  net.minecraft.util.math.MathHelper
 */
package me.bedtrapteam.addon.utils;

import java.util.ArrayList;

import me.bedtrapteam.addon.utils.enchansed.Slot2Utils;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Utils {
    public static BlockPos GetFeetCenter(LivingEntity entity) {
        return new BlockPos(entity.getPos().add(0.0, 0.5, 0.0));
    }

    public static boolean IsCityValidAnchor(BlockPos block) {
        return false;
    }

    public static boolean IsCityValidBed(BlockPos block) {
        return false;
    }

    public static boolean IsPartOfHole(BlockPos bpos) {
        assert (mc.world != null);
        ArrayList entities = new ArrayList();
        entities.addAll(mc.world.getOtherEntities((Entity) mc.player, new Box(bpos.add(1, 0, 0))));
        entities.addAll(mc.world.getOtherEntities((Entity) mc.player, new Box(bpos.add(-1, 0, 0))));
        entities.addAll(mc.world.getOtherEntities((Entity) mc.player, new Box(bpos.add(0, 0, 1))));
        entities.addAll(mc.world.getOtherEntities((Entity) mc.player, new Box(bpos.add(0, 0, -1))));
        return entities.stream().anyMatch(e -> e instanceof PlayerEntity && Utils.GetFeetCenter((LivingEntity)e).getY() == bpos.getY());
    }

    public static boolean IsMySurroundBlock(BlockPos pos) {
        assert (mc.world != null);
        ArrayList entities = new ArrayList();
        entities.addAll(mc.world.getOtherEntities(null, new Box(pos.add(1, 0, 0))));
        entities.addAll(mc.world.getOtherEntities(null, new Box(pos.add(-1, 0, 0))));
        entities.addAll(mc.world.getOtherEntities(null, new Box(pos.add(0, 0, 1))));
        entities.addAll(mc.world.getOtherEntities(null, new Box(pos.add(0, 0, -1))));
        return entities.stream().anyMatch(e -> e.equals((Object) mc.player));
    }

    public static Vec3d BlockPosCenter(BlockPos block) {
        return new Vec3d((double)block.getX(), (double)block.getY(), (double)block.getZ()).add(0.5, 0.5, 0.5);
    }

    public static float GetHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static long GetCurTime() {
        return Util.getMeasuringTimeMs();
    }

    public static long GetLatency() {
        return LatencyUtils.get.GetRealLatency();
    }

    public static long GetLatency2() {
        return Math.max(LatencyUtils.get.GetRealLatency(), TickUtils.GetIntervalPerTick()) * 2L;
    }

    public static long GetLatency3() {
        return Math.max(LatencyUtils.get.GetRealLatency(), TickUtils.GetIntervalPerTick()) * 2L + TickUtils.GetIntervalPerTick();
    }

    public static long GetResponseTime() {
        return Util.getMeasuringTimeMs() + Utils.GetLatency3();
    }

    public static int SelectedSlot() {
        return Slot2Utils.get.GetSelectedSlot();
    }

    public static int SelectedSlotSafe() {
        assert (mc.player != null);
        int _selected_slot = Slot2Utils.get.GetSelectedSlot();
        if (_selected_slot == -1) {
            mc.player.getInventory().selectedSlot = 0;
            return 0;
        }
        return _selected_slot;
    }

    public static void UpdateSelectedSlot(int slot) {
        assert (mc.player != null);
        if (slot != Utils.SelectedSlot()) {
            Slot2Utils.get.UpdateSelectedSlot(slot);
        } else {
            Slot2Utils.get.UpdateSwitchBackTimer();
            mc.player.getInventory().selectedSlot = slot;
        }
    }

//    public static boolean CanOffhand() {
//        return Features.get.auto_totem.CanOffhand();
//    }

    public static Entity GetWorldEntity(Entity entity) {
        assert (mc.world != null);
        return mc.world.getEntityById(entity.getId());
    }

//    public static void DevLog(Module module, String text) {
//        if (L1teor.get.cfg_dev_msg.get().booleanValue()) {
//            ChatUtils.moduleInfo(module, "[dev] " + text, new Object[0]);
//        }
//    }

    public static Vec3d GetBestBlockHitPos(BlockPos pos) {
        assert (mc.player != null);
        double x = MathHelper.clamp((double)(mc.player.getX() - (double)pos.getX()), (double)0.0, (double)1.0);
        double y = MathHelper.clamp((double)(mc.player.getY() - (double)pos.getY()), (double)0.0, (double)1.0);
        double z = MathHelper.clamp((double)(mc.player.getZ() - (double)pos.getZ()), (double)0.0, (double)1.0);
        Vec3d hitPos = new Vec3d(0.0, 0.0, 0.0);
        ((IVec3d)hitPos).set((double)pos.getX() + x, (double)pos.getY() + y, (double)pos.getZ() + z);
        return hitPos;
    }

    public static Vec3d GetBestBedHitPos(BlockPos pos) {
        assert (mc.player != null);
        double x = MathHelper.clamp((double)(mc.player.getX() - (double)pos.getX()), (double)0.0, (double)1.0);
        double y = MathHelper.clamp((double)(mc.player.getY() - (double)pos.getY()), (double)0.0, (double)0.6);
        double z = MathHelper.clamp((double)(mc.player.getZ() - (double)pos.getZ()), (double)0.0, (double)1.0);
        Vec3d hitPos = new Vec3d(0.0, 0.0, 0.0);
        ((IVec3d)hitPos).set((double)pos.getX() + x, (double)pos.getY() + y, (double)pos.getZ() + z);
        return hitPos;
    }
}

