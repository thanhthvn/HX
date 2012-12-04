package cnc.hx;


import java.nio.charset.Charset;
import cnc.hx.utils.Constants;
import cnc.hx.utils.Utils;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.app.Activity;
import android.content.Intent;
import android.text.format.Time;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements CreateNdefMessageCallback,
		OnNdefPushCompleteCallback {

	Button btBuzzer, btAvOnOff, btSettings, btConnectOnOff, btExit;
	NfcAdapter mNfcAdapter;
	private static final int MESSAGE_SENT = 1;
	String serverIp, clientIp;
	Boolean isVideoIntenCall = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btBuzzer = (Button) findViewById(R.id.btBuzzer);
        btAvOnOff = (Button) findViewById(R.id.btAvOnOff);
        btSettings = (Button) findViewById(R.id.btSettings);
        btConnectOnOff = (Button) findViewById(R.id.btConnectOnOff);
        btExit = (Button) findViewById(R.id.btExit);
        
        btBuzzer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				callVideoConversation();
			}
		});
        btAvOnOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//Intent i = new Intent(MainActivity.this, WiFiDirectActivity.class);
				//i.putExtra(Constants.CALL_INTENT, 2); // IP Camera Video
				//startActivity(i);
			}
		});
        btConnectOnOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//startActivity(new Intent(MainActivity.this, WiFiDirectActivity.class));
			}
		});
        btExit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
        
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, getString(R.string.nfc_not_available), Toast.LENGTH_LONG).show();
        } else {
	        // Register callback to set NDEF message
	        mNfcAdapter.setNdefPushMessageCallback(this, this);
	        // Register callback to listen for message-sent success
	        mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
        
        
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private void callVideoConversation() {
    	if (!isVideoIntenCall) {
	    	Intent i = new Intent(MainActivity.this, VideoConversationActivity.class);
			i.putExtra(Constants.HOST_ADDRESS, clientIp);
			startActivity(i);
			finish();
			isVideoIntenCall = true;
    	}
    }
    
	public void onNdefPushComplete(NfcEvent event) {
		// A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
	}

	public NdefMessage createNdefMessage(NfcEvent event) {
		Time time = new Time();
        time.setToNow();
        serverIp = Utils.getIpAddress(this);
        if (!serverIp.isEmpty()) {
        	//String text = ("Beam me up!\n\n" + "Beam Time: " + time.format("%H:%M:%S"));
            NdefMessage msg = new NdefMessage(
                    new NdefRecord[] { createMimeRecord(
                            "application/cnc.hx", serverIp.getBytes())
            });
            return msg;
        } else {
        	Toast.makeText(this, getString(R.string.ip_not_detect), Toast.LENGTH_LONG).show();
        }
        return null;
        
	}
	
	/** This handler receives a message from onNdefPushComplete */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SENT:
                Toast.makeText(getApplicationContext(), getString(R.string.sent_ip), Toast.LENGTH_LONG).show();
                callVideoConversation();
                break;
            }
        }
    };
    
    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }
    
    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        String payloadMessage = new String(msg.getRecords()[0].getPayload());
        if (!payloadMessage.isEmpty()) {
        	clientIp = payloadMessage;
        	Toast.makeText(this, "Client IP: " + clientIp, Toast.LENGTH_LONG).show();
        	callVideoConversation();
        }
        
    }
    
    /**
     * Creates a custom MIME type encapsulated in an NDEF record
     *
     * @param mimeType
     */
    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }
}
