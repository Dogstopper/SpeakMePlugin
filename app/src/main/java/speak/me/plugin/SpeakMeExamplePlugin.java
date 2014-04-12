package speak.me.plugin;

import android.util.Log;

/**
 * Created by Stephen on 3/6/14.
 */
public class SpeakMeExamplePlugin extends SpeakMePlugin{

    @Override
    public void performAction(String text) {
        Log.d("SPeakMeEXamplePlugin", "performAction() called");
        this.speak("This is example text...");
        this.speak("I am going to close up shop now.");
        Log.d("SPeakMeEXamplePlugin", "performAction() finished");

    }

}
