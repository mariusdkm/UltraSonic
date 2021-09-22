package io.github.mariusdkm.ultrasonic.pathing;

import net.minecraft.util.math.BlockPos;
import xyz.wagyourtail.jsmacros.client.movement.MovementDummy;

import java.util.Comparator;
import java.util.concurrent.ExecutionException;

public class Node {
    public BlockPos pos;
    public int score;
    public double distTravel;
    public MovementDummy player;
    public Node prevNode = null;

    public Node(BlockPos pos, int score, double distTravel, MovementDummy player) {
        this.pos = pos;
        this.player = player.clone();
        this.score = score;
        this.distTravel = distTravel;
    }

    public boolean isWalkable() {
        try {
            return Caches.WALKABLE.get(pos);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.pos.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return pos.equals(((Node) o).pos);
    }

    @Override
    public String toString() {
        return "Node{" +
                "pos=" + pos +
                ", score=" + score +
                '}';
    }

    static class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(Node o1, Node o2) {
            return o1.score - o2.score;
        }
    }
}
