package takehome;

/** 一个可旋转的矩形物体 */
public class RectItem {
    public final String name;
    public final String type;      // fridge, shelf, overShelf, iceMaker
    public final double length;    // 尺寸1
    public final double width;     // 尺寸2
    public final double angleDeg;  // 0 或 90
    public final Vec2 center;

    /** 开门边的局部索引：0/2 是长边，1/3 是短边，-1 表示没有开门边 */
    public final int openSide;

    public RectItem(String name, String type, double length, double width,
                    Vec2 center, double angleDeg) {
        this(name, type, length, width, center, angleDeg, -1);
    }

    public RectItem(String name, String type, double length, double width,
                    Vec2 center, double angleDeg, int openSide) {
        this.name = name; this.type = type;
        this.length = length; this.width = width;
        this.center = center; this.angleDeg = angleDeg;
        this.openSide = openSide;
    }

    /** 返回指定局部边在全局坐标系下的两个端点 */
    public Vec2[] edge(int side) {
        Vec2[] c = corners();
        return new Vec2[]{ c[side], c[(side + 1) % 4] };
    }

    /** 指定边的朝外方向单位法线（全局坐标） */
    public Vec2 outwardNormal(int side) {
        Vec2[] e = edge(side);
        Vec2 d = e[1].sub(e[0]).norm();
        Vec2 n1 = new Vec2(-d.y, d.x);
        // 用中心判断哪个方向朝外
        Vec2 mid = e[0].add(e[1]).mul(0.5);
        Vec2 toCenter = center.sub(mid).norm();
        return (n1.dot(toCenter) < 0) ? n1 : new Vec2(d.y, -d.x);
    }

    /** 返回四个角点（按顺序） */
    public Vec2[] corners() {
        double hx = length / 2, hy = width / 2;
        Vec2[] local = {
            new Vec2(-hx, -hy), new Vec2(hx, -hy),
            new Vec2(hx, hy), new Vec2(-hx, hy)
        };
        double rad = Math.toRadians(angleDeg);
        Vec2[] out = new Vec2[4];
        for (int i = 0; i < 4; i++) out[i] = local[i].rot(rad).add(center);
        return out;
    }

    /** 分离轴定理：两个矩形是否重叠 */
    public static boolean overlap(RectItem a, RectItem b) {
        Vec2[] ca = a.corners(), cb = b.corners();
        Vec2[] aa = axes(ca), ab = axes(cb);
        Vec2[] all = { aa[0], aa[1], ab[0], ab[1] };
        for (Vec2 ax : all) {
            double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE;
            double minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;
            for (Vec2 p : ca) { double d = p.dot(ax); minA = Math.min(minA, d); maxA = Math.max(maxA, d); }
            for (Vec2 p : cb) { double d = p.dot(ax); minB = Math.min(minB, d); maxB = Math.max(maxB, d); }
            if (maxA <= minB + 1e-6 || maxB <= minA + 1e-6) return false;
        }
        return true;
    }

    private static Vec2[] axes(Vec2[] c) {
        Vec2 e1 = c[1].sub(c[0]).norm();
        Vec2 e2 = c[3].sub(c[0]).norm();
        return new Vec2[]{
            new Vec2(-e1.y, e1.x), new Vec2(-e2.y, e2.x)
        };
    }
}
