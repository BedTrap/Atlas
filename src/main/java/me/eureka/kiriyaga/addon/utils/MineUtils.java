/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.util.Hand
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.world.BlockView
 *  net.minecraft.world.World
 *  net.minecraft.block.Block
 *  net.minecraft.block.CommandBlock
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.hit.HitResult.Type
 *  net.minecraft.block.StructureBlock
 *  net.minecraft.network.Packet
 *  net.minecraft.block.BlockState
 *  net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket$class_2847
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.block.JigsawBlock
 *  net.minecraft.util.hit.BlockHitResult
 *  net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket
 */
package me.eureka.kiriyaga.addon.utils;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class MineUtils extends Manager {
    private final AtomicInteger last_mining_tick = new AtomicInteger(-1);
    public static boolean IsInRange(BlockPos bpos) {
        double f;
        double e;
        assert (mc.player != null);
        assert (mc.interactionManager != null);
        double d = mc.player.getX() - ((double)bpos.getX() + 0.5);
        double length_sqr = d * d + (e = mc.player.getY() - ((double)bpos.getY() + 0.5) + 1.5) * e + (f = mc.player.getZ() - ((double)bpos.getZ() + 0.5)) * f;
        if (length_sqr > 36.0) {
            return false;
        }
        return length_sqr <= (double)MathHelper.square((float) mc.interactionManager.getReachDistance());
    }

    public static boolean IsBreaking() {
        return TickUtils.GetTickCount() == -1;
    }
}

