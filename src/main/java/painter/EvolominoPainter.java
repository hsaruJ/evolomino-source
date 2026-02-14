package painter;

import evolomino.Sample;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class EvolominoPainter {
    private static int cellSizePx = 102;

    public static void paint(Sample s, String pathToSave) {


        BufferedImage resultImage = new BufferedImage(
                s.width * cellSizePx, s.height * cellSizePx, BufferedImage.TYPE_4BYTE_ABGR
        );

        Graphics2D g2d = resultImage.createGraphics();

        g2d.setBackground(Color.GREEN);

        for (int row = 0; row < s.height; ++row) {
            for (int col = 0; col < s.width; ++col) {
                String name = "cellMiniImages/" + s.field[row * s.width + col] + ".png";
                BufferedImage cellImage = null;
                try {
                    cellImage = ImageIO.read(new File(name));
                } catch (IOException e) {
                    System.out.println("Can't open '" + name + "'");
                    continue;
                }

                g2d.drawImage(cellImage, col * cellSizePx, row * cellSizePx, 100, 100,  null);
            }
        }

        g2d.dispose();

        try {
            ImageIO.write(resultImage, "png", new File(pathToSave));
        } catch (IOException e) {
            System.out.println("Can't save to '" + pathToSave + "'");
        }

        System.out.println("Paint was saved as '" + pathToSave + "'");
    }
}
