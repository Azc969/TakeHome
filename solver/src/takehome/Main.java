package takehome;

import java.nio.file.*;
import java.util.*;

/** 主入口：读入 json，摆放物体，输出结果 */
public class Main {

    public static void main(String[] args) throws Exception {
        String file = args.length > 0 ? args[0] : "example1.json";
        String text = new String(Files.readAllBytes(Paths.get(file)));
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) Json.parse(text);

        // 1. 解析轮廓
        List<Vec2> bpts = new ArrayList<>();
        for (Object o : (List<Object>) root.get("boundary")) {
            List<Object> p = (List<Object>) o;
            bpts.add(new Vec2((Double) p.get(0), (Double) p.get(1)));
        }
        bpts.remove(bpts.size() - 1); // 末点常与首点重复
        Polygon boundary = new Polygon(bpts);

        // 2. 解析门
        List<Object> d = (List<Object>) root.get("door");
        List<Object> d0 = (List<Object>) d.get(0);
        List<Object> d1 = (List<Object>) d.get(1);
        Vec2 doorA = new Vec2((Double) d0.get(0), (Double) d0.get(1));
        Vec2 doorB = new Vec2((Double) d1.get(0), (Double) d1.get(1));
        boolean inward = Boolean.TRUE.equals(root.get("isOpenInward"));

        // 3. 构建门禁放区
        List<Polygon> obstacles = new ArrayList<>();
        if (inward) {
            obstacles.add(buildDoorObstacle(doorA, doorB, boundary));
        }

        // 4. 解析待放物体，按面积从大到小排序
        Map<String, Object> items = (Map<String, Object>) root.get("algoToPlace");
        List<double[]> list = new ArrayList<>(); // {len, wid, order}
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, Object> e : items.entrySet()) {
            List<Object> sz = (List<Object>) e.getValue();
            double l = (Double) sz.get(0), w = (Double) sz.get(1);
            names.add(e.getKey());
            list.add(new double[]{l, w, l * w});
        }
        Integer[] idx = new Integer[names.size()];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(list.get(b)[2], list.get(a)[2]));

        // 5. 依次摆放
        Solver solver = new Solver(boundary, obstacles);
        for (int k : idx) {
            String name = names.get(k);
            String type = typeOf(name);
            double l = list.get(k)[0], w = list.get(k)[1];
            boolean isFridge = "fridge".equals(type);
            boolean ok = solver.place(name, type, l, w, isFridge);
            if (!ok) {
                System.out.println("不可行：无法摆放 " + name);
                return;
            }
        }

        // 6. 输出结果
        System.out.println("可行");
        for (RectItem r : solver.getPlaced()) {
            System.out.printf("%s center=%s angle=%.1f%n",
                    r.name, r.center, r.angleDeg);
        }
    }

    private static String typeOf(String name) {
        if (name.startsWith("fridge")) return "fridge";
        if (name.startsWith("overShelf")) return "overShelf";
        if (name.startsWith("shelf")) return "shelf";
        if (name.startsWith("iceMaker")) return "iceMaker";
        return "unknown";
    }

    /** 内开门禁放区：以门线段为一边，向多边形内部延伸成 N x N 正方形 */
    private static Polygon buildDoorObstacle(Vec2 a, Vec2 b, Polygon boundary) {
        Vec2 dir = b.sub(a);
        double n = dir.len();
        Vec2 d = dir.norm();
        Vec2 n1 = new Vec2(-d.y, d.x);
        Vec2 n2 = new Vec2(d.y, -d.x);
        // 选择指向多边形内部的方向
        Vec2 mid = a.add(b).mul(0.5);
        Vec2 probe = mid.add(n1.mul(n * 0.1));
        Vec2 inwardN = boundary.contains(probe) ? n1 : n2;
        Vec2 off = inwardN.mul(n);
        List<Vec2> pts = new ArrayList<>();
        pts.add(a);
        pts.add(b);
        pts.add(b.add(off));
        pts.add(a.add(off));
        return new Polygon(pts);
    }
}
