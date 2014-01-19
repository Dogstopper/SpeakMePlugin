package speak.me.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class SpeakMePlugin extends Service {
	
	// CHANGE THIS TO BE A DESCRIPTIVE CATEGORY
	static final String CATEGORY_ADD_IF = "speak.me.category.DEFAULT_PLUGIN_CATEGORY";
	
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }    
    
	// Processes the command
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand( intent, flags, startId );
		Intent reader = new Intent("speak.me.action.TTSREADER");
		reader.putExtra("text", "SUCCESS");
		this.startService(reader);
		return START_STICKY;
	}

	public void onDestroy() {
		super.onDestroy();
	}

	

}
