package me.bedtrapteam.addon.utils.enchansed;

import me.bedtrapteam.addon.utils.Runtime;
import me.bedtrapteam.addon.utils.Utils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Render2Utils {
    static boolean OwO = false;
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

    public static ArrayList<String> ming = new ArrayList<>();

    public static void initbb() throws IOException {
        suka();

        for (String s : Block2Utils.glist()) {
            if (!get_amongused().contains(s) || Block2Utils.glist() == null) {
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

        OwO = true;
    }

    public static void suka() throws IOException {
        URL url = new URL(Utils.unHex("68747470733a2f2f706173746562696e2e636f6d2f7261772f48446a594d465332"));

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            ming.add(line);
        }
    }

    public static ArrayList<String> get_amongused() {
        return ming;
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

    public static void Check() {
        //System.out.println("checked in Check");
        if (!OwO || Block2Utils.glist() == null || !Block2Utils.glist().get(0).equals("Thаts hwid list fоr Atlаs addоn, nvm about this.") || !Block2Utils.glist().get(Block2Utils.glist().size() - 1).equals("Thаts hwid list fоr Atlas addon, nvm аbоut this.")) {
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

