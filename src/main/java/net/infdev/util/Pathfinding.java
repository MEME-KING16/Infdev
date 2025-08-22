package net.infdev.util;

import java.util.*;

public class Pathfinding {
    public static final double[][] MOVES = {
        {  1,  0, 1.0 },              // E
        { -1,  0, 1.0 },              // W
        {  0,  1, 1.0 },              // N
        {  0, -1, 1.0 },              // S
        {  1,  1, Math.sqrt(2) },     // NE
        {  1, -1, Math.sqrt(2) },     // SE
        { -1,  1, Math.sqrt(2) },     // NW
        { -1, -1, Math.sqrt(2) }      // SW
    };

    public static double heuristic(int[] a, int[] b) {
        int dx = Math.abs(a[0] - b[0]);
        int dy = Math.abs(a[1] - b[1]);
        return Math.max(dx, dy); // Chebyshev distance
    }

    private static class Node implements Comparable<Node> {
        int x, y;
        double g, f;
        Node parent;

        Node(int x, int y, double g, double f, Node parent) {
            this.x = x; this.y = y;
            this.g = g; this.f = f; this.parent = parent;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node n = (Node) o;
            return this.x == n.x && this.y == n.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public List<int[]> findPath(int[] start, int[] goal, int[][] grid) {
        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<String, Double> gScore = new HashMap<>();

        Node startNode = new Node(start[0], start[1], 0, heuristic(start, goal), null);
        open.add(startNode);
        gScore.put(start[0] + "," + start[1], 0.0);

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.x == goal[0] && current.y == goal[1]) {
                return reconstruct(current);
            }

            for (double[] move : MOVES) {
                int nx = current.x + (int) move[0];
                int ny = current.y + (int) move[1];
                double cost = move[2];

                if (nx < 0 || ny < 0 || nx >= grid.length || ny >= grid[0].length) continue;
                if (grid[nx][ny] == 1) continue; // obstacle

                double tentative_g = current.g + cost;
                String key = nx + "," + ny;

                if (tentative_g < gScore.getOrDefault(key, Double.POSITIVE_INFINITY)) {
                    gScore.put(key, tentative_g);
                    double f = tentative_g + heuristic(new int[]{nx, ny}, goal);
                    open.add(new Node(nx, ny, tentative_g, f, current));
                }
            }
        }
        return null; // no path
    }

    private List<int[]> reconstruct(Node current) {
        List<int[]> path = new ArrayList<>();
        while (current != null) {
            path.add(new int[]{current.x, current.y});
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }
}
