package cnc.hx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

	private MediaStreamer recorder = null;
	private boolean isRecording = false;

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
					startRecording();
				} else {
					stopRecording();
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
		try {
			String dirName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cnc-hx";
			String fileName = dirName +  "/hx-client-" + System.currentTimeMillis() + ".3gp"; 
            File dirs = new File(dirName);
            if (!dirs.exists()) dirs.mkdirs();
            //f.createNewFile();
			recorder = new MediaStreamer();
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			// recorder.setOutputFile(fileName);
			recorder.prepare();
			recorder.start();   // Recording is now started
			isRecording = true;
			btRecord.setText(R.string.stop);
			new recordTask().execute();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class recordTask extends AsyncTask<Void, Integer, Integer> {

		Socket socket;
		int port = 8988;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		@Override
		protected Integer doInBackground(Void... p) {
			try {
				socket = new Socket();
				Log.d(WiFiDirectActivity.TAG, "Opening client socket");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), 5000);
				Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
				InputStream is = recorder.getOutputStream();
				DeviceDetailFragment.copyFile(is, socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void stopRecording() {
		recorder.stop();
		recorder.reset();
		recorder.release();
		isRecording = false;
		btRecord.setText(R.string.start);
	}



}
