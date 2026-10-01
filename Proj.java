package uz.dexvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** World -> screen projection computed from the camera (no render-event matrices needed). */
public final class Proj {
    public static float capturedFov = -1f;
    public static boolean awaitFov = true;
    public static float partial = 0f;

    private static double cx, cy, cz;
    private static double fx, fy, fz, rx, ry, rz, ux, uy, uz;
    private static double tanHalf = 1.0, aspect = 1.7;
    private static int sw = 1, sh = 1;

    public static void update(MinecraftClient mc, int w, int h) {
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d p = cam.getPos();
        cx = p.x;
        cy = p.y;
        cz = p.z;
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        fx = -Math.sin(yaw) * Math.cos(pitch);
        fy = -Math.sin(pitch);
        fz = Math.cos(yaw) * Math.cos(pitch);
        rx = -Math.cos(yaw);
        ry = 0;
        rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;
        double fov = capturedFov > 1f ? capturedFov : mc.options.getFov().getValue();
        tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
        aspect = (double) mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());
        sw = w;
        sh = h;
        partial = Math.max(0f, Math.min(1f, (System.nanoTime() - DexVisual.lastTickNs) / 5.0e7f));
    }

    /** Returns false if the point is behind the camera. out = {x, y} in scaled GUI pixels. */
    public static boolean project(double x, double y, double z, double[] out) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double zz = dx * fx + dy * fy + dz * fz;
        if (zz <= 0.05) return false;
        double xx = dx * rx + dy * ry + dz * rz;
        double yy = dx * ux + dy * uy + dz * uz;
        out[0] = sw * 0.5 * (1.0 + (xx / zz) / (tanHalf * aspect));
        out[1] = sh * 0.5 * (1.0 - (yy / zz) / tanHalf);
        return true;
    }

    /** Like project, but points behind the camera are pushed far off-screen in the right direction. */
    public static void projectAny(double x, double y, double z, double[] out) {
        if (project(x, y, z, out)) return;
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double xx = dx * rx + dy * ry + dz * rz;
        double yy = dx * ux + dy * uy + dz * uz;
        double len = Math.hypot(xx, yy);
        if (len < 1e-6) {
            xx = 0;
            yy = -1;
            len = 1;
        }
        out[0] = sw * 0.5 + xx / len * 4000.0;
        out[1] = sh * 0.5 - yy / len * 4000.0;
    }

    private Proj() {}
}
