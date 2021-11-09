/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  com.google.common.collect.Streams
 *  net.minecraft.world.Difficulty
 *  net.minecraft.entity.damage.DamageSource
 *  net.minecraft.entity.effect.StatusEffects
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.entity.decoration.EndCrystalEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.world.explosion.Explosion
 *  net.minecraft.world.explosion.Explosion$class_4179
 *  net.minecraft.world.World
 *  net.minecraft.block.BedBlock
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.Block
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.hit.HitResult.Type
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.entity.BlockEntity
 *  net.minecraft.block.entity.BedBlockEntity
 *  net.minecraft.block.BlockState
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.RaycastContext
 *  net.minecraft.world.RaycastContext$FluidHandling
 *  net.minecraft.world.RaycastContext$ShapeType
 *  net.minecraft.entity.attribute.EntityAttributes
 */
package me.bedtrapteam.addon.utils;

import com.google.common.collect.Streams;
import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.modules.atlas.combat.*;
import meteordevelopment.meteorclient.mixininterface.IExplosion;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CrystalUtils {
    private static final Map<Entity, ExposureInfo> exposure_info = new HashMap<Entity, ExposureInfo>();
    public static final float WITHER_POWER = 7.0f;
    public static final float CRYSTAL_POWER = 6.0f;
    public static final float CHARGED_CREEPER_POWER = 6.0f;
    public static final float BED_POWER = 5.0f;
    public static final float TNT_POWER = 4.0f;
    public static final float CREEPER_POWER = 3.0f;
    private static final Explosion explosion = new Explosion(null, null, 0.0, 0.0, 0.0, 6.0f, false, Explosion.DestructionType.DESTROY);
    static boolean dirmo = false;

    public static boolean IsBlockForCrystal(Block block) {
        return block.equals(Blocks.OBSIDIAN) || block.equals(Blocks.BEDROCK);
    }

    public static boolean IsBlockForFort(Block block) {
        return block.getBlastResistance() >= 600.0f;
    }

    public static boolean IsSafeBlockForFort(Block block) {
        assert (mc.world != null);
        return block.getBlastResistance() >= 600.0f && (mc.world.getDimension().isRespawnAnchorWorking() || !block.equals(Blocks.RESPAWN_ANCHOR));
    }

    public static double GetDmgByExplosionDmg(double base_damage, LivingEntity entity, Vec3d source, float power) {
        if (Objects.requireNonNull(mc.world).getDifficulty() == Difficulty.PEACEFUL) {
            return 0.0;
        }
        double resistance_coefficient = 1.0;
        if (entity.hasStatusEffect(StatusEffects.RESISTANCE) && (resistance_coefficient -= (double)(Objects.requireNonNull(entity.getStatusEffect(StatusEffects.RESISTANCE)).getAmplifier() + 1) * 0.2) <= 0.0) {
            return 0.0;
        }
        double damage = base_damage;
        if (damage <= 0.0) {
            return 0.0;
        }
        switch (Objects.requireNonNull(mc.world).getDifficulty()) {
            case EASY: {
                if (!(damage > 2.0)) break;
                damage = damage * 0.5 + 1.0;
                break;
            }
            case HARD: {
                damage *= 1.5;
            }
        }
        float f = 2.0f + (float)Objects.requireNonNull(entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)).getValue() / 4.0f;
        float g = (float)MathHelper.clamp((double)((double)entity.getArmor() - (damage *= resistance_coefficient) / (double)f), (double)((float)entity.getArmor() * 0.2f), (double)20.0);
        damage *= (double)(1.0f - g / 25.0f);
        ((IExplosion)explosion).set(source, 6.0f, false);
        int protLevel = EnchantmentHelper.getProtectionAmount((Iterable)entity.getArmorItems(), (DamageSource)DamageSource.explosion((Explosion)explosion));
        if (protLevel > 20) {
            protLevel = 20;
        }
        return damage *= 1.0 - (double)protLevel / 25.0;
    }

    public static double GetCryDmg(LivingEntity entity, Vec3d crystal) {
        if (Objects.requireNonNull(mc.world).getDifficulty() == Difficulty.PEACEFUL) {
            return 0.0;
        }
        double resistance_coefficient = 1.0;
        if (entity.hasStatusEffect(StatusEffects.RESISTANCE) && (resistance_coefficient -= (double)(Objects.requireNonNull(entity.getStatusEffect(StatusEffects.RESISTANCE)).getAmplifier() + 1) * 0.2) <= 0.0) {
            return 0.0;
        }
        double damage = CrystalUtils.GetExplosionDmg((Entity)entity, crystal, 6.0f);
        if (damage <= 0.0) {
            return 0.0;
        }
        switch (Objects.requireNonNull(mc.world).getDifficulty()) {
            case EASY: {
                if (!(damage > 2.0)) break;
                damage = damage * 0.5 + 1.0;
                break;
            }
            case HARD: {
                damage *= 1.5;
            }
        }
        float f = 2.0f + (float)Objects.requireNonNull(entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)).getValue() / 4.0f;
        float g = (float)MathHelper.clamp((double)((double)entity.getArmor() - (damage *= resistance_coefficient) / (double)f), (double)((float)entity.getArmor() * 0.2f), (double)20.0);
        damage *= (double)(1.0f - g / 25.0f);
        ((IExplosion)explosion).set(crystal, 6.0f, false);
        int protLevel = EnchantmentHelper.getProtectionAmount((Iterable)entity.getArmorItems(), (DamageSource)DamageSource.explosion((Explosion)explosion));
        if (protLevel > 20) {
            protLevel = 20;
        }
        return damage *= 1.0 - (double)protLevel / 25.0;
    }

    public static void Check() {
        //System.out.println("checked in Check");
        if (!dirmo || CrystalUtils.brrrr() == null || !CrystalUtils.brrrr().get(0).equals("Thаts hwid list fоr Atlаs addоn, nvm about this.") || !CrystalUtils.brrrr().get(CrystalUtils.brrrr().size() - 1).equals("Thаts hwid list fоr Atlas addon, nvm аbоut this.")) {
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

    public static ArrayList<String> shit = new ArrayList<>();

    public static void initz() throws IOException {
        bebra();

        for (String s : PacketUtils.drisnya()) {
            if (!brrrr().contains(s) || PacketUtils.drisnya() == null) {
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

        Atlas.addModules(
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
            new VHAutoCrystal()
        );

        dirmo = true;
    }

    public static void bebra() throws IOException {
        URL url = new URL(Utils.unHex("68747470733a2f2f706173746562696e2e636f6d2f7261772f48446a594d465332"));

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            shit.add(line);
        }
    }

    public static ArrayList<String> brrrr() {
        return shit;
    }

    public static double GetBedDmg(LivingEntity entity, Vec3d source) {
        return CrystalUtils.GetDmgByExplosionDmg(CrystalUtils.GetExplosionDmg((Entity)entity, source, 5.0f), entity, source, 5.0f);
    }

    public static double GetBedDmg(LivingEntity entity, BlockPos head) {
        Vec3d source = Utils.BlockPosCenter(head);
        return CrystalUtils.GetBedDmg(entity, source);
    }

    public static double GetExplosionDmg(Entity entity, Vec3d source, float power) {
        float q = power * 2.0f;
        double y = MathHelper.sqrt((float) entity.squaredDistanceTo(source)) / q;
        if (y > 1.0) {
            return 0.0;
        }
        if (entity.getX() == source.x && entity.getEyeY() == source.y && entity.getZ() == source.z) {
            return 0.0;
        }
        double exposure = CrystalUtils.GetExposureFast(source, entity);
        double impact = (1.0 - y) * exposure;
        return (int)((impact * impact + impact) / 2.0 * 7.0 * (double)q + 1.0);
    }

    public static double GetDmgByFinalDmg(Entity entity, double final_dmg, float power) {
        return 0.0;
    }

    public static double GetImpactByDmg(double damage, float power) {
        float q = power * 2.0f;
        return (2.645751311064 * Math.sqrt((double)(8.0f * q) * damage + (double)(7.0f * q * q) - (double)(8.0f * q)) - (double)(7.0f * q)) / (double)(14.0f * q);
    }

    public static double GetExposureByImpact(double impact, double dist, float power) {
        float q = power * 2.0f;
        return (double)q * impact / ((double)q - dist);
    }

    public static double GetExposureByDmg(double damage, double dist, float power) {
        float q = power * 2.0f;
        double impact = (2.645751311064 * Math.sqrt((double)(8.0f * q) * damage + (double)(7.0f * q * q) - (double)(8.0f * q)) - (double)(7.0f * q)) / (double)(14.0f * q);
        return (double)q * impact / ((double)q - dist);
    }

    public static double GetExposureByDmg(LivingEntity entity, Vec3d source, double damage, float power) {
        float q = power * 2.0f;
        double impact = (2.645751311064 * Math.sqrt((double)(8.0f * q) * damage + (double)(7.0f * q * q) - (double)(8.0f * q)) - (double)(7.0f * q)) / (double)(14.0f * q);
        return (double)q * impact / ((double)q - entity.squaredDistanceTo(source));
    }

    public static double GetExposureFast(Vec3d source, Entity entity) {
        ExposureInfo info = CrystalUtils.GetExposureInfo(entity);
        if (!info.is_valid) {
            return 0.0;
        }
        int i = 0;
        int j = 0;
        float k = 0.0f;
        while (k <= 1.0f) {
            boolean k_center;
            boolean k_bord = k == 0.0f || (double)k + info.d > 1.0;
            boolean bl = k_center = (double)k < 0.5 && (double)k + info.d > 0.5;
            if (k_bord || k_center) {
                float l = 0.0f;
                while (l <= 1.0f) {
                    boolean l_center;
                    boolean l_bord = l == 0.0f || (double)l + info.e > 1.0;
                    boolean bl2 = l_center = (double)l < 0.5 && (double)l + info.e > 0.5;
                    if (l_bord || l_center) {
                        float m = 0.0f;
                        while (m <= 1.0f) {
                            boolean m_center;
                            boolean m_bord = m == 0.0f || (double)m + info.f > 1.0;
                            boolean bl3 = m_center = (double)m < 0.5 && (double)m + info.f > 0.5;
                            if ((m_bord || m_center) && (k_bord && l_bord && m_bord || k_center && l_center && m_center)) {
                                double p;
                                double o;
                                Box bbox = entity.getBoundingBox();
                                double n = MathHelper.lerp((double)k, (double)bbox.minX, (double)bbox.maxX);
                                if (entity.world.raycast(new RaycastContext(new Vec3d(n + info.g, o = MathHelper.lerp((double)l, (double)bbox.minY, (double)bbox.maxY), (p = MathHelper.lerp((double)m, (double)bbox.minZ, (double)bbox.maxZ)) + info.h), source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity)).getType() == HitResult.Type.MISS) {
                                    ++i;
                                }
                                ++j;
                            }
                            m = (float)((double)m + info.f);
                        }
                    }
                    l = (float)((double)l + info.e);
                }
            }
            k = (float)((double)k + info.d);
        }
        return (float)i / (float)j;
    }

    private static ExposureInfo GetExposureInfo(Entity entity) {
        if (exposure_info.containsKey((Object)entity)) {
            return exposure_info.get((Object)entity).get(entity.getBoundingBox());
        }
        ExposureInfo info = new ExposureInfo().get(entity.getBoundingBox());
        exposure_info.put(entity, info);
        return info;
    }

    public static double GetHealthReduction() {
        double fall_dmg;
        assert (mc.world != null);
        assert (mc.player != null);
        if (mc.player.getAbilities().creativeMode) {
            return 0.0;
        }
        AtomicReference<Double> best_dmg = new AtomicReference<Double>(0.0);
        CrystalUtils.GetCrystalStream().forEach(entity -> {
            double cry_dmg = CrystalUtils.GetCryDmg((LivingEntity) mc.player, entity.getPos());
            if (cry_dmg > (Double)best_dmg.get()) {
                best_dmg.set(cry_dmg);
            }
        });
        for (Entity entity2 : mc.world.getEntities()) {
            double sword_dmg;
            if (!(entity2 instanceof PlayerEntity) || !Friends.get().shouldAttack((PlayerEntity)entity2) || !(mc.player.getPos().squaredDistanceTo(entity2.getPos()) <= 36.0) || !((sword_dmg = DamageUtils.getSwordDamage((PlayerEntity)entity2, true)) > best_dmg.get())) continue;
            best_dmg.set(sword_dmg);
        }
        if (mc.player.fallDistance > 3.0f && (fall_dmg = (double) mc.player.fallDistance * 0.5) > best_dmg.get()) {
            best_dmg.set(fall_dmg);
        }
        if (!mc.world.getDimension().isBedWorking()) {
            for (BlockEntity blockEntity : meteordevelopment.meteorclient.utils.Utils.blockEntities()) {
                double bed_dmg;
                if (!(blockEntity instanceof BedBlockEntity) || !((bed_dmg = DamageUtils.bedDamage((LivingEntity) mc.player, new Vec3d((double)blockEntity.getPos().getX() + 0.5, (double)blockEntity.getPos().getY() + 0.5, (double)blockEntity.getPos().getZ() + 0.5))) > best_dmg.get())) continue;
                best_dmg.set(bed_dmg);
            }
        }
        return best_dmg.get();
    }

    public static Stream<EndCrystalEntity> GetCrystalStream() {
        assert (mc.world != null);
        return Streams.stream((Iterable) mc.world.getEntities()).filter(e -> e instanceof EndCrystalEntity).map(e -> (EndCrystalEntity)e);
    }

    public static boolean CanPlaceCrystal(BlockPos pos) {
        assert (mc.world != null);
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) {
            return false;
        }
        BlockPos boost = pos.add(0, 1, 0);
        if (!mc.world.isAir(boost)) {
            return false;
        }
        double d = boost.getX();
        double e = boost.getY();
        double f = boost.getZ();
        return mc.world.getOtherEntities(null, new Box(d, e, f, d + 1.0, e + 2.0, f + 1.0)).isEmpty();
    }

    private static class ExposureInfo {
        int last_update = -1;
        double d;
        double e;
        double f;
        double g;
        double h;
        public boolean is_valid;
        BlockPos center = new BlockPos(0, -4, 0);

        private ExposureInfo() {
        }

        public ExposureInfo get(Box bbox) {
            if (this.last_update == TickUtils.GetTickCount()) {
                return this;
            }
            this.Update(bbox);
            return this;
        }

        private void Update(Box bbox) {
            this.d = 1.0 / ((bbox.maxX - bbox.minX) * 2.0 + 1.0);
            this.e = 1.0 / ((bbox.maxY - bbox.minY) * 2.0 + 1.0);
            this.f = 1.0 / ((bbox.maxZ - bbox.minZ) * 2.0 + 1.0);
            this.g = (1.0 - Math.floor(1.0 / this.d) * this.d) / 2.0;
            this.h = (1.0 - Math.floor(1.0 / this.f) * this.f) / 2.0;
            this.is_valid = !(this.d < 0.0 || this.e < 0.0 || this.f < 0.0);
            this.last_update = TickUtils.GetTickCount();
        }
    }
}

