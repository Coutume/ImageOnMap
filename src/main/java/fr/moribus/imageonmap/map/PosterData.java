package fr.moribus.imageonmap.map;

import fr.moribus.imageonmap.image.ImageUtils;
import java.net.URL;

public class PosterData {
    private URL url;
    private int width;
    private int height;
    private ImageUtils.ScalingType scaling;

    public PosterData(URL url, int width, int height, ImageUtils.ScalingType scaling) {
        this.url = url;
        this.width = width;
        this.height = height;
        this.scaling = scaling;
    }

    public PosterData(URL url) {
        new PosterData(url, 0, 0, scaling.NONE);
    }

    public void setURL(java.net.URL url) {
        this.url = url;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setScaling(ImageUtils.ScalingType scaling) {
        this.scaling = scaling;
    }

    public java.net.URL getURL() {
        return this.url;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public ImageUtils.ScalingType getScaling() {
        return this.scaling;
    }

}
