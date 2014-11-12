package net.jingx;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import org.joda.time.DateTimeComparator;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Created by jsc on 09.11.14.
 */
public class JiraParser {

    private static final long DEFAULT_MAX_RESULTS = 100;

    private static Pattern HOURS_AND_MINUTES = Pattern.compile("'(\\d*).hour,.(\\d*).minutes' (auf|protokolliert)");
    private static Pattern HOURS = Pattern.compile("'(\\d*).hours?' (auf|protokolliert)");
    private static Pattern MINUTES = Pattern.compile("'(\\d*).minutes?' (auf|protokolliert)");


    private String url;
    private String username;
    private String password;
    private int stunden;
    private int minuten;
    private Long maxResults;

    public JiraParser(String url, String username, String password, Long maxResults) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxResults = maxResults;
        if(maxResults == null || maxResults == 0){
            this.maxResults = DEFAULT_MAX_RESULTS;
        }
    }

    public JiraParser parse() {
        try {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
            URLConnection openConnection = new URL(url + "/activity?maxResults=" + maxResults + "&streams=user+IS+" + username + "&os_authType=basic&title=undefined").openConnection();
            InputStream is = openConnection.getInputStream();
            if ("gzip".equals(openConnection.getContentEncoding())) {
                is = new GZIPInputStream(is);
            }
            InputSource source = new InputSource(is);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(source);
            Calendar cal = Calendar.getInstance();
            Date dateToFind = cal.getTime();
            for (SyndEntry entry : feed.getEntries()) {
                int i = DateTimeComparator.getDateOnlyInstance().compare(dateToFind, entry.getUpdatedDate());
                if (i == 0) { //matching date
                    String title = entry.getTitle();
                    checkRegexAndCount(title);
                    if (entry.getContents().size() > 0) {
                        String content = entry.getContents().get(0).getValue();
                        checkRegexAndCount(content);
                    }
                } else if (i == 1) {
                    //date < as search date
                    break;
                } else {
                    //find next
                }
            }
            if (minuten >= 60) {
                float restminuten = minuten % 60;
                float xstunden = (minuten - restminuten) / 60;
                stunden = stunden + Math.round(xstunden);
                minuten = Math.round(restminuten);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public String formattedString() {
        return stunden + " Std. " + minuten + " Min.";
    }

    private void checkRegexAndCount(String content) {
        Matcher hoursAndMinutesMatcher = HOURS_AND_MINUTES.matcher(content);
        Matcher hoursMatcher = HOURS.matcher(content);
        Matcher minuteMatcher = MINUTES.matcher(content);
        if (hoursAndMinutesMatcher.find()) {
            stunden = stunden + Integer.parseInt(hoursAndMinutesMatcher.group(1));
            minuten = minuten + Integer.parseInt(hoursAndMinutesMatcher.group(2));
        } else if (hoursMatcher.find()) {
            stunden = stunden + Integer.parseInt(hoursMatcher.group(1));
        } else if (minuteMatcher.find()) {
            minuten = minuten + Integer.parseInt(minuteMatcher.group(1));
        } else {
            //no time found
        }
    }
}
