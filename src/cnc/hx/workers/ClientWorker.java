package cnc.hx.workers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import cnc.hx.utils.Constants;
import cnc.hx.utils.FileTransferService;
import cnc.hx.utils.MediaStreamer;
import cnc.hx.utils.Utils;

public class ClientWorker {
	
	private MediaStreamer recorder = null;
    private boolean isRecording = false;
    String host;
    Context context;
    
    public ClientWorker(Context context) {
    	this.context = context;
    }
    
    
    public void sendFile(Uri fileUri, String address) {
    	Intent serviceIntent = new Intent(context, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, fileUri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, address);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, Constants.OWNER_PORT_FILE);
        context.startService(serviceIntent); 
    }
    
    public Boolean recordVoice() {
    	if (!isRecording) {
    		startRecording();
    	} else {
    		stopRecording();
    	}
    	return isRecording;
    }
    
    private void startRecording() {
 		try {
 			String dirName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cnc-hx";
 			//String fileName = dirName +  "/hx-client-" + System.currentTimeMillis() + ".3gp"; 
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
 			new recordTask().execute();
 		} catch (IllegalStateException e) {
 			e.printStackTrace();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}

    private void stopRecording() {
 		recorder.stop();
 		recorder.reset();
 		recorder.release();
 		recorder = null;
 		isRecording = false;
 	}
    
 	class recordTask extends AsyncTask<Void, Integer, Integer> {
 		Socket socket;
 		@Override
 		protected void onPreExecute() {
 			super.onPreExecute();
 		}
 		@Override
 		protected Integer doInBackground(Void... p) {
 			try {
 				socket = new Socket();
 				Log.d("recordTask", "Opening client socket");
 				socket.bind(null);
 				socket.connect((new InetSocketAddress(host, Constants.OWNER_PORT_VOICE)), 5000);
 				Log.d("recordTask", "Client socket - " + socket.isConnected());
 				InputStream is = recorder.getOutputStream();
 				Utils.copyFile(is, socket.getOutputStream());
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
 	
}
