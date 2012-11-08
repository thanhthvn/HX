package cnc.hx;

import java.io.ByteArrayOutputStream;
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
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class ServerActivity extends Activity  {

	TextView txPairName;
	String host;
	
	
	    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        txPairName = (TextView) findViewById(R.id.txPairName);
        host = getIntent().getStringExtra("INFO");
        Log.d("ClientActivity", "host" + host);
        if (host !=null) txPairName.setText(host);
        
        new FileServerAsyncTask(this).execute();

	}
	
	private boolean copyFile(InputStream inputStream, OutputStream out) {
	        byte buf[] = new byte[1024];
	        int len;
	        int total = 0;
	        try {
	            while ((len = inputStream.read(buf)) != -1) {
	                out.write(buf, 0, len);
	                total+= len;
	                //txPairName.setText(String.valueOf(total));
	                Log.i("ServerActivity", "TOTAL: " +  total);
	            }
	            out.close();
	            inputStream.close();
	        } catch (IOException e) {
	            Log.d(WiFiDirectActivity.TAG, e.toString());
	            return false;
	        }
	        return true;
	    }
	 
	/**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    private class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        /**
         * @param context
         */
        public FileServerAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                ServerActivity.this.copyFile(inputstream, new FileOutputStream(f));
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
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
                
            }

        }
    }
    
}
