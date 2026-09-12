package takehome;

import java.util.List;

/** 简单多边形，顶点按顺序（首尾相连） */
public class Polygon {
    public final List<Vec2> pts;
    public Polygon(List<Vec2> pts) { this.pts = pts; }

    /** 点是否在多边形内或边上（射线法） */
    public boolean contains(Vec2 p) {
        boolean in = false;
        int n = pts.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Vec2 a = pts.get(i), b = pts.get(j);
            if (pointOnSegment(p, a, b)) return true;
            boolean inter = ((a.y > p.y) != (b.y > p.y))
                    && (p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x);
            if (inter) in = !in;
        }
        return in;
    }

    /** 线段是否与多边形任一非相邻边相交 */
    public boolean segmentIntersectsEdges(Vec2 p, Vec2 q) {
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Vec2 a = pts.get(i), b = pts.get((i + 1) % n);
            if (segSegIntersect(p, q, a, b)) return true;
        }
        return false;
    }

    public static boolean pointOnSegment(Vec2 p, Vec2 a, Vec2 b) {
        double cross = b.sub(a).cross(p.sub(a));
        if (Math.abs(cross) > 1e-6) return false;
        double dot = p.sub(a).dot(p.sub(b));
        return dot <= 1e-6;
    }

    /** 两线段相交（含端点接触） */
    public static boolean segSegIntersect(Vec2 p1, Vec2 p2, Vec2 p3, Vec2 p4) {
        double d1 = cross(p3, p4, p1);
        double d2 = cross(p3, p4, p2);
        double d3 = cross(p1, p2, p3);
        double d4 = cross(p1, p2, p4);
        if (((d1 > 1e-9 && d2 < -1e-9) || (d1 < -1e-9 && d2 > 1e-9))
                && ((d3 > 1e-9 && d4 < -1e-9) || (d3 < -1e-9 && d4 > 1e-9)))
            return true;
        return false;
    }

    private static double cross(Vec2 a, Vec2 b, Vec2 c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }
}
