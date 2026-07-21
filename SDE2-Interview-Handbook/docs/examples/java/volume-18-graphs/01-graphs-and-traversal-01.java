import java.util.*;

public class GraphPatterns {
    public static List<Integer> bfs(int n, int[] u, int[] v, int start) {
        List<List<Integer>> g = new ArrayList<>();
        for (int i = 0; i < n; i++) g.add(new ArrayList<>());
        for (int i = 0; i < u.length; i++) {
            g.get(u[i]).add(v[i]);
            g.get(v[i]).add(u[i]);
        }
        boolean[] vis = new boolean[n];
        List<Integer> order = new ArrayList<>();
        ArrayDeque<Integer> q = new ArrayDeque<>();
        vis[start] = true;
        q.offer(start);
        while (!q.isEmpty()) {
            int x = q.poll();
            order.add(x);
            for (int nxt : g.get(x)) {
                if (!vis[nxt]) {
                    vis[nxt] = true;
                    q.offer(nxt);
                }
            }
        }
        return order;
    }
}
