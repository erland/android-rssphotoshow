package info.isaksson.rssphotoshow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageTransformer {
    public static void transform(Image image) {
        if (image.getUrl().startsWith("http://www.w3wallpapers.com/thumbs/")) {
            Matcher m = Pattern.compile("http://www.w3wallpapers.com/thumbs/(.*)-.*?.jpg").matcher(image.getUrl());
            if (m.find()) {
                String imageKey = m.group(1);
                image.setUrl("http://www.w3wallpapers.com/wallpapers/" + imageKey + "-1600x1200.jpg");
            }
        } else if (image.getUrl().contains("static.flickr.com")) {
            if (image.getUrl().contains("_m.jpg")) {
                image.setUrl(image.getUrl().replaceAll("_m.jpg", "_b.jpg"));
            } else if (image.getUrl().contains("_s.jpg")) {
                image.setUrl(image.getUrl().replaceAll("_s.jpg", "_b.jpg"));
            } else if (image.getUrl().contains("_t.jpg")) {
                image.setUrl(image.getUrl().replaceAll("_t.jpg", "_b.jpg"));
            } else if (image.getUrl().contains("_z.jpg")) {
                image.setUrl(image.getUrl().replaceAll("_z.jpg", "_b.jpg"));
            }
        }
    }
}
