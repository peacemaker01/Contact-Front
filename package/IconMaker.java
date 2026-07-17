import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Generates assets/icon.ico for Contact Front.
 *
 * Draws a tactical marker motif in the fixed palette: a dark panel, a friendly-blue
 * diamond frame, and a caution/red center. Writes a valid 256x256 32-bit ICO.
 *
 * Run:  javac IconMaker.java && java IconMaker
 */
public final class IconMaker {
    private IconMaker() {}

    public static void main(String[] args) throws Exception {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Background panel
        g.setColor(new Color(0x12, 0x18, 0x1a));
        g.fillRect(0, 0, size, size);

        // Subtle border
        g.setColor(new Color(0x23, 0x30, 0x2a));
        g.fillRect(0, 0, size, 6);
        g.fillRect(0, size - 6, size, 6);
        g.fillRect(0, 0, 6, size);
        g.fillRect(size - 6, 0, 6, size);

        int cx = size / 2, cy = size / 2;
        int r = size / 2 - 30;

        // Friendly-blue diamond frame
        g.setColor(new Color(0x4f, 0x8f, 0xd1));
        g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(
                new int[]{cx, cx + r, cx, cx - r},
                new int[]{cy - r, cy, cy + r, cy},
                4);

        // Hostile-red inner diamond
        int ri = r - 34;
        g.setColor(new Color(0xd1, 0x59, 0x4f));
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(
                new int[]{cx, cx + ri, cx, cx - ri},
                new int[]{cy - ri, cy, cy + ri, cy},
                4);

        // Caution center dot
        g.setColor(new Color(0xd1, 0xa3, 0x4f));
        g.fillOval(cx - 12, cy - 12, 24, 24);

        g.dispose();

        byte[] png = toPng(img);
        // Also emit a PNG (used as app icon on macOS/Linux where helpful).
        try (FileOutputStream fos = new FileOutputStream("assets/icon.png")) {
            fos.write(png);
        }

        byte[] ico = toIco(img);
        try (FileOutputStream fos = new FileOutputStream("assets/icon.ico")) {
            fos.write(ico);
        }
        System.out.println("Wrote assets/icon.ico (" + ico.length + " bytes) and assets/icon.png (" + png.length + " bytes)");
    }

    private static byte[] toPng(BufferedImage img) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    /** Build a 32-bit BGRA ICO from a BufferedImage. */
    private static byte[] toIco(BufferedImage src) throws Exception {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage bgra = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gg = bgra.createGraphics();
        gg.drawImage(src, 0, 0, null);
        gg.dispose();

        int stride = w * 4;
        int xorSize = stride * h;
        int andSize = (w * h + 7) / 8; // 1-bit AND mask, zeroed
        int imageSize = 40 + xorSize + andSize;

        ByteBuffer buf = ByteBuffer.allocate(6 + 16 + imageSize).order(ByteOrder.LITTLE_ENDIAN);

        // ICONDIR
        buf.putShort((short) 0);      // reserved
        buf.putShort((short) 1);      // type = icon
        buf.putShort((short) 1);      // count

        // ICONDIRENTRY
        buf.put((byte) w);            // width (0 => 256)
        buf.put((byte) h);            // height
        buf.put((byte) 0);            // colors
        buf.put((byte) 0);            // reserved
        buf.putShort((short) 1);      // planes
        buf.putShort((short) 32);     // bit count
        buf.putInt(imageSize);        // bytes in resource
        buf.putInt(6 + 16);           // image offset

        // BITMAPINFOHEADER
        buf.putInt(40);               // header size
        buf.putInt(w);
        buf.putInt(h * 2);            // height includes XOR + AND
        buf.putShort((short) 1);      // planes
        buf.putShort((short) 32);     // bit count
        buf.putInt(0);                // BI_RGB
        buf.putInt(xorSize);
        buf.putInt(0);                // x ppm
        buf.putInt(0);                // y ppm
        buf.putInt(0);                // colors used
        buf.putInt(0);                // colors important

        // XOR mask: BGRA, bottom-up row order
        int[] px = new int[w];
        for (int y = h - 1; y >= 0; y--) {
            bgra.getRGB(0, y, w, 1, px, 0, w);
            for (int x = 0; x < w; x++) {
                int v = px[x];
                buf.put((byte) (v & 0xFF));        // B
                buf.put((byte) ((v >> 8) & 0xFF)); // G
                buf.put((byte) ((v >> 16) & 0xFF));// R
                buf.put((byte) ((v >> 24) & 0xFF));// A
            }
        }

        // AND mask: all zeros
        for (int i = 0; i < andSize; i++) buf.put((byte) 0);

        return buf.array();
    }
}
