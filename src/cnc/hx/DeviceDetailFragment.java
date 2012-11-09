/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cnc.hx;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ReceiverCallNotAllowedException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cnc.hx.ClientActivity.recordTask;
import cnc.hx.DeviceListFragment.DeviceActionListener;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    private MediaStreamer recorder = null;
    private boolean isRecording = false;
    
    Intent voiceServiceIntent;
    
    String host;
    Button btSendFile, btRecord;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
                        );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });	
        btRecord = (Button) mContentView.findViewById(R.id.btn_start_voice);		
        btRecord.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!isRecording) {
					startRecording();
				} else {
					stopRecording();
				}
			}
		});
        
        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d("DeviceDetailFragment", "onActivityResult");
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
    	if (requestCode == CHOOSE_FILE_RESULT_CODE) {
	        Uri uri = data.getData();
	        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
	        statusText.setText("Sending: " + uri);
	        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
	        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
	        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
	        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
	        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
	                info.groupOwnerAddress.getHostAddress());
	        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
	        getActivity().startService(serviceIntent); 
    	}
    }   
    
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
    	Log.d("onConnectionInfoAvailable", "WifiP2pInfo " + info.toString());
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        host = info.groupOwnerAddress.getHostAddress();
        if (info.groupFormed && info.isGroupOwner) {
            // new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
        	new VoiceServerAsyncTask(getActivity()).execute();
        	//Intent intent = new Intent(getActivity(), ServerActivity.class);
        	//intent.putExtra("INFO", info.groupOwnerAddress.getHostAddress());
        	//startActivity(intent);
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            // mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            mContentView.findViewById(R.id.btn_start_voice).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.client_text));
        	//Intent intent = new Intent(getActivity(), ClientActivity.class);
        	//intent.putExtra("INFO", info.groupOwnerAddress.getHostAddress());
            //startActivity(intent);
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_start_voice).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

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
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
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
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }
    }
    

    // >>>>> SERVER
    private class VoiceServerAsyncTask extends AsyncTask<Void, Void, String> {

        Context context;
        /**
         * @param context
         * @param host 
         */
        public VoiceServerAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
        	super.onPreExecute();
        	// at.play();
        	
        }
        
        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                //Toast.makeText(context, "=== START VOICE ===" , Toast.LENGTH_SHORT).show();
                String fileName = Environment.getExternalStorageDirectory() + "/"
	                + context.getPackageName() + "/hx-server-" + System.currentTimeMillis() + ".3gp";
                final File f = new File(fileName);
                File dirs = new File(f.getParent());
                if (!dirs.exists()) dirs.mkdirs();
                f.createNewFile();
                
                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
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
                Toast.makeText(context, "=== STOP VOICE ===" , Toast.LENGTH_SHORT).show();
            }
            //at.stop();
            //at.release();
        }
    }
    // <<<<< SERVER
    
    // >>>>> CLIENT
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
    
    // <<<<< CLIENT
    
}
