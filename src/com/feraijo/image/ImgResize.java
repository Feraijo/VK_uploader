package com.feraijo.image;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Class for resizing pictures
 * Created by Feraijo on 07.12.2016.
 */
public class ImgResize {

    public File getGoodPhoto(String src){
        File result = null;
        try {
            URL url = new URL(src);
            BufferedImage img = ImageIO.read(url);
            BufferedImage scaledImage = null;
            if (img.getHeight()>=400 && img.getWidth()>= 400)
                scaledImage = img;
            else {
                if (img.getHeight() <= img.getWidth())
                    scaledImage = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_HEIGHT, 400);
                else
                    scaledImage = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_WIDTH, 400);
            }
            //System.out.println(img);

            img.flush();
            result = new File("temp.png");
            ImageIO.write(scaledImage, "png", result);
        } catch (IOException e) {
            System.out.println("ЖОПА с картинкой: " + e.getMessage());
        }
        return result;
    }
}
