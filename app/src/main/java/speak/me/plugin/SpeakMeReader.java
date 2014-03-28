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

public class SpeakMeReader {
	private TextToSpeech tts;
    private Service parent;

    private boolean speaking =  false;
    private String utteranceID = "utterance_id_speech";

    public SpeakMeReader(Service parent) {
//    @Override
//    public void onCreate() {
//        super.onCreate();
//
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


    public boolean isDoneSpeaking() {
        return speaking == false;
    }

    public void invokeTTS(String text) {
        if (text != null && text.trim().length() != 0) {
            textToSpeech(text);
        }
    }

    public void textToSpeech(String text) {
        AudioManager am = (AudioManager)parent.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_SYSTEM,false);
        am.setStreamMute(AudioManager.STREAM_MUSIC,false);

        HashMap<String, String> voiceMap = new HashMap();
        voiceMap.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS,"true");
        voiceMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, this.utteranceID);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, voiceMap);
        Log.d("TTS", "finished Queueing");
	}
//
//	@Override
//	public IBinder onBind(Intent arg0) {
//		return mBinder;
//	}

}
