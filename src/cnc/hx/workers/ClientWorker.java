package cnc.hx.workers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import cnc.hx.utils.Constants;
import cnc.hx.utils.FileTransferService;

public class ClientWorker {
	
	private AudioRecord recorder = null;
    private boolean isRecording = false;
    String host;
    Context context;
    Socket socket;
    
    public ClientWorker(Context context, String host) {
    	this.context = context;
    	this.host = host;
    }
    
    
    public void sendFile(Uri fileUri) {
    	Intent serviceIntent = new Intent(context, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, fileUri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, host);
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
 			//String dirName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cnc-hx";
 			//String fileName = dirName +  "/hx-client-" + System.currentTimeMillis() + ".3gp"; 
             //File dirs = new File(dirName);
             //if (!dirs.exists()) dirs.mkdirs();
             //f.createNewFile();
 			if (!isRecording && recorder != null) {
 				recorder.startRecording();
 			} else {
 				new recordTask().execute();
 			}
 			isRecording = true;
 		} catch (IllegalStateException e) {
 			e.printStackTrace();
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}

    private void stopRecording() {
 		try {
 			if (recorder != null) {
 		 		recorder.stop();
 		 		//recorder.release();
 		 		//recorder = null;
 	    	}
 	 		isRecording = false;
 			//if (socket !=null) 
 			//	if (socket.isConnected()) 
 			//		socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
 	}
    
 	class recordTask extends AsyncTask<Void, Integer, Integer> {
 		
 		@Override
 		protected void onPreExecute() {
 			super.onPreExecute();
 		}
 		@Override
 		protected Integer doInBackground(Void... p) {
 			try {
 				//if (socket == null) {
 					socket = new Socket();
 					socket.bind(null);
 				//}
 				//if (!socket.isConnected()) {
	 				Log.d("recordTask", "Opening client socket");
	 				socket.connect((new InetSocketAddress(host, Constants.OWNER_PORT_VOICE)), 5000);
	 				Log.d("recordTask", "Client socket - " + socket.isConnected());
 				//}
 				// InputStream is = recorder.getOutputStream();
 				
 				int frequency = 44100;
                int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
                int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
                
 				int bufferSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

 				recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
 						frequency,channelConfiguration ,audioEncoding, 4 * bufferSize);   
                byte[] buffer = new byte[bufferSize];
                
                BufferedOutputStream buff = new BufferedOutputStream(socket.getOutputStream()); //out1 is the socket's outputStream
                DataOutputStream dataOutputStreamInstance = new DataOutputStream (buff);
                
                recorder.startRecording();
                int bufferRead = 0;
                while (isRecording) {
                    bufferRead = recorder.read(buffer, 0, bufferSize);
                    dataOutputStreamInstance.write(buffer);
	              }
                if (recorder != null) {
	                recorder.stop();
	                recorder.release();
                }
                
 	 			return 1;
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 			return 0;
 		}
 		
 		@Override
 		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
 		}
 		
 	}
 	
}
