package ai.herald.clientmod.stream;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * High-performance framebuffer capture → JPEG encoder.
 * Reuses buffers across frames to minimize GC pressure.
 * Must be called on the Render thread.
 */
public final class FrameCapture {

    private static final float JPEG_QUALITY = 0.70f;
    private static volatile int targetFps = 30;
    private static long lastCaptureNanos = 0;

    // Reusable resources (lazily initialized, render-thread only)
    private static ByteBuffer pixelBuffer;
    private static int lastW, lastH;
    private static BufferedImage image;
    private static ImageWriter jpegWriter;
    private static ImageWriteParam jpegParam;

    private FrameCapture() {}

    public static void setTargetFps(int fps) {
        targetFps = Math.max(1, Math.min(fps, 60));
    }

    public static int getTargetFps() {
        return targetFps;
    }

    public static boolean shouldCapture() {
        long now = System.nanoTime();
        long intervalNanos = 1_000_000_000L / targetFps;
        return (now - lastCaptureNanos) >= intervalNanos;
    }

    /**
     * Capture current framebuffer as JPEG. ~3-8ms for 854x480 on modern GPU.
     */
    public static byte[] captureFrame() {
        try {
            lastCaptureNanos = System.nanoTime();

            Minecraft mc = Minecraft.getInstance();
            int width = mc.getWindow().getWidth();
            int height = mc.getWindow().getHeight();
            if (width <= 0 || height <= 0) return null;

            // Reallocate buffer if window resized
            int pixelCount = width * height;
            if (pixelBuffer == null || width != lastW || height != lastH) {
                pixelBuffer = ByteBuffer.allocateDirect(pixelCount * 4);
                image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                lastW = width;
                lastH = height;
            }

            // Read framebuffer (BGRA is fastest on most GPUs/drivers)
            pixelBuffer.clear();
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

            // Bulk convert to int[] with Y-flip (OpenGL bottom-left → top-left)
            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < height; y++) {
                int srcRow = (height - 1 - y) * width;
                int dstRow = y * width;
                for (int x = 0; x < width; x++) {
                    int idx = (srcRow + x) * 4;
                    int r = pixelBuffer.get(idx) & 0xFF;
                    int g = pixelBuffer.get(idx + 1) & 0xFF;
                    int b = pixelBuffer.get(idx + 2) & 0xFF;
                    pixels[dstRow + x] = (r << 16) | (g << 8) | b;
                }
            }

            // Encode JPEG (reuse writer)
            if (jpegWriter == null) {
                jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
                jpegParam = jpegWriter.getDefaultWriteParam();
                jpegParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpegParam.setCompressionQuality(JPEG_QUALITY);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream(pixelCount / 6);
            jpegWriter.setOutput(new MemoryCacheImageOutputStream(baos));
            jpegWriter.write(null, new IIOImage(image, null, null), jpegParam);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
