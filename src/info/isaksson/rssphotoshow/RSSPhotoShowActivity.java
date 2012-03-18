package info.isaksson.rssphotoshow;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import com.bugsense.trace.BugSenseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RSSPhotoShowActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private List<Image> imageUrlList = new ArrayList<Image>();
    private int currentImagePos = -1;
    private Image currentImage = null;
    private Timer nextImageTimer;
    private Timer nextRSSTimer;
    private ActivitySwipeDetector swipeDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        BugSenseHandler.setup(this, "ddb540ec");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean showTitle = sharedPreferences.getBoolean("showtitle", Boolean.TRUE);
        TextView titleView = (TextView) findViewById(R.id.title);
        if (!showTitle) {
            titleView.setVisibility(View.GONE);
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        swipeDetector = new ActivitySwipeDetector(new ActivitySwipeDetector.SwipeListener() {
            @Override
            public void right2left(View v) {
                TextView loadingView = (TextView) findViewById(R.id.loading);
                loadingView.setVisibility(View.VISIBLE);
                setNextImage(false);
            }

            @Override
            public void left2right(View v) {
                TextView loadingView = (TextView) findViewById(R.id.loading);
                loadingView.setVisibility(View.VISIBLE);
                setPrevImage(false);
            }

            @Override
            public void top2bottom(View v) {
                TextView loadingView = (TextView) findViewById(R.id.loading);
                loadingView.setVisibility(View.VISIBLE);
                setPrevImage(false);
            }

            @Override
            public void bottom2top(View v) {
                TextView loadingView = (TextView) findViewById(R.id.loading);
                loadingView.setVisibility(View.VISIBLE);
                setNextImage(false);
            }
        });
        ImageView imageView = (ImageView) findViewById(R.id.image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentImage != null && currentImage.getLink() != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentImage.getLink()));
                    startActivity(browserIntent);
                }
            }
        });
        imageView.setOnTouchListener(swipeDetector);

        refreshImageFlow();
    }

    private void refreshImageFlow() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String imageFlowUrl = sharedPreferences.getString("imageflowurl", null);
        if (imageFlowUrl != null && imageFlowUrl.trim().length() > 0) {
            initializeImageFlow(imageFlowUrl);
        } else {
            TextView textView = (TextView) findViewById(R.id.title);
            textView.setText(getResources().getText(R.string.noimages));
            textView.setVisibility(View.VISIBLE);
            TextView loadingView = (TextView) findViewById(R.id.loading);
            loadingView.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preference) {
        if (preference.equals("imageflowurl")) {
            refreshImageFlow();
        } else if (preference.equals("showtitle")) {
            Boolean showTitle = sharedPreferences.getBoolean("showtitle", Boolean.TRUE);
            TextView titleView = (TextView) findViewById(R.id.title);
            if (showTitle) {
                titleView.setVisibility(View.VISIBLE);
            } else {
                titleView.setVisibility(View.GONE);
            }
        } else if (preference.equals("imagedelay")) {
            setNextImage(0, false);
        } else if (preference.equals("rssdelay")) {
            refreshImageFlow();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_title:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return false;
    }

    private static class RSSParser extends DefaultHandler {
        private List<Image> images = new ArrayList<Image>();
        Stack<String> mediaTitles = new Stack<String>();
        String mediaTitle = null;
        Stack<String> titles = new Stack<String>();
        String link = null;
        String title = null;
        String url = null;
        String credit = null;
        Stack<String> copyrights = new Stack<String>();
        String copyright = null;
        Stack<String> mediaCopyrights = new Stack<String>();
        String mediaCopyright = null;
        String description = null;
        boolean collectCharacters = false;
        StringBuffer characters = new StringBuffer();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ((qName.equals("enclosure") || qName.equals("media:content")) && attributes.getValue("url") != null && (
                    attributes.getValue("type").equals("image/png") ||
                            attributes.getValue("type").equals("image/jpeg") ||
                            attributes.getValue("url").endsWith("jpg") ||
                            attributes.getValue("url").endsWith("jpeg") ||
                            attributes.getValue("url").endsWith("png")
            )) {
                url = attributes.getValue("url");
                if (attributes.getValue("rdf:about") != null) {
                    link = attributes.getValue("rdf:about");
                }
            } else if (qName.equals("item")) {
                description = null;
                url = null;
                if (title != null) {
                    titles.push(title);
                }
                title = null;
                if (mediaTitle != null) {
                    mediaTitles.push(mediaTitle);
                }
                mediaTitle = null;
                credit = null;
                if (mediaCopyright != null) {
                    mediaCopyrights.push(mediaCopyright);
                }
                mediaCopyright = null;
                if (copyright != null) {
                    copyrights.push(copyright);
                }
                copyright = null;
            } else if (qName.equals("link")) {
                collectCharacters = true;
            } else if (qName.equals("title")) {
                collectCharacters = true;
            } else if (qName.equals("description")) {
                collectCharacters = true;
            } else if (qName.equals("media:title")) {
                collectCharacters = true;
            } else if (qName.equals("media:credit")) {
                collectCharacters = true;
            } else if (qName.equals("media:copyright")) {
                collectCharacters = true;
            } else if (qName.equals("copyright")) {
                collectCharacters = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("title")) {
                if (title != null) {
                    titles.push(title);
                }
                title = characters.toString();
                characters.setLength(0);
                collectCharacters = false;
            } else if (qName.equals("link")) {
                link = characters.toString();
                characters.setLength(0);
                collectCharacters = false;
            } else if (qName.equals("description")) {
                description = characters.toString();
                characters.setLength(0);
                collectCharacters = false;
            } else if (qName.equals("media:title")) {
                if (mediaTitle != null) {
                    mediaTitles.push(title);
                }
                mediaTitle = characters.toString();
                characters.setLength(0);
                collectCharacters = false;
            } else if (qName.equals("media:credit")) {
                credit = characters.toString();
                characters.setLength(0);
                collectCharacters = false;
            } else if (qName.equals("media:copyright")) {
                if (mediaCopyright != null) {
                    mediaCopyrights.push(mediaCopyright);
                }
                mediaCopyright = characters.toString();
                characters.setLength(0);
                collectCharacters = false;
            } else if (qName.equals("copyright")) {
                if (copyright != null) {
                    copyrights.push(copyright);
                }
                copyright = characters.toString();
                characters.setLength(0);
                collectCharacters = false;
            } else if (qName.equals("item")) {
                List<String> multipleUrls = new ArrayList<String>();
                List<String> multipleTitles = new ArrayList<String>();
                if (url == null && description != null) {
                    Matcher m = Pattern.compile(".*?<img.*?src=\"(.*?)\"[^>]*title=\"(.*?)\".*?>.*?").matcher(description);
                    while (m.find()) {
                        String link = m.group(1);
                        String linkTitle = m.group(2);
                        if (link.contains("jpg") || link.contains("jpeg") || link.contains("png")) {
                            url = link;
                            title = linkTitle;
                            multipleUrls.add(url);
                            multipleTitles.add(title);
                        }
                    }
                    if (url == null) {
                        m = Pattern.compile(".*?<img.*?src=\"(.*?)\".*?>.*?").matcher(description);
                        while (m.find()) {
                            String link = m.group(1);
                            if (link.contains("jpg") || link.contains("jpeg") || link.contains("png")) {
                                url = link;
                                multipleUrls.add(url);
                            }
                        }
                    }
                }
                if (url != null) {
                    String currentTitle = this.title;
                    if (currentTitle == null && !titles.empty()) {
                        currentTitle = titles.peek();
                    }
                    String currentMediaTitle = this.mediaTitle;
                    if (currentMediaTitle == null && !mediaTitles.empty()) {
                        currentMediaTitle = mediaTitles.peek();
                    }
                    String currentCopyright = this.mediaCopyright;
                    if (currentCopyright == null && !mediaCopyrights.empty()) {
                        currentCopyright = mediaCopyrights.peek();
                    }
                    if (currentCopyright == null) {
                        currentCopyright = copyright;
                        if (currentCopyright == null && !copyrights.empty()) {
                            currentCopyright = copyrights.peek();
                        }
                    }
                    if (multipleUrls.size() > 1) {
                        if (multipleTitles.size() > 1) {
                            while (multipleUrls.size() > 0) {
                                String url = multipleUrls.remove(0);
                                String title = multipleTitles.remove(0);
                                addImage(url, link, currentTitle, title, currentCopyright, credit);
                            }
                        } else {
                            for (String url : multipleUrls) {
                                addImage(url, link, currentTitle, currentMediaTitle, currentCopyright, credit);
                            }
                        }
                    } else {
                        addImage(url, link, currentTitle, currentMediaTitle, currentCopyright, credit);
                    }
                }
                if (title != null && !titles.empty()) {
                    title = titles.pop();
                } else {
                    title = null;
                }
                if (mediaTitle != null && !mediaTitles.empty()) {
                    mediaTitle = mediaTitles.pop();
                } else {
                    mediaTitle = null;
                }
                if (mediaCopyright != null && !mediaCopyrights.empty()) {
                    mediaCopyright = mediaCopyrights.pop();
                } else {
                    mediaCopyright = null;
                }
            }
        }

        private void addImage(String url, String link, String title, String mediaTitle, String copyright, String credit) {
            Image image;
            if (title != null && title.equalsIgnoreCase("no title")) {
                title = null;
            }
            if (mediaTitle != null && mediaTitle.equalsIgnoreCase("no title")) {
                mediaTitle = null;
            }
            if (title != null) {
                if (mediaTitle != null && !title.equals(mediaTitle)) {
                    image = new Image(url, title + ": " + mediaTitle);
                } else {
                    image = new Image(url, title);
                }
            } else if (mediaTitle != null) {
                image = new Image(url, mediaTitle);
            } else {
                image = new Image(url);
            }
            if (copyright != null && credit != null) {
                image.setCopyright(copyright + " (credit to: " + credit);
            } else if (copyright != null) {
                image.setCopyright(copyright);
            } else if (credit != null) {
                image.setCopyright(credit);

            }
            image.setLink(link);
            ImageTransformer.transform(image);
            images.add(image);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (collectCharacters) {
                characters.append(ch, start, length);
            }
        }

        public List<Image> getImages() {
            return images;
        }
    }

    private void initializeImageFlow(String imageFlowUrl) {
        new AsyncTask<String, Void, List<Image>>() {
            @Override
            protected List<Image> doInBackground(String... imageFlowUrls) {
                List<Image> imageUrlList = new ArrayList<Image>();
                HttpClient client = new DefaultHttpClient();
                try {
                    HttpResponse response = client.execute(new HttpGet(imageFlowUrls[0]));
                    HttpEntity entity = response.getEntity();
                    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                    RSSParser rssParser = new RSSParser();
                    parser.parse(entity.getContent(), rssParser);
                    imageUrlList = rssParser.getImages();
                } catch (IOException e) {
                    Log.e(RSSPhotoShowActivity.class.getName(), "Error retrieving " + imageFlowUrls[0], e);
                } catch (ParserConfigurationException e) {
                    Log.e(RSSPhotoShowActivity.class.getName(), "Error configuring XML parser for " + imageFlowUrls[0], e);
                } catch (SAXException e) {
                    Log.e(RSSPhotoShowActivity.class.getName(), "Error parsing XML for " + imageFlowUrls[0], e);
                }
                return imageUrlList;
            }

            @Override
            protected void onPostExecute(List<Image> imageUrlList) {
                Collections.shuffle(imageUrlList);
                RSSPhotoShowActivity.this.imageUrlList = imageUrlList;
                currentImagePos = -1;
                if (nextRSSTimer != null) {
                    nextRSSTimer.cancel();
                }
                if (nextImageTimer != null) {
                    nextImageTimer.cancel();
                }
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(RSSPhotoShowActivity.this);
                Integer delay = Integer.valueOf(sharedPreferences.getString("rssdelay", "900"));
                nextRSSTimer = new Timer();
                nextRSSTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        refreshImageFlow();
                    }
                }, delay * 1000);
                setNextImage(true);
            }
        }.execute(imageFlowUrl);

    }

    private void setPrevImage(boolean shuffle) {
        setNextImage(-1, shuffle);
    }

    private void setNextImage(boolean shuffle) {
        setNextImage(1, shuffle);
    }

    private void setNextImage(int increment, boolean shuffle) {
        final ImageView imageView = (ImageView) findViewById(R.id.image);
        final int height = imageView.getHeight();
        final int width = imageView.getWidth();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean showTitle = sharedPreferences.getBoolean("showtitle", Boolean.TRUE);
        if (!showTitle) {
            findViewById(R.id.title).setVisibility(View.GONE);
        }
        if (imageUrlList.size() > 0) {
            currentImagePos += increment;
            if (currentImagePos >= imageUrlList.size()) {
                currentImagePos = 0;
                if (shuffle) {
                    Collections.shuffle(imageUrlList);
                }
            } else if (currentImagePos < 0) {
                currentImagePos = imageUrlList.size() - 1;
                if (shuffle) {
                    Collections.shuffle(imageUrlList);
                }
            }

            if (nextImageTimer != null) {
                nextImageTimer.cancel();
            }
            if (currentImage == null ||
                    !currentImage.getUrl().equals(imageUrlList.get(currentImagePos).getUrl()) ||
                    sharedPreferences.getBoolean("alwaysrefresh", Boolean.FALSE)) {

                currentImage = imageUrlList.get(currentImagePos);

                new AsyncTask<Image, Void, Drawable>() {
                    private Image image;

                    @Override
                    protected Drawable doInBackground(Image... images) {
                        try {
                            image = images[0];
                            String url = image.getUrl();
                            HttpClient client = new DefaultHttpClient();
                            HttpGet request = new HttpGet(url);
                            HttpResponse response = client.execute(request);
                            HttpEntity entity = response.getEntity();
                            if (entity == null) {
                                return null;
                            }
                            InputStream is = entity.getContent();
                            System.gc();
                            try {
                                //Decode image size
                                BitmapFactory.Options o = new BitmapFactory.Options();
                                o.inJustDecodeBounds = true;

                                BitmapFactory.decodeStream(is, null, o);

                                response = client.execute(request);
                                entity = response.getEntity();
                                if (entity == null) {
                                    return null;
                                }
                                is = entity.getContent();

                                int scale = 1;
                                if (o.outHeight > height || o.outWidth > width) {
                                    scale = Math.max(o.outWidth / width, o.outHeight / height);
                                }
                                if (scale < 1) {
                                    scale = 1;
                                }

                                //Decode with inSampleSize
                                BitmapFactory.Options o2 = new BitmapFactory.Options();
                                o2.inSampleSize = scale;
                                Bitmap b = BitmapFactory.decodeStream(is, null, o2);
                                is.close();

                                Drawable d = new BitmapDrawable(b);
                                System.gc();
                                return d;
                            } catch (OutOfMemoryError e) {
                                System.gc();
                                return null;
                            }
                        } catch (IOException e) {
                            return null;
                        }

                    }

                    @Override
                    protected void onPostExecute(final Drawable drawable) {
                        final ImageView imageView = (ImageView) findViewById(R.id.image);
                        final TextView loadingView = (TextView) findViewById(R.id.loading);
                        final TextView titleView = (TextView) findViewById(R.id.title);
                        final TextView creditView = (TextView) findViewById(R.id.credit);
                        if (drawable != null) {
                            if (imageView.getDrawable() != null && (imageView.getAnimation() == null || !imageView.getAnimation().hasStarted())) {
                                final AlphaAnimation fadeOut = new AlphaAnimation(1.00f, 0.00f);
                                fadeOut.setDuration(1000);
                                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        if (image.getTitle() != null) {
                                            titleView.setText(image.getTitle());
                                        } else {
                                            titleView.setText("");
                                        }
                                        if (image.getCopyright() != null) {
                                            creditView.setText(image.getCopyright());
                                        } else {
                                            creditView.setText("");
                                        }
                                        TextView loadingView = (TextView) findViewById(R.id.loading);
                                        loadingView.setVisibility(View.INVISIBLE);
                                        AlphaAnimation fadeIn = new AlphaAnimation(0.00f, 1.00f);
                                        fadeIn.setDuration(1000);
                                        imageView.setAnimation(fadeIn);
                                        if (titleView.getVisibility() == View.VISIBLE) {
                                            titleView.setAnimation(fadeIn);
                                        }
                                        creditView.setAnimation(fadeIn);
                                        imageView.setImageDrawable(drawable);
                                        System.gc();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {
                                    }
                                });
                                if (titleView.getVisibility() == View.VISIBLE) {
                                    titleView.startAnimation(fadeOut);
                                }
                                if (loadingView.getVisibility() == View.VISIBLE) {
                                    loadingView.startAnimation(fadeOut);
                                }
                                creditView.startAnimation(fadeOut);
                                imageView.startAnimation(fadeOut);
                            } else {
                                loadingView.setVisibility(View.INVISIBLE);
                                AlphaAnimation fadeIn = new AlphaAnimation(0.00f, 1.00f);
                                fadeIn.setDuration(1000);
                                imageView.setAnimation(fadeIn);
                                creditView.setAnimation(fadeIn);
                                if (titleView.getVisibility() == View.VISIBLE) {
                                    titleView.setAnimation(fadeIn);
                                }
                                imageView.setImageDrawable(drawable);
                                if (image.getTitle() != null) {
                                    titleView.setText(image.getTitle());
                                } else {
                                    titleView.setText("");
                                }
                                if (image.getCopyright() != null) {
                                    creditView.setText(image.getCopyright());
                                } else {
                                    creditView.setText("");
                                }
                                System.gc();
                            }
                        } else {
                            imageView.setImageDrawable(null);
                            System.gc();
                        }
                        nextImageTimer = new Timer();
                        if (drawable != null) {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(RSSPhotoShowActivity.this);
                            Integer delay = Integer.valueOf(sharedPreferences.getString("imagedelay", "60"));
                            nextImageTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    setNextImage(true);
                                }
                            }, delay * 1000);
                        } else {
                            nextImageTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    setNextImage(true);
                                }
                            }, 100);
                        }
                    }
                }.execute(currentImage);
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = (ImageView) findViewById(R.id.image);
                        TextView loadingView = (TextView) findViewById(R.id.loading);
                        loadingView.setVisibility(View.INVISIBLE);
                        if (imageView.getDrawable() == null) {
                            TextView textView = (TextView) findViewById(R.id.title);
                            textView.setText(getResources().getText(R.string.noimages));
                            textView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) findViewById(R.id.title);
                    textView.setText(getResources().getText(R.string.noimages));
                    textView.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}
