package takehome;

import java.util.*;

/** 主求解器：把矩形物体摆放进多边形轮廓，贴墙优先，避开门的禁放区 */
public class Solver {
    private final Polygon boundary;
    private final List<Polygon> obstacles;       // 门禁放区（四边形/多边形）
    private final List<RectItem> placed = new ArrayList<>();
    private final List<Vec2> boundaryPts;

    public Solver(Polygon boundary, List<Polygon> obstacles) {
        this.boundary = boundary;
        this.obstacles = obstacles;
        this.boundaryPts = boundary.pts;
    }

    public List<RectItem> getPlaced() { return placed; }

    /** 尝试放置单个物体，成功返回 true */
    public boolean place(String name, String type, double len, double wid) {
        return place(name, type, len, wid, false);
    }

    /** 尝试放置单个物体；isFridge 时自动挑选开门边（长边），并检查净空 */
    public boolean place(String name, String type, double len, double wid, boolean isFridge) {
        // 物体体积大者优先已在外部排序；这里对两个朝向都试
        // {长, 宽, 角度, 开门边局部索引}
        double[][] orientations = {
            {len, wid, 0, isFridge ? 2 : -1},
            {wid, len, 90, isFridge ? 2 : -1}
        };

        for (double[] ori : orientations) {
            double l = ori[0], w = ori[1], ang = ori[2];
            int openSide = (int) ori[3];

            // 候选位置：沿着每条轮廓边内侧滑动
            for (int i = 0; i < boundaryPts.size(); i++) {
                Vec2 a = boundaryPts.get(i);
                Vec2 b = boundaryPts.get((i + 1) % boundaryPts.size());
                Vec2 edge = b.sub(a);
                double edgeLen = edge.len();
                if (edgeLen < 1e-6) continue;
                Vec2 dir = edge.norm();
                Vec2 inward = inwardNormal(a, dir);

                // 让物体的"背面"贴这条边，中心在边内侧 w/2 处
                // 沿边滑动，尝试多个位置
                int steps = Math.max(2, (int) ((edgeLen - l) / 25.0));
                if (edgeLen < l) steps = 2;
                for (int s = 0; s <= steps; s++) {
                    double t = (edgeLen - l) <= 0 ? (edgeLen / 2) : (l / 2 + (edgeLen - l) * s / steps);
                    Vec2 basePt = a.add(dir.mul(t));           // 物体贴边面的中心投影点
                    Vec2 center = basePt.add(inward.mul(w / 2));
                    RectItem cand = new RectItem(name, type, l, w, center, ang, openSide);
                    if (isFridge) {
                        cand = new RectItem(name, type, l, w, center, ang, pickOpenSide(cand));
                    }
                    if (isValid(cand)) {
                        placed.add(cand);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 判断候选物体是否合法 */
    private boolean isValid(RectItem cand) {
        Vec2[] c = cand.corners();
        // 1. 所有顶点在多边形内
        for (Vec2 p : c) if (!boundary.contains(p)) return false;
        // 2. 边不与多边形边相交（跨越）
        for (int i = 0; i < 4; i++) {
            if (boundary.segmentIntersectsEdges(c[i], c[(i + 1) % 4])) return false;
        }
        // 3. 不与门禁放区重叠
        for (Polygon ob : obstacles) {
            if (polyIntersectRect(ob, cand)) return false;
        }
        // 4. 不与已放物体重叠
        for (RectItem r : placed) {
            if (RectItem.overlap(cand, r)) return false;
        }
        // 5. 冰箱开门边净空检查
        if (cand.openSide >= 0) {
            double clearance = Math.min(cand.length, cand.width);
            if (!hasOpenClearance(cand, clearance)) return false;
        }
        // 6. 不能占用已放冰箱/物体的开门净空区
        for (RectItem r : placed) {
            if (r.openSide >= 0) {
                double cl = Math.min(r.length, r.width);
                if (rectIntersectsClearance(cand, r, cl)) return false;
            }
        }
        return true;
    }

    /** 候选矩形是否与某物体的开门净空区重叠 */
    private boolean rectIntersectsClearance(RectItem cand, RectItem owner, double clearance) {
        Vec2[] e = owner.edge(owner.openSide);
        Vec2 out = owner.outwardNormal(owner.openSide);
        Vec2[] zone = {
            e[0], e[1],
            e[1].add(out.mul(clearance)),
            e[0].add(out.mul(clearance))
        };
        Polygon zonePoly = new Polygon(java.util.Arrays.asList(zone));
        return polyIntersectRect(zonePoly, cand);
    }

    /** 冰箱开门边动态选择：两条长边中，外法线更朝向房间内部的那一条 */
    private int pickOpenSide(RectItem r) {
        double cx = r.center.x, cy = r.center.y;
        Vec2 toCenter = new Vec2(centroid().x - cx, centroid().y - cy).norm();
        // 长边为 0-1 和 2-3
        int[] cands = {0, 2};
        int best = 0; double bestDot = -Double.MAX_VALUE;
        for (int side : cands) {
            Vec2 out = r.outwardNormal(side);
            double d = out.dot(toCenter);
            if (d > bestDot) { bestDot = d; best = side; }
        }
        return best;
    }

    /** 检查物体开门边外侧是否留出 clearance 宽度、且不越界的净空 */
    private boolean hasOpenClearance(RectItem cand, double clearance) {
        Vec2[] e = cand.edge(cand.openSide);
        Vec2 out = cand.outwardNormal(cand.openSide);
        // 门的两个外推端点
        Vec2[] zone = {
            e[0], e[1],
            e[1].add(out.mul(clearance)),
            e[0].add(out.mul(clearance))
        };
        // 净空区不能越出轮廓
        Polygon zonePoly = new Polygon(java.util.Arrays.asList(zone));
        for (Vec2 p : zone) if (!boundary.contains(p)) return false;
        // 净空区不能与其他已放物体重叠
        for (RectItem r : placed) {
            if (polyIntersectRect(zonePoly, r)) return false;
        }
        return true;
    }

    /** 多边形与矩形是否相交（矩形任一点在多边形内，或边相交） */
    private boolean polyIntersectRect(Polygon poly, RectItem rect) {
        Vec2[] c = rect.corners();
        for (Vec2 p : c) if (poly.contains(p)) return true;
        for (Vec2 p : poly.pts) if (insideRect(p, c)) return true;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < poly.pts.size(); j++) {
                Vec2 a = poly.pts.get(j), b = poly.pts.get((j + 1) % poly.pts.size());
                if (Polygon.segSegIntersect(c[i], c[(i + 1) % 4], a, b)) return true;
            }
        }
        return false;
    }

    private boolean insideRect(Vec2 p, Vec2[] c) {
        double sign = 0;
        for (int i = 0; i < 4; i++) {
            Vec2 a = c[i], b = c[(i + 1) % 4];
            double cr = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
            if (Math.abs(cr) < 1e-9) continue;
            if (sign == 0) sign = Math.signum(cr);
            else if (Math.signum(cr) != sign) return false;
        }
        return true;
    }

    /** 求轮廓边 a->dir 的内侧法线（用多边形质心方向判断） */
    private Vec2 inwardNormal(Vec2 a, Vec2 dir) {
        Vec2 n1 = new Vec2(-dir.y, dir.x);
        Vec2 n2 = new Vec2(dir.y, -dir.x);
        Vec2 centroid = centroid();
        Vec2 test = a.add(n1.mul(1.0));
        return polygonContainsForNormal(test) ? n1 : n2;
    }

    private boolean polygonContainsForNormal(Vec2 p) {
        return boundary.contains(p);
    }

    private Vec2 centroid() {
        double sx = 0, sy = 0;
        for (Vec2 p : boundaryPts) { sx += p.x; sy += p.y; }
        int n = boundaryPts.size();
        return new Vec2(sx / n, sy / n);
    }
}
