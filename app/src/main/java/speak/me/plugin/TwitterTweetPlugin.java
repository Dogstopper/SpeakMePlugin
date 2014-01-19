package speak.me.plugin;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;
import android.net.Uri;

import twitter4j.auth.AccessToken;

public class TwitterTweetPlugin extends Service {
	
	// CHANGE THIS TO BE A DESCRIPTIVE CATEGORY
	static final String CATEGORY_ADD_IF = "speak.me.category.DEFAULT_PLUGIN_CATEGORY";
    static final String PREFS_FILE = "data";    //Used to save the access tokens


	public int onStartCommand(final Intent intent, int flags, int startId) {
//		Log.d("PLUGIN", "[text:] "  +intent.getExtras().getString("text"));
	
		super.onStartCommand(intent, flags, startId);
		final TwitterHandler th = new TwitterHandler();
		SharedPreferences sp = getSharedPreferences(PREFS_FILE, 0);
		
		String accessToken = sp.getString("access_token", "146879636-eWdQqJUuZHp4QCB5LsoNyml59Pi7ubxoDZLEVB4I");
		String accessSecret = sp.getString("access_secret", "fGmbqYqVQqmBMzXXAx2otI5gaUvGQwjIEkTEmgAwRa1En");
		th.authenticate(accessToken, accessSecret);    //Now TwitterHandler is authorized to access user

        Bundle extras = intent.getExtras();
        String text = "";

        if (extras != null) {
            text = extras.getString("text").substring(12);
        }

//        th.tweet(intent.getExtras().getString("text").substring(12));
        th.tweet(text);

		return START_STICKY;
	}


	public void onDestroy() {
		super.onDestroy();
	}

	
    public boolean isAuthorized() {
        SharedPreferences sp = getSharedPreferences(PREFS_FILE, 0);
        String accessToken = sp.getString("access_token", "");
        String accessSecret = sp.getString("access_secret", "");

        return accessToken.equals("") || accessSecret.equals("") ? false : true;
    }

    public void authorize() {

    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}