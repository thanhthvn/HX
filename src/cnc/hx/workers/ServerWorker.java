package cnc.hx.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import cnc.hx.utils.Constants;
import cnc.hx.utils.Utils;

public class ServerWorker {
	
	ServerSocket serverSocketFile, serverSocketVoice, serverSocketText;
	
	Context context;
    
    public ServerWorker(Context context) {
    	this.context = context;
    }
    
    public void receiveFile() {
    	new FileServerAsyncTask().execute();
    }
    
    public void receiveVoice() {
    	new VoiceServerAsyncTask().execute();
    }
    
    public void receiveHostIp() {
    	new MessageServerAsyncTask().execute();
    }
    
	/**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    private class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        //private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask() {
            // this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
            	if (serverSocketFile == null) {
            		serverSocketFile = new ServerSocket(Constants.OWNER_PORT_FILE);
            	}
                Log.d("FileServerAsyncTask", "Server: Socket opened");
                Socket client = serverSocketFile.accept();
                Log.d("FileServerAsyncTask", "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() 
                		+ "/cnc-hx/s-" + System.currentTimeMillis() + ".jpg");
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d("FileServerAsyncTask", "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                Utils.copyFile(inputstream, new FileOutputStream(f));
                serverSocketFile.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e("FileServerAsyncTask", e.getMessage());
                return null;
            }
        }


        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                // statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }
    }
    
	private class VoiceServerAsyncTask extends AsyncTask<Void, Void, String> {

		AudioTrack at;
		//OutputStream os;
		
        public VoiceServerAsyncTask() {
			
        }

        @Override
        protected void onPreExecute() {
        	super.onPreExecute();
        	//Toast.makeText(context, "START VOICE" , Toast.LENGTH_SHORT).show();
        	
        }
        
        @Override
        protected String doInBackground(Void... params) {
            try {
            	//if (serverSocketVoice == null) {
            		serverSocketVoice = new ServerSocket(Constants.OWNER_PORT_VOICE);
            	//}
                Log.d("VoiceServerAsyncTask", "Server: Socket opened");
                
                Socket client = serverSocketVoice.accept();
                Log.d("VoiceServerAsyncTask", "Server: connection done");
                
//                String fileName = Environment.getExternalStorageDirectory()
//	                + "/cnc-hx/s-" + System.currentTimeMillis() + ".3gp";
//                final File f = new File(fileName);
//                File dirs = new File(f.getParent());
//                if (!dirs.exists()) dirs.mkdirs();
//                f.createNewFile();
//                Log.d("VoiceServerAsyncTask", "server: copying files " + f.toString());

                InputStream is = client.getInputStream();
                // if (os == null) os = new BufferedOutputStream(new FileOutputStream(f)); //new FileOutputStream(f);
                
                int frequency = 44100;
                int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
                int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
                
                int bufferSize = AudioRecord.getMinBufferSize(frequency, 
                		channelConfiguration, audioEncoding); 
                Log.i("AudioRecord", "bufferSize:" + bufferSize);
                byte[] buffer = new byte[bufferSize];
                int len = 0;
                at = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, 
                		channelConfiguration, audioEncoding, 4*bufferSize, AudioTrack.MODE_STREAM);
                at.setPlaybackRate(frequency);
                at.play();
                int atLen = 0;
                while ((len = is.read(buffer)) != -1) {
                	//os.write(buffer, 0, len);
                	atLen += at.write(buffer, 0, len);
                	Log.i("InputStream", "LENGTH:" + len);
                }
                is.close();
                Log.i("InputStream", "AudioTrack LEN:" + atLen);
                at.stop();
                at.release();
                return null;//f.getAbsolutePath();
            } catch (IOException e) {
                Log.e("VoiceServerAsyncTask", e.getMessage());
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
				try {
					//if(os != null) os.close();
					serverSocketVoice.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
                /*Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "audio/*");
                context.startActivity(intent);
                Toast.makeText(context, "STOP VOICE" , Toast.LENGTH_SHORT).show();*/
        }
	}
	
	private class MessageServerAsyncTask extends AsyncTask<Void, Void, String> {

        public MessageServerAsyncTask() {
			
        }

        @Override
        protected void onPreExecute() {
        	super.onPreExecute();
        	
        }
        
        @Override
        protected String doInBackground(Void... params) {
            try {
            	serverSocketText = new ServerSocket(Constants.OWNER_PORT_VOICE);
                Log.d("MessageServerAsyncTask", "Server: Socket opened");
                Socket client = serverSocketText.accept();
                Log.d("MessageServerAsyncTask", "Server: connection done");
                InputStream is = client.getInputStream();
                String streamUrl = convertStreamToString(is);
                Log.d("MessageServerAsyncTask", "Stream URL: " + streamUrl);
                return streamUrl;
            } catch (Exception e) {
                Log.e("MessageServerAsyncTask", e.getMessage());
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String streamUrl) {
				try {
					serverSocketText.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (streamUrl != null) {
					Toast.makeText(context, "PLAY VIDEO STREAM: rtsp://" + streamUrl + ":8086" , Toast.LENGTH_LONG).show();
	                try {
						Intent intent = new Intent();
		                intent.setAction(android.content.Intent.ACTION_VIEW);
		                intent.setDataAndType(Uri.parse("rtsp://" + streamUrl + ":8086"), "video/*");
		                context.startActivity(intent);
	                } catch (Exception e) {
	                	Toast.makeText(context, "Please install video player to play stream video." , Toast.LENGTH_LONG).show();
	                	
	                	e.printStackTrace();
	                }
				}
        }
	}
	
	public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	        sb.append(line);
	    }
	    is.close();
	    return sb.toString();
	}
}
