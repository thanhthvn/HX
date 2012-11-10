package cnc.hx.workers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import cnc.hx.utils.Constants;
import cnc.hx.utils.Utils;

public class ServerWorker {
	
	ServerSocket serverSocketFile, serverSocketVoice;
	
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

        public VoiceServerAsyncTask() {
        }

        @Override
        protected void onPreExecute() {
        	super.onPreExecute();
        	//Toast.makeText(context, "START VOICE" , Toast.LENGTH_SHORT).show();
        	// at.play();
        }
        
        @Override
        protected String doInBackground(Void... params) {
            try {
            	if (serverSocketVoice == null) {
            		serverSocketVoice = new ServerSocket(Constants.OWNER_PORT_VOICE);
            	}
                Log.d("VoiceServerAsyncTask", "Server: Socket opened");
                
                Socket client = serverSocketVoice.accept();
                Log.d("VoiceServerAsyncTask", "Server: connection done");
                
                String fileName = Environment.getExternalStorageDirectory()
	                + "/cnc-hx/s-" + System.currentTimeMillis() + ".3gp";
                final File f = new File(fileName);
                File dirs = new File(f.getParent());
                if (!dirs.exists()) dirs.mkdirs();
                f.createNewFile();
                
                Log.d("VoiceServerAsyncTask", "server: copying files " + f.toString());
                InputStream is = client.getInputStream();
                OutputStream os = new BufferedOutputStream(new FileOutputStream(f)); //new FileOutputStream(f);
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                	os.write(buffer, 0, len);
                	Log.i("OutputStream", "LENGTH:" + len);
                }
                if(os != null)  os.close();
                serverSocketVoice.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e("VoiceServerAsyncTask", e.getMessage());
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "audio/*");
                context.startActivity(intent);
                Toast.makeText(context, "STOP VOICE" , Toast.LENGTH_SHORT).show();
            }
            //at.stop();
            //at.release();
        }
	}
}
