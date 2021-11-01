package me.murphy.addon.modules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.murphy.addon.n1gger;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.Tag;
import net.minecraft.fluid.FluidState;

public class NewChunks extends Module {
    private final Map<ChunkPos, ChunkInfo> world_chunks = new ConcurrentHashMap<ChunkPos, ChunkInfo>();
    private static final Direction[] search_dirs = new Direction[]{Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP};
    private final SettingGroup sg_general = this.settings.getDefaultGroup();
    private final Setting<Boolean> cfg_remove = this.sg_general.add(new BoolSetting.Builder().name("remove").description("Removes the cached chunks when disabling the module.").defaultValue(false).build());
    private final Setting<Boolean> cfg_entities = this.sg_general.add(new BoolSetting.Builder().name("entities").description("...").defaultValue(false).build());
    private final Setting<Boolean> cfg_use_is_full_chunk = this.sg_general.add(new BoolSetting.Builder().name("isFullChunk()").description("...").defaultValue(false).build());
    private final Setting<Boolean> cfg_accurate_old_chunks = this.sg_general.add(new BoolSetting.Builder().name("accurate-old-chunks").description("...").defaultValue(false).build());
    private final Setting<SettingColor> cfg_color_new_chunks = this.sg_general.add(new ColorSetting.Builder().name("new-chunks-color").description("Color of the chunks that are (most likely) completely new.").defaultValue(new SettingColor(204, 153, 217)).build());
    private final Setting<SettingColor> cfg_color_old_chunks = this.sg_general.add(new ColorSetting.Builder().name("old-chunks-color").description("Color of the chunks that have (most likely) been loaded before.").defaultValue(new SettingColor(230, 51, 51)).build());
    private final SettingGroup sg_dev = this.settings.createGroup("Dev");
    private final Setting<Boolean> cfg_dev_msg = this.sg_dev.add(new BoolSetting.Builder().name("des-msg").description("...").defaultValue(false).build());

    public NewChunks() {
        super(n1gger.Category, "chunk-trails", "Detects completely new chunks using certain traits of them");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        assert (this.mc.world != null);
        if (event.packet instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket)event.packet;
            this.onPacketBlockUpdate(packet.getPos(), packet.getState());
        } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket)event.packet;
            packet.visitUpdates((arg_0, arg_1) -> this.onPacketBlockUpdate(arg_0, arg_1));
        } else if (event.packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket)event.packet;
            ChunkPos cpos = new ChunkPos(packet.getX(), packet.getZ());
            if (this.AlreadyProcessed(cpos)) {
                return;
            }
            this.world_chunks.putIfAbsent(cpos, new ChunkInfo());
            if (!packet.isWritingErrorSkippable()) {
                this.world_chunks.computeIfPresent(cpos, (k, v) -> ((ChunkInfo)v).SetNewNotFull());
            }
            if (this.mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) != null) {
                return;
            }
            WorldChunk chunk = new WorldChunk((World)this.mc.world, cpos, null);
            chunk.loadFromPacket(null, packet.getReadBuffer(), new NbtCompound(), packet.getVerticalStripBitmask());
            int lava_blocks = 0;
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < this.mc.world.getHeight(); ++y) {
                    for (int z = 0; z < 16; ++z) {
                        FluidState fluid = chunk.getFluidState(x, y, z);
                        if (!this.IsOldLava(fluid)) continue;
                        if (lava_blocks > 0) {
                            this.world_chunks.computeIfPresent(cpos, (k, v) -> ((ChunkInfo)v).SetOldLavaAccurate());
                        } else {
                            this.world_chunks.computeIfPresent(cpos, (k, v) -> ((ChunkInfo)v).SetOldLava());
                        }
                        ++lava_blocks;
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        assert (this.mc.cameraEntity != null);
        if (this.cfg_color_old_chunks.get().a > 3) {
            this.world_chunks.entrySet().stream().filter(entry -> ((ChunkInfo)entry.getValue()).IsOld()).map(Map.Entry::getKey).filter(c -> this.mc.cameraEntity.getBlockPos().isWithinDistance((BlockPos)c.getStartPos(), 1024.0)).forEach(c -> this.DrawChunk(event, (ChunkPos)c, this.cfg_color_old_chunks.get()));
        }
        if (this.cfg_color_new_chunks.get().a > 3) {
            this.world_chunks.entrySet().stream().filter(entry -> ((ChunkInfo)entry.getValue()).IsNew()).map(Map.Entry::getKey).filter(c -> this.mc.cameraEntity.getBlockPos().isWithinDistance((BlockPos)c.getStartPos(), 1024.0)).forEach(c -> this.DrawChunk(event, (ChunkPos)c, this.cfg_color_new_chunks.get()));
        }
    }

    @Override
    public void onDeactivate() {
        if (this.cfg_remove.get().booleanValue()) {
            this.world_chunks.clear();
        }
    }

    private boolean IsOldLava(FluidState fluid) {
        return !fluid.isEmpty() && fluid.isIn((Tag)FluidTags.LAVA) && !fluid.isStill();
    }

    private void onPacketBlockUpdate(BlockPos bpos, BlockState state) {
        assert (this.mc.world != null);
        if (state.getFluidState().isEmpty() || state.getFluidState().isStill()) {
            return;
        }
        ChunkPos cpos = new ChunkPos(bpos);
        for (Direction dir : search_dirs) {
            if (!this.mc.world.getBlockState(bpos.offset(dir)).getFluidState().isStill()) continue;
            this.world_chunks.computeIfPresent(cpos, (k, v) -> ((ChunkInfo)v).SetNewLiquid());
            return;
        }
    }

    private boolean AlreadyProcessed(ChunkPos cpos) {
        return this.world_chunks.containsKey((Object)cpos);
    }

    private void DrawChunk(Render3DEvent event, ChunkPos cpos, SettingColor color) {
        Box box = new Box(cpos.getStartPos(), cpos.getStartPos().add(16, 0, 16));
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, new Color(0, 0, 0, 0), color, ShapeMode.Lines, 0);
    }

    private class ChunkInfo {
        boolean new_not_full;
        boolean new_liquid;
        boolean old_lava_accurate;
        boolean old_lava;

        private ChunkInfo() {
        }

        boolean IsNew() {
            return this.NewNotFullChunk() || !this.IsOld() && this.new_liquid;
        }

        boolean IsOld() {
            return this.OldLava();
        }

        boolean IsUnknown() {
            return !this.IsOld() && !this.IsNew();
        }

        private boolean NewNotFullChunk() {
            return (Boolean)NewChunks.this.cfg_use_is_full_chunk.get() != false && this.new_not_full;
        }

        private boolean OldLava() {
            if (((Boolean)NewChunks.this.cfg_accurate_old_chunks.get()).booleanValue()) {
                return this.old_lava_accurate;
            }
            return this.old_lava;
        }

        private ChunkInfo SetNewLiquid() {
            this.new_liquid = true;
            return this;
        }

        private ChunkInfo SetOldLava() {
            this.old_lava = true;
            return this;
        }

        private ChunkInfo SetOldLavaAccurate() {
            this.old_lava_accurate = true;
            return this;
        }

        private ChunkInfo SetNewNotFull() {
            this.new_not_full = true;
            return this;
        }
    }
}

