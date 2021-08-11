package io.github.mariusdkm.ultrasonic.api;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import io.github.mariusdkm.ultrasonic.pathing.AStar;
import io.github.mariusdkm.ultrasonic.pathing.Node;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import xyz.wagyourtail.jsmacros.client.api.classes.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;
import xyz.wagyourtail.jsmacros.client.api.sharedclasses.PositionCommon;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.core.library.Library;

import java.util.ArrayList;
import java.util.List;

/**
 * The entry point for pathing in JsMacros
 *
 * @author NotSomeBot
 * @since 0.2.0
 */
@SuppressWarnings("unused")
@Library("Pathing")
public class Pathing extends BaseLibrary {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Draw3D pathBlocks;
    public AStar star;
    public Node node;

    public static List<Node> getPath(Node currentNode) {
        List<Node> path = new ArrayList<>();
        while (currentNode != null) {
            path.add(currentNode);
            currentNode = currentNode.prevNode;
        }
        return path;
    }

    public void abort() {
        if (star == null) return;
        star.stopAsync();
    }

    public boolean pathTo(int x, int y, int z, boolean allowSprint) throws Exception {
        assert mc.player != null;
        // Math.ceil(mc.player.getY()) - 1 gives us the block the player is standing on
        abort();
        star = new AStar(mc.player, new BlockPos(mc.player.getBlockX(), Math.ceil(mc.player.getY()) - 1, mc.player.getBlockZ()), new BlockPos(x, y, z), allowSprint);
        star.startAsync();
        star.awaitTerminated();

        return doPath(star.result);
    }

    private boolean doPath(Optional<Node> result) {
        if (result.isPresent()) {
            node = result.get();
            // Just sneak for 2 ticks at the end, so that we don't fall down
            node.player.applyInput(new PlayerInput(0.0F, 0.0F, 0.0F, 0.0F, false, true, false));
            node.player.applyInput(new PlayerInput(0.0F, 0.0F, 0.0F, 0.0F, false, true, false));
            return true;
        }
        return false;
    }

    public List<PlayerInput> getInputs() {
        return node.player.getInputs();
    }

    public void visualizePath() {
        List<Node> path = getPath(this.node);
        if (FHud.renders.contains(pathBlocks)) {
            synchronized (FHud.renders) {
                FHud.renders.remove(pathBlocks);
            }
        }
        pathBlocks = new Draw3D();
        synchronized (FHud.renders) {
            FHud.renders.add(pathBlocks);
        }
        for (Node node : path) {
            pathBlocks.addPoint(new PositionCommon.Pos3D(node.pos.getX() + 0.5D, node.pos.getY() + 0.5D, node.pos.getZ() + 0.5D), 0.5, 0xde070a);
        }

        // int redValue = (int) (newNode.score > maxScore / 2 ? 1 - 2 * (newNode.score - maxScore / 2) / maxScore : 1.0) * 255;
        // int greenValue = (int) (newNode.score > maxScore / 2 ? 1.0 : 2 * newNode.score / maxScore) * 255;
        // int blueValue = 0;
        // int color = ((redValue & 0xFF) << 16) | ((greenValue & 0xFF) << 8);
        // scoreBlocks.addPoint(new PositionCommon.Pos3D(newNode.pos.getX() + 0.5D, newNode.pos.getY() + 0.5D, newNode.pos.getZ() + 0.5D), 0.5, color);
    }
}
