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
    
    public static final String IP_SERVER = "192.168.49.1";
    
    String host, serverIp, clientIP = IP_SERVER;
    Button btSendFile, btRecord, btSendMessage;
    
    Boolean isResume = false;
    Integer callIntent = 0;
    
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
        
        callIntent = getActivity().getIntent().getIntExtra(Constants.CALL_INTENT, 1);

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
				if (callIntent == 1) {
					if (client == null) client = new ClientWorker(getActivity(), clientIP);
					Boolean isRecording = client.recordVoice();
					if (isRecording) {
						btRecord.setText(R.string.stop_voice);
					} else {
						btRecord.setText(R.string.start_voice);
					}
				}
				if (callIntent == 2) {
					
				}
			}
		});
        
        btSendMessage = (Button) mContentView.findViewById(R.id.btSendMessage);		
        btSendMessage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (client == null) client = new ClientWorker(getActivity(), clientIP);
				client.sendMessage("Hello " + clientIP + "!");
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

        host = info.groupOwnerAddress.getHostAddress();
        serverIp = Utils.getIpAddress(getActivity());
        
        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + host);
        
        Log.d("P2P IP", "# " + host);
        Log.d("Wifi IP", "# " + serverIp);
        
        getClientIp();
        
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        
        server = new ServerWorker(getActivity());
        
    	if (!isResume) {
    		if (callIntent == 1) {
		        if (info.groupFormed && info.isGroupOwner) {
		        	server.receiveMessage();
		        	Log.d("ConnectionInfo", "groupFormed && isGroupOwner");
		        } else if (info.groupFormed) {
		        	server.receiveMessage();
		        	//btRecord.setVisibility(View.VISIBLE);
		        	btSendMessage.setVisibility(View.VISIBLE);
		        	((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.client_text));
		        	Log.d("ConnectionInfo", "isGroupOwner");
		        }
				isResume = true;
				Log.d("onConnectionInfoAvailable", "server.receiveVoice();isResume");
    		}
    		
    		if (callIntent == 2) {
    			client = new ClientWorker(getActivity(), host);
    			if (info.groupFormed && info.isGroupOwner) {
		        	server.receiveMessage();
		        	isResume = true;
    	        } else if (info.groupFormed) {
    	        	mContentView.findViewById(R.id.btRecord).setVisibility(View.VISIBLE);
	        		client.sendMessage(serverIp); // serverIp
		        	startServer();
		        	isResume = true;
    	        }
    		}
    	}
    	
        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }
    
    private void startServer() {
    	 Intent serverIntent = new Intent(getActivity(), ServerActivity.class);
         serverIntent.putExtra(Constants.HOST_ADDRESS, host); // host
         startActivity(serverIntent);
    }

    private void startClient() {
    	Intent clientIntent = new Intent(getActivity(), ClientActivity.class);
    	clientIntent.putExtra(Constants.HOST_ADDRESS, host);
        startActivity(clientIntent);
    }
    
    private void getClientIp() {
        if (device != null) {
        	String localIP = Utils.getLocalIPAddress();
            Log.d("Local IP", "# " + localIP);
        	// Trick to find the ip in the file /proc/net/arp
			String client_mac_fixed = new String(device.deviceAddress).replace("99", "19");
			String clientIPFromMac = Utils.getIPFromMac(client_mac_fixed);
			Log.d("#clientIPFromMac", "# " + clientIPFromMac);
			if(localIP != null && clientIPFromMac != null) {
				if(localIP.equals(IP_SERVER)) clientIP = clientIPFromMac;
			}
        }
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
        if (info != null) {
			if (info.groupFormed || info.isGroupOwner) {
				getClientIp();
				//btRecord.setVisibility(View.VISIBLE);
				btSendMessage.setVisibility(View.VISIBLE);
			}
		}
        //isResume = false;
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