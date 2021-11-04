package me.bedtrapteam.addon.utils.enchansed;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Render2Utils {
    public static void thick_box(Render3DEvent event, BlockPos pos, Color side_color, Color line_color, ShapeMode shapeMode) {
        if (shapeMode.lines()) {
            // Sides
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ() + 0.02, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 0.02, pos.getY() + 1, pos.getZ(), line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 0.02, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 0.98, pos.getY() + 1, pos.getZ(), line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 1, pos.getZ() + 0.98, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 0.02, pos.getY() + 1, pos.getZ() + 1, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 0.98, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 0.98, pos.getY() + 1, pos.getZ() + 1, line_color, line_color);

            // Up
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 0.98, pos.getZ(), line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 0.02, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX(), pos.getY() + 0.98, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 0.02, pos.getZ() + 1, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.98, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getZ() + 0.98, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 0.98, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + 0.98, pos.getZ() + 1, line_color);

            // Down
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.02, pos.getZ(), line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + 0.02, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 0.02, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 0.02, pos.getZ() + 1, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.02, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getZ() + 0.98, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.02, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 0.98, pos.getZ() + 1, line_color);
        }

        if (shapeMode.sides()) {
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ(), side_color, side_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ() + 1, side_color, side_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ(), side_color, side_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 1, pos.getZ() + 1, side_color, side_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 1, side_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + 1, side_color);
        }
    }

    public static void thick_bed(Render3DEvent event, BlockPos pos, Color side_color, Color line_color, ShapeMode shapeMode) {
        if (shapeMode.lines()) {
            // Sides
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 0.6, pos.getZ() + 0.02, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 0.02, pos.getY() + 0.6, pos.getZ(), line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.6, pos.getZ() + 0.02, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 0.98, pos.getY() + 0.6, pos.getZ(), line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 0.6, pos.getZ() + 0.98, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 0.02, pos.getY() + 0.6, pos.getZ() + 1, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.6, pos.getZ() + 0.98, line_color, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 0.98, pos.getY() + 0.6, pos.getZ() + 1, line_color, line_color);

            // Up
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 0.6, pos.getZ(), pos.getX() + 1, pos.getY() + 0.58, pos.getZ(), line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 0.6, pos.getZ(), pos.getX() + 1, pos.getZ() + 0.02, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 0.6, pos.getZ(), pos.getX(), pos.getY() + 0.58, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 0.6, pos.getZ(), pos.getX() + 0.02, pos.getZ() + 1, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 0.6, pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.58, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 0.6, pos.getZ() + 1, pos.getX() + 1, pos.getZ() + 0.98, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY() + 0.6, pos.getZ(), pos.getX() + 1, pos.getY() + 0.58, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX() + 1, pos.getY() + 0.6, pos.getZ(), pos.getX() + 0.98, pos.getZ() + 1, line_color);

            // Down
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.02, pos.getZ(), line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + 0.02, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 0.02, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 0.02, pos.getZ() + 1, line_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.02, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getZ() + 0.98, line_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.02, pos.getZ() + 1, line_color, line_color);
            event.renderer.quadHorizontal(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 0.98, pos.getZ() + 1, line_color);
        }

        if (shapeMode.sides()) {
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.6, pos.getZ(), side_color, side_color);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 0.6, pos.getZ() + 1, side_color, side_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.6, pos.getZ(), side_color, side_color);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 0.6, pos.getZ() + 1, side_color, side_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 0.6, pos.getZ(), pos.getX() + 1, pos.getZ() + 1, side_color);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + 1, side_color);
        }
    }

    public static void render_bed(Render3DEvent event, BlockPos pos, Direction direction, Color side_color, Color line_color, ShapeMode shape_mode) {
        switch (direction) {
            case NORTH -> {
                thick_bed(event, pos, side_color, line_color, shape_mode);
                thick_bed(event, pos.south(), side_color, line_color, shape_mode);
            }
            case SOUTH -> {
                thick_bed(event, pos, side_color, line_color, shape_mode);
                thick_bed(event, pos.north(), side_color, line_color, shape_mode);
            }
            case EAST -> {
                thick_bed(event, pos, side_color, line_color, shape_mode);
                thick_bed(event, pos.west(), side_color, line_color, shape_mode);
            }
            case WEST -> {
                thick_bed(event, pos, side_color, line_color, shape_mode);
                thick_bed(event, pos.east(), side_color, line_color, shape_mode);
            }
        }
    }
}

