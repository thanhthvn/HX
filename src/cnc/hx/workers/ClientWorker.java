package cnc.hx.workers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import android.util.Log;
import cnc.hx.utils.Constants;
import cnc.hx.utils.FileTransferService;

public class ClientWorker {
	
    private boolean isRecording = false;
    String host;
    Context context;
    
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
    		Log.d("recordVoice", "startRecording()");
    	} else {
    		stopRecording();
    		Log.d("recordVoice", "stopRecording()");
    	}
    	return isRecording;
    }
    
    public void sendHostIp(String ipAdress) {
    	new messageTask().execute(ipAdress);
    }
    
    private void startRecording() {
 		try {
 			//if (!isRecording && recorder != null) {
 			//	recorder.startRecording();
 			//	Log.d("startRecording()", "recorder.startRecording()");
 			//} else {
 				new RecordTask().execute();
 				Log.d("startRecording()", "recordTask().execute()");
 			//}
 			isRecording = true;
 		} catch (IllegalStateException e) {
 			e.printStackTrace();
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}

    private void stopRecording() {
 		try {
 			//if (recorder != null) {
 		 	//	recorder.stop();
 		 	//	recorder.release();
 		 	//	recorder = null;
 	    	//}
 	 		isRecording = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
 	}
    
 	class RecordTask extends AsyncTask<Void, Void, Void> {
 		
 		@Override
 		protected void onPreExecute() {
 			super.onPreExecute();
 			Log.d("RecordTask", "onPreExecute()");
 		}
 		
 		@Override
 		protected Void doInBackground(Void... params) {
 			Log.d("RecordTask", "doInBackground(...)");
 			Socket socket = new Socket();
 			try {
				socket.bind(null);
 				Log.d("RecordTask", "Opening client socket, host: " + host);
 				socket.connect((new InetSocketAddress(host, Constants.PORT_VOICE_CLIENT)), 5000);
 				Log.d("RecordTask", "Client socket connected? : " + socket.isConnected());
 				
 				int frequency = 44100;
                int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
                int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
                
 				int bufferSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

 				AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
 						frequency,channelConfiguration ,audioEncoding, 4 * bufferSize);   
                byte[] buffer = new byte[bufferSize];
                
                BufferedOutputStream buff = new BufferedOutputStream(socket.getOutputStream()); //out1 is the socket's outputStream
                DataOutputStream dataOutputStreamInstance = new DataOutputStream (buff);
                
                recorder.startRecording();
                Log.d("RecordTask", "recorder.startRecording()");
                while (isRecording) {
                    recorder.read(buffer, 0, bufferSize);
                    dataOutputStreamInstance.write(buffer);
                }
                if (recorder != null) {
	                recorder.stop();
	                recorder.release();
	                recorder = null;
	                Log.d("RecordTask", "recorder.release()");
                }
 			} catch (Exception e) {
 				e.printStackTrace();
 			} finally {
				if (socket != null) {
					if (socket.isConnected()) {
						try {
							socket.close();
							Log.d("RecordTask", "socket.close()");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					socket = null;
				}
			}
			return null;
 		}
 		
 		@Override
 		protected void onPostExecute(Void result) {
 			Log.d("RecordTask", "onPostExecute");
			super.onPostExecute(result);
 		}
 	}
 	
 	class messageTask extends AsyncTask<String, Void, Void> {
 		
 		@Override
 		protected void onPreExecute() {
 			super.onPreExecute();
 		}
 		@Override
 		protected Void doInBackground(String... msgs) {
 			Socket socket = new Socket();
 			try {
				socket.bind(null);
 				Log.d("sendMessageTask", "Opening client socket");
 				socket.connect((new InetSocketAddress(host, Constants.OWNER_PORT_VOICE)), 5000);
 				Log.d("sendMessageTask", "Client socket - " + socket.isConnected());
 				if (msgs[0] != null) {
 					OutputStream os = socket.getOutputStream();
	 				byte[] stringByte = msgs[0].getBytes();
	 				os.write(stringByte);
	 				os.close();
 				}
 			} catch (Exception e) {
 				e.printStackTrace();
 			} finally {
				if (socket != null) {
					if (socket.isConnected()) {
						try {
							socket.close();
						} catch (IOException e) {
							// Give up
							e.printStackTrace();
						}
					}
				}
			}
 			return null;
 		}
 		
 		@Override
 		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
 		}
 		
 	}
 	
}
