package speak.me.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Extend this if you want to implement activities in SpeakMe. Typically used for things like
 * settings. This class ensures that the main SpeakMe app starts and stops on time.
 */
public class SpeakMeActivity extends Activity {

    /**
     * Delegates to the super.onCreate(Bundle) method, but also sends out an event broadcast that
     * is intended to stop the SpeakMe app from listening.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent stopIntent = new Intent();
        stopIntent.setAction("event-stop-broadcast");
        sendBroadcast(stopIntent);
    }

    /**
     * Delegates to the super.onResume() method, but also sends out an event broadcast that
     * is intended to stop the SpeakMe app from listening.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Intent stopIntent = new Intent();
        stopIntent.setAction("event-stop-broadcast");
        sendBroadcast(stopIntent);
    }

    /**
     * Delegates to the super.onStop() method, but also sends out an event broadcast that
     * is intended to start the SpeakMe app listener again.
     */
    @Override
    protected void onStop() {
        super.onStop();
        Intent stopIntent = new Intent();
        stopIntent.setAction("event-start-broadcast");
        sendBroadcast(stopIntent);
    }

    /**
     * Delegates to the super.onPause() method, but also sends out an event broadcast that
     * is intended to start the SpeakMe app listener again.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Intent stopIntent = new Intent();
        stopIntent.setAction("event-start-broadcast");
        sendBroadcast(stopIntent);
    }
}
