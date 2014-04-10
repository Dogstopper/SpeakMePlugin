package speak.me.plugin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * Controls the SpeakMePlugin's text-to-speech reader, and deals with the utterances
 * necessary to create a blocking situation.
 */
public class SpeakMeReader {
	private TextToSpeech tts;
    private Service parent;

    private boolean speaking = false;
    private String utteranceID = "utterance_id_speech";

    /**
     * Creates a new android.speech.tts.TextToSpeech object and sets it up. This involves making a
     * new UtteranceListener, that will set a member boolean to true/false based on whether the
     * tts is currently speaking or is finished.
     * @param parent Refers to the owner of this class, so it's attrivutes can be used.
     */
    public SpeakMeReader(Service parent) {
        this.parent = parent;
        tts = new TextToSpeech(parent.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    if (tts.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
                        tts.setLanguage(Locale.US);

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

            @Override
            public void onStart(String s) {
                if (s.equalsIgnoreCase(utteranceID)) {
                    Log.d("TTS", "Starting");
                    speaking = true;
                }
            }

            @Override
            public void onDone(String s) {
                if (s.equalsIgnoreCase(utteranceID)) {
                    Log.d("TTS", "Done");
                    speaking = false;
                }
            }

            @Override
            public void onError(String s) {
                if (s.equalsIgnoreCase(utteranceID)) {
                    speaking = false;
                }
                Log.e("TTS", "onError()");
            }
        });
    }

    /**
     * Returns true if this service is not currently speaking, and it returns false if
     * the utterance listener has been started but not finished.
     * @return true if not currently speaking, false otherwise.
     */
    public boolean isDoneSpeaking() {
        return !speaking;
    }

    /**
     * Detects whether the input is valid, and if so, will invoke the text-to-speech
     * reader. Its helper method will also explicitly ensure that the system volume is unmuted,
     * as the main SpeakMe app may mute it temporarily.
     * @param text The text to pass to the tts reader
     */
    public void invokeTTS(String text) {
        if (text != null && text.trim().length() != 0) {
            speaking = true;
            textToSpeech(text);
        }
    }

    /**
     * Adds the given text to the speech queue after ensuring that the system volume is unmuted.
     * @param text The text to pass to the tts reader
     */
    private void textToSpeech(String text) {
        // Unmute both main streams
        AudioManager am = (AudioManager)parent.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_SYSTEM,false);
        am.setStreamMute(AudioManager.STREAM_MUSIC,false);

        // Use
        HashMap<String, String> voiceMap = new HashMap();
        voiceMap.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS,"true");
        voiceMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, this.utteranceID);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, voiceMap);
        Log.d("TTS", "finished Queueing");
	}

}
