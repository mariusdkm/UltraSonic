package io.github.mariusdkm.ultrasonic.api;

import io.github.mariusdkm.ultrasonic.pathing.AStar;
import io.github.mariusdkm.ultrasonic.pathing.Node;
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

@SuppressWarnings("unused")
@Library("Pathing")
public class Pathing extends BaseLibrary {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Draw3D pathBlocks;

    private Node node;

    public static List<Node> getPath(Node currentNode) {
        List<Node> path = new ArrayList<>();
        while (currentNode != null) {
            path.add(currentNode);
            currentNode = currentNode.prevNode;
        }
        return path;
    }

    public AStar pathTo(int x, int y, int z, boolean allowSprint) {
        assert mc.player != null;
        return new AStar(mc.player, mc.player.getBlockPos().down(), new BlockPos(x, y, z), allowSprint);
    }

    public List<PlayerInput> getInputs(Node currentNode) {
        return currentNode.player.getInputs();
    }

    public void visualizePath(List<Node> path) {
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
//            scoreBlocks.addLine(node.pos.getX() + 0.5D, node.pos.getY() + 1.5D, node.pos.getZ() + 0.5D, node.prevNode.pos.getX() + 0.5D, node.prevNode.pos.getY() + 1.5D, node.prevNode.pos.getZ() + 0.5D, 0xde070a);
        }

//        int redValue = (int) (newNode.score > maxScore / 2 ? 1 - 2 * (newNode.score - maxScore / 2) / maxScore : 1.0) * 255;
//        int greenValue = (int) (newNode.score > maxScore / 2 ? 1.0 : 2 * newNode.score / maxScore) * 255;
//        int blueValue = 0;
//        int color = ((redValue & 0xFF) << 16) | ((greenValue & 0xFF) << 8);
//        scoreBlocks.addPoint(new PositionCommon.Pos3D(newNode.pos.getX() + 0.5D, newNode.pos.getY() + 0.5D, newNode.pos.getZ() + 0.5D), 0.5, color);

    }

}
