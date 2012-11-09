// Copyright 2011 Google Inc. All Rights Reserved.

package cnc.hx;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class VoiceTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_VOICE = "cnc.hx.SEND_VOICE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    static Socket socket;
    
    public VoiceTransferService(String name) {
        super(name);
    }

    public VoiceTransferService() {
        super("VoiceTransferService");
    }
    
    
    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

    	Log.d("VoiceTransferService", "onHandleIntent");
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_VOICE)) {
        	// VOICE_STREAM
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d("VoiceTransferService.onHandleIntent", "Opening client socket - ");
                if (socket == null) {
                	socket = new Socket();
                }
                if (!socket.isConnected()) {
                	socket.bind(null);
	                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
	                Log.d("VoiceTransferService.onHandleIntent", "Client socket - " + socket.isConnected());
                }
                OutputStream stream = socket.getOutputStream();
                byte[] data = intent.getExtras().getByteArray("VOICE_STREAM");
                if (data != null) {
                	Log.d("VoiceTransferService", "DATA size: " + data.length);
                	//DeviceDetailFragment.sendVoice(data, stream);
                }
                Log.d("VoiceTransferService.onHandleIntent", "Client: Data written");
            } catch (IOException e) {
                Log.e("VoiceTransferService IOException", e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            // socket.close();
                        } catch (Exception e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
    
}
