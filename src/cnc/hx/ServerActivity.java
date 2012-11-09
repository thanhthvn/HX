package cnc.hx;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class ServerActivity extends Activity  {

	TextView txPairName;
	String host;
	AudioTrack at;
	
	    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        txPairName = (TextView) findViewById(R.id.txPairName);
        host = getIntent().getStringExtra("INFO");
        Log.d("ClientActivity", "host" + host);
        if (host !=null) txPairName.setText(host);
        
        at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 20000, AudioTrack.MODE_STREAM);

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		new VoiceServerAsyncTask(this).execute();
	}
	
	/**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    private class VoiceServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        /**
         * @param context
         */
        public VoiceServerAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
        	super.onPreExecute();
        	at.play();
        	Toast.makeText(context, "=== STOP ===" , Toast.LENGTH_LONG).show();
        }
        
        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                String fileName = Environment.getExternalStorageDirectory() + "/"
	                + context.getPackageName() + "/hx-server-" + System.currentTimeMillis() + ".3gp";
                final File f = new File(fileName);
                File dirs = new File(f.getParent());
                if (!dirs.exists()) dirs.mkdirs();
                f.createNewFile();
                
                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream is = client.getInputStream();
                OutputStream os = new BufferedOutputStream(new FileOutputStream(f)); //new FileOutputStream(f);
                // ServerActivity.this.copyFile(inputstream, os);
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                	os.write(buffer, 0, len);
                	Log.i("OutputStream", "LENGTH:" + len);
                }
                if(os != null)  os.close();
                
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
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
                Toast.makeText(context, "=== STOP ===" , Toast.LENGTH_LONG).show();
            }
            at.stop();
            at.release();
        }
    }
    
}
