package io.github.mariusdkm.ultrasonic.pathing;

import io.github.mariusdkm.ultrasonic.pathing.Simple2DAStar;

import java.util.Comparator;

public class NodeComparator implements Comparator<Simple2DAStar.Node> {
    @Override
    public int compare(Simple2DAStar.Node o1, Simple2DAStar.Node o2) {
        return o1.score - o2.score;
    }
}
