package me.bedtrapteam.addon.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class RayTraceUtils {
    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean canSee(Entity entity) {
        Vec3d entityEyes = LookCalculator.getEyesPos(entity);
        Vec3d entityPos = entity.getPos();
        return canSee(entityEyes, entityPos);
    }

    public static boolean canSee(Vec3d entityEyes, Vec3d entityPos) {
        Vec3d playerEyes = LookCalculator.getEyesPos(mc.player);

        if (mc.world.raycast(new RaycastContext(playerEyes, entityEyes, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
            return true;
        }

        if (playerEyes.getY() > entityPos.getY()) {
            if (mc.world.raycast(new RaycastContext(playerEyes, entityPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }
}
