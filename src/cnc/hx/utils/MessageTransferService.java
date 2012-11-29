
package cnc.hx.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;


public class MessageTransferService extends IntentService {

	private static final int SOCKET_TIMEOUT = 5000;
	public static final String ACTION_SEND_MESSAGE = "cnc.hx.SEND_MESSAGE";
	public static final String EXTRAS_MESSSGE_STRING = "message_string";
	public static final String EXTRAS_ADDRESS = "go_host";
	public static final String EXTRAS_PORT = "go_port";

	public MessageTransferService(String name) {
		super(name);
	}

	public MessageTransferService() {
		super("MessageTransferService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (intent.getAction().equals(ACTION_SEND_MESSAGE)) {
			String messageString = intent.getExtras().getString(EXTRAS_MESSSGE_STRING);
			String host = intent.getExtras().getString(EXTRAS_ADDRESS);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_PORT);

			try {
				Log.d("MessageTransferService", "Opening client socket - host: " + host);
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
				Log.d("MessageTransferService", "Client socket - " + socket.isConnected());
				OutputStream os = socket.getOutputStream();
 				byte[] stringByte = messageString.getBytes();
 				os.write(stringByte);
 				os.close();
				Log.d("MessageTransferService", "Client: Data written");
			} catch (IOException e) {
				Log.e("MessageTransferService", e.getMessage());
			} finally {
				if (socket != null) {
					if (socket.isConnected()) {
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

		}
	}
}
