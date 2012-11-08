package cnc.hx;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class ClientActivity extends Activity {
	String host;
	Button btSendFile, btRecord;
	ArrayAdapter<WifiP2pDevice> peerListAdapter;
	TextView txPairName;

	private MediaRecorder recorder = null;
	private boolean isRecording = false;
	private int SAMPLERATE = 8000;

	private int CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	private int bufferSize = AudioRecord.getMinBufferSize(SAMPLERATE, CHANNELS,
			AUDIO_FORMAT);
	private Thread recordingThread = null;

	Intent voiceServiceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client);
		host = getIntent().getStringExtra("INFO");
		btSendFile = (Button) findViewById(R.id.btSendFile);
		btSendFile.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(intent, 1);
			}
		});
		btRecord = (Button) findViewById(R.id.btRecord);
		btRecord.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!isRecording) {
					btRecord.setText("STOP");

					startRecording();
				} else {
					stopRecording();
					btRecord.setText("START");

				}
			}
		});
		txPairName = (TextView) findViewById(R.id.txPairName);
		if (host != null)
			txPairName.setText(host);

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			Log.d("DeviceDetailFragment", "onActivityResult");
			// User has picked an image. Transfer it to group owner i.e peer
			// using
			// FileTransferService.
			Uri uri = data.getData();
			Intent serviceIntent = new Intent(this, FileTransferService.class);
			serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
			serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH,
					uri.toString());
			serviceIntent.putExtra(
					FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, host);
			serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT,
					8988);
			startService(serviceIntent);
		}
	}

	// /////////////////////////////////////
	public void startRecording() {
		isRecording = true;
		recorder = new MediaRecorder();
		new Thread(new Runnable() {
			
			public void run() {
				// open socket
				Socket socket = new Socket();
				int port = 8988;

				try {
					Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
					socket.bind(null);
					socket.connect((new InetSocketAddress(host, port)), 5000);

					Log.d(WiFiDirectActivity.TAG,
							"Client socket - " + socket.isConnected());

					ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);

//					MediaRecorder recorder = new MediaRecorder();

					// recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
					// SAMPLERATE,
					// CHANNELS, AUDIO_FORMAT, bufferSize);
					//

					recorder.setOutputFile(pfd.getFileDescriptor());

					recorder.prepare();

					recorder.start();
					

				} catch (Exception ex) {
					isRecording = false;
				}
				
			}
		}).start();
		

		// voiceServiceIntent = new Intent(this, VoiceTransferService.class);
		// voiceServiceIntent.setAction(VoiceTransferService.ACTION_SEND_VOICE);
		// voiceServiceIntent.putExtra(
		// VoiceTransferService.EXTRAS_GROUP_OWNER_ADDRESS, host);
		// voiceServiceIntent.putExtra(
		// VoiceTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
		// startService(voiceServiceIntent);
		// recordingThread = new Thread(new Runnable()
		// {
		// public void run() {
		// writeAudioData();
		// }
		//
		// });
		// recordingThread.start();

	}

	public void stopRecording() {
		recorder.stop();
		recorder.release();
		recorder = null;
		isRecording = false;
	}

//	private void writeAudioData() {
//		byte data[] = new byte[bufferSize];
//		while (isRecording) {
//			recorder.read(data, 0, bufferSize);
//			sendData(data);
//		}
//	}
//
//	private void sendData(byte[] data) {
//		// TextView statusText = (TextView)
//		// mContentView.findViewById(R.id.status_text);
//		// statusText.setText("Sending: " + uri);
//		// Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
//		Log.d("sendData", "data size: " + data.length);
//		voiceServiceIntent.putExtra("VOICE_STREAM", data);
//		startService(voiceServiceIntent);
//	}
//
//	private void connectServerSocket() {
//		Socket socket = new Socket();
//		int port = 8988;
//
//		try {
//			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
//			socket.bind(null);
//			socket.connect((new InetSocketAddress(host, port)), 5000);
//
//			Log.d(WiFiDirectActivity.TAG,
//					"Client socket - " + socket.isConnected());
//			OutputStream stream = socket.getOutputStream();
//			ContentResolver cr = getContentResolver();
//			InputStream is = null;
//			try {
//				is = new ByteInputStream();
//			} catch (FileNotFoundException e) {
//				Log.d(WiFiDirectActivity.TAG, e.toString());
//			}
//			// DeviceDetailFragment.copyFile(is, stream);
//			DeviceDetailFragment.copyFile(is, stream);
//			Log.d(WiFiDirectActivity.TAG, "Client: Data written");
//		} catch (IOException e) {
//			Log.e(WiFiDirectActivity.TAG, e.getMessage());
//		} finally {
//			if (socket != null) {
//				if (socket.isConnected()) {
//					try {
//						socket.close();
//					} catch (IOException e) {
//						// Give up
//						e.printStackTrace();
//					}
//				}
//			}
//		}
//	}

	// private void send(byte[] data) {
	//
	// int minBufferSize = AudioTrack.getMinBufferSize(8000,
	// AudioFormat.CHANNEL_CONFIGURATION_MONO,
	// AudioFormat.ENCODING_PCM_16BIT);
	//
	// AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
	// AudioFormat.CHANNEL_CONFIGURATION_MONO,
	// AudioFormat.ENCODING_PCM_16BIT, minBufferSize,
	// AudioTrack.MODE_STREAM);
	//
	// at.play();
	// at.write(data, 0, bufferSize);
	// at.stop();
	// at.release();
	//
	// }
	//
	// public static void receiveVoiceStream(InputStream is) {
	// ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	// int nRead;
	// int SAMPLERATE = 8000;
	// int CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	// int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	// int bufferSize = AudioRecord.getMinBufferSize(SAMPLERATE, CHANNELS,
	// AUDIO_FORMAT);
	// Log.d("receiveVoiceStream", is.toString());
	// byte[] data = new byte[16384]; // 16384 bufferSize
	// try {
	// while ((nRead = is.read(data, 0, data.length)) != -1) {
	// buffer.write(data, 0, nRead);
	// }
	// buffer.flush();
	// byte[] arr = buffer.toByteArray();
	// Log.d("receiveVoiceStream", "data size: " + arr.length);
	// receiveVoice(arr);
	// } catch (IOException e) {
	// Log.e("receiveVoiceStream", e.getMessage());
	// }
	// }
	//
	// public static boolean receiveVoice(byte[] data) {
	// Log.d("receiveVoice", "data size: " + data.length);
	// int SAMPLERATE = 8000;
	// int CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	// int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	// int bufferSize = AudioRecord.getMinBufferSize(SAMPLERATE, CHANNELS,
	// AUDIO_FORMAT);
	// int minBufferSize = AudioTrack.getMinBufferSize(8000,
	// AudioFormat.CHANNEL_CONFIGURATION_MONO,
	// AudioFormat.ENCODING_PCM_16BIT);
	//
	// AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
	// AudioFormat.CHANNEL_CONFIGURATION_MONO,
	// AudioFormat.ENCODING_PCM_16BIT, minBufferSize,
	// AudioTrack.MODE_STREAM);
	//
	// at.play();
	// at.write(data, 0, bufferSize);
	// at.stop();
	// at.release();
	// return true;
	// }
	//
	// public static boolean sendVoice(byte[] data, OutputStream out) {
	// try {
	// out.write(data);
	// out.close();
	// } catch (IOException e) {
	// Log.d(WiFiDirectActivity.TAG, e.toString());
	// }
	// return true;
	// }

}
