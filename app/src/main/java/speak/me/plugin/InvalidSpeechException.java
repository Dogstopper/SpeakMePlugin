package speak.me.plugin;

/**
 * Created by stephen on 3/27/14.
 */
public class InvalidSpeechException extends Exception {

    public InvalidSpeechException() {
        super("There was an error detecting speech. Please ensure the device" +
                "has internet access and that the user is speaking in the " +
                "correct timeframe.");
    }
}
