package com.airxiechao.axcboot.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {

    /**
     * 保存图片
     * @param imageBytes
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String saveJpg(byte[] imageBytes, String fileName) throws IOException {

        try( InputStream in = new ByteArrayInputStream(imageBytes) ){
            BufferedImage image = ImageIO.read(in);

            File file = new File(fileName);
            ImageIO.write(image, "jpg", file);

            return file.getCanonicalPath();
        }

    }
}
