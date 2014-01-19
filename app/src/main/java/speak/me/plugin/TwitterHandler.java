package speak.me.plugin;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;
import android.util.Log;

public final class TwitterHandler {
    public static final String CONSUMER_KEY = "Tnts4MdH0J3EEmKd2QlRg";
    public static final String CONSUMER_SECRET = "Ru0Uzukxj6tFFHGLaNTv7MGCBSIiXts9LHUmED7DY";

    public String m_consumerKey;
    public String m_consumerSecret;
    public String m_accessToken;
    public String m_accessSecret;
    public RequestToken m_requestToken;
    Twitter m_twitter;

    TwitterHandler() {
        m_consumerKey = CONSUMER_KEY;
        m_consumerSecret = CONSUMER_SECRET;
        m_accessToken = "";
        m_accessSecret = "";

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(m_consumerKey)
                .setOAuthConsumerSecret(m_consumerSecret);

        TwitterFactory tf = new TwitterFactory(cb.build());
        m_twitter  = tf.getInstance();
    }

    TwitterHandler(String accessToken, String accessSecret) {
        m_consumerKey = CONSUMER_KEY;
        m_consumerSecret = CONSUMER_SECRET;

        authenticate(accessToken, accessSecret);
    }

    public void authenticate(String token, String secret) {
        m_accessToken = token;
        m_accessSecret = secret;

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(m_consumerKey)
                .setOAuthConsumerSecret(m_consumerSecret)
                .setOAuthAccessToken(m_accessToken)
                .setOAuthAccessTokenSecret(m_accessSecret);

        TwitterFactory tf = new TwitterFactory(cb.build());
        m_twitter  = tf.getInstance();
    }

    public void authenticate(AccessToken at) {
        authenticate(at.getToken(), at.getTokenSecret());
    }

    public String getAuthorizationUrl() {
        String url = "";
        try {
            // get request token.
            // this will throw IllegalStateException if access token is already available
            m_requestToken = m_twitter.getOAuthRequestToken();
            url = m_requestToken.getAuthorizationURL();
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            System.exit(-1);
        }

        return url;
    }

    //Returns null if user did not authorize app or entered incorrect pin
    public AccessToken getAccessTokenUsingPin(String pin) {
        AccessToken accessToken = null;

        try {
            if (pin.length() > 0) {
                accessToken = m_twitter.getOAuthAccessToken(m_requestToken, pin);
            }
        } catch (TwitterException te) {
            if (401 == te.getStatusCode()) {
                Log.d("TwitterHandler", "Unable to get the access token.");
            } else {
                //ERROR
            }

            return null;
        }

        return accessToken;
    }

//    public void tweet(String text) {
//        try {
//            Status status = m_twitter.updateStatus(text);
//        } catch(TwitterException te) {
//            te.printStackTrace();
//            System.out.println("Failed to get timeline: " + te.getMessage());
//            System.exit(-1);
//        }
//    }

    public void tweet(String text) {
        class TweetTask extends AsyncTask<String, Void, Boolean> {

            private Exception exception;

            protected Boolean doInBackground(String... tweet) {
                try {
                    twitter4j.Status status = m_twitter.updateStatus(tweet[0]);
                } catch (Exception e) {
                    this.exception = e;
                    Log.e("SPEAK ME", "ERROR: " + e);
                    return false;
                }
                return true;
            }
        }

        new TweetTask().execute(text);
    }

    public boolean parseCommand(String command) {
        //Check if the user wants to tweet something
        if(command.toLowerCase().startsWith("tweet")) {
            tweet(command.substring(6));
        }

        return true;
    }
}