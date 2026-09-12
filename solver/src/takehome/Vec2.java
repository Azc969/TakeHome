package takehome;

/** 二维向量/点 */
public class Vec2 {
    public final double x, y;
    public Vec2(double x, double y) { this.x = x; this.y = y; }

    public Vec2 add(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
    public Vec2 sub(Vec2 o) { return new Vec2(x - o.x, y - o.y); }
    public Vec2 mul(double k) { return new Vec2(x * k, y * k); }
    public double dot(Vec2 o) { return x * o.x + y * o.y; }
    public double cross(Vec2 o) { return x * o.y - y * o.x; }
    public double len() { return Math.sqrt(x * x + y * y); }
    public Vec2 norm() { double l = len(); return l == 0 ? new Vec2(0, 0) : new Vec2(x / l, y / l); }
    public Vec2 rot(double rad) {
        double c = Math.cos(rad), s = Math.sin(rad);
        return new Vec2(x * c - y * s, x * s + y * c);
    }
    @Override public String toString() { return String.format("(%.4f, %.4f)", x, y); }
}
