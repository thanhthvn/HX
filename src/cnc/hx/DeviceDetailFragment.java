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

import java.net.UnknownHostException;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cnc.hx.DeviceListFragment.DeviceActionListener;
import cnc.hx.utils.Constants;
import cnc.hx.utils.Utils;
import cnc.hx.workers.ClientWorker;
import cnc.hx.workers.ServerWorker;

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
    
    Intent voiceServiceIntent;
    
    String host;
    Button btSendFile, btRecord;
    
    Boolean isResume = false;
    
    ClientWorker client;
    ServerWorker server;
    
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
                        // Allow user to pick an image from Gallery or other registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });	
        btRecord = (Button) mContentView.findViewById(R.id.btRecord);		
        btRecord.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (client != null) {
					Boolean isRecording = client.recordVoice();
					if (isRecording) {
						btRecord.setText(R.string.stop_voice);
					} else {
						btRecord.setText(R.string.start_voice);
						//((DeviceActionListener) getActivity()).disconnect();
					}
				} else {
					Toast.makeText(getActivity(), "Client is disconnect.", Toast.LENGTH_SHORT).show();
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
    	if (data != null && requestCode == CHOOSE_FILE_RESULT_CODE) {
	        Uri uri = data.getData();
	        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
	        statusText.setText("Sending: " + uri);
	        client.sendFile(uri);
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
        String serverIp = Utils.getIpAddress(getActivity());
        
        Log.d("IP Address", "IP: " + serverIp);
        Log.d("IP Address", "P2P IP: " + host);
        
        client = new ClientWorker(getActivity(), info.groupOwnerAddress.getHostAddress());
    	server = new ServerWorker(getActivity());
    	
        if (info.groupFormed && info.isGroupOwner) {
        	if (!isResume) {
	            // server.receiveFile();
	            // server.receiveVoice();
	        	
	        	// startClient();
	        	server.receiveHostIp();
	        	isResume = true;
        	}
        } else if (info.groupFormed) {
        	if (!isResume) {
	            // The other device acts as the client. In this case, we enable the
	            // get file button.
	            // mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
	            //mContentView.findViewById(R.id.btRecord).setVisibility(View.VISIBLE);
	            //((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.client_text));
	        	client.sendHostIp(serverIp);
	        	startServer();
	        	isResume = true;
        	}
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }
    
    private void startServer() {
    	 Intent serverIntent = new Intent(getActivity(), ServerActivity.class);
         serverIntent.putExtra(Constants.HOST_ADDRESS, host);
         startActivity(serverIntent);
    }

    private void startClient() {
    	Intent clientIntent = new Intent(getActivity(), ClientActivity.class);
    	clientIntent.putExtra(Constants.HOST_ADDRESS, host);
        startActivity(clientIntent);
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
        isResume = false;
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
        mContentView.findViewById(R.id.btRecord).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
        isResume = false;
    }
       
}