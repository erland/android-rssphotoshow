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
        }
    }
}
