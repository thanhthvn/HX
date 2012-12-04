package cnc.hx;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.networking.RtspServer;
import net.majorkernelpanic.networking.Session;
import net.majorkernelpanic.streaming.video.H264Stream;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import cnc.hx.utils.Constants;
import cnc.hx.utils.CustomHttpServer;
import cnc.hx.utils.Utils;

public class VideoConversationActivity extends Activity implements
		OnSharedPreferenceChangeListener, 
		OnCompletionListener, OnPreparedListener, OnItemSelectedListener {

	private SurfaceHolder holder;
	private SurfaceView svCameraServer;
	private CustomHttpServer httpServer = null;
	private RtspServer rtspServer = null;
	private TextView tvServerInfo;
	private Context context;
	private ImageButton btSetting;

	// Client
	private SharedPreferences settings;
	Button btConnect;
	private EditText etServerIp;
	private MyVideoView videoView;
	private MediaPlayer audioStream;
	private FrameLayout flCameraClient;
	private RelativeLayout rlForm;
	private ProgressBar progressBar;
	private String host;
	private String videoParameters = "", audioParameters = "";
	private Boolean isStreaming = false;
	LinearLayout llConnectForm;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
		//		| ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.video_conversation);
		svCameraServer = (SurfaceView) findViewById(R.id.svCameraServer);
		tvServerInfo = (TextView) findViewById(R.id.tvServerInfo);
		context = this.getApplicationContext();
		svCameraServer.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		holder = svCameraServer.getHolder();

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		H264Stream.setPreferences(settings);
		settings.registerOnSharedPreferenceChangeListener(this);

		Session.setSurfaceHolder(holder);
		Session.setHandler(handler);
		Session.setDefaultAudioEncoder(settings.getBoolean("stream_audio",
				true) ? Integer.parseInt(settings.getString("audio_encoder",
				"3")) : 0);
		Session.setDefaultVideoEncoder(settings
				.getBoolean("stream_video", true) ? Integer.parseInt(settings
				.getString("video_encoder", "2")) : 0);
		Session.setDefaultVideoQuality(new VideoQuality(settings.getInt(
				"video_resX", 0), settings.getInt("video_resY", 0), Integer
				.parseInt(settings.getString("video_framerate", "0")), Integer
				.parseInt(settings.getString("video_bitrate", "0")) * 1000));
		rtspServer = new RtspServer(Constants.RTSP_PORT, handler);
		httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);

		btSetting = (ImageButton) findViewById(R.id.btSetting);
		btSetting.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(context,OptionsActivity.class);
	            startActivityForResult(intent, 0);
			}
		}); 
		
		// Client
		btConnect = (Button) findViewById(R.id.btConnect);
		etServerIp = (EditText) findViewById(R.id.etServerIp);
		flCameraClient = (FrameLayout) findViewById(R.id.flCameraClient);
		rlForm = (RelativeLayout) findViewById(R.id.rlForm);
		llConnectForm = (LinearLayout) findViewById(R.id.llConnectForm);
		progressBar = (ProgressBar)findViewById(R.id.progress);
		
		//llConnectForm.setVisibility(View.GONE);
		
		// Resolution
        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        		R.array.videoResolutionArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        // Framerate
        spinner = (Spinner) findViewById(R.id.spinner2);
        adapter = ArrayAdapter.createFromResource(this,
        		R.array.videoFramerateArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        
        // Bitrate
        spinner = (Spinner) findViewById(R.id.spinner3);
        adapter = ArrayAdapter.createFromResource(this,
        		R.array.videoBitrateArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        
        // Video Encoder
        spinner = (Spinner) findViewById(R.id.spinner4);
        adapter = ArrayAdapter.createFromResource(this,
        		R.array.videoEncoderArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);  
        spinner.setOnItemSelectedListener(this);
        
        // Audio Encoder
        spinner = (Spinner) findViewById(R.id.spinner5);
        adapter = ArrayAdapter.createFromResource(this,
        		R.array.audioEncoderArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);          
        spinner.setOnItemSelectedListener(this);
        
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		etServerIp.setText(settings.getString("last_server_ip", "10.0.2.10"));
        
		audioStream = new MediaPlayer();

		btConnect.setOnClickListener(
			new OnClickListener() {
				public void onClick(View v) {
					host = etServerIp.getText().toString();
					// startConnect();
					
			        Utils.getHxDevices(VideoConversationActivity.this, 8080);
				}
		});
		
		host = getIntent().getStringExtra(Constants.HOST_ADDRESS);
		/*btStop = (ImageView) findViewById(R.id.btStop);
        btStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
	            stopServer();
			}
		}); */
				
	}
	
	private void startConnect() {
		if (host != null) {
			if (!host.equalsIgnoreCase("")) {
				etServerIp.setText(host);
				if (!isStreaming) {
					Editor editor = settings.edit();
					editor.putString("last_server_ip", host);
					editor.commit();
					progressBar.setVisibility(View.VISIBLE);
					getCurrentConfiguration();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(etServerIp.getWindowToken(), 0);
					btConnect.setText(R.string.disconnect);	
					Toast.makeText(this, "Streaming from " + host, Toast.LENGTH_LONG).show();
					isStreaming = true;
				} else {
					stopStreaming();
					btConnect.setText(R.string.connect);
					progressBar.setVisibility(View.GONE);
					isStreaming = false;
				}
			}
		}
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("video_resX")) {
			Session.defaultVideoQuality.resX = sharedPreferences.getInt(
					"video_resX", 0);
		} else if (key.equals("video_resY")) {
			Session.defaultVideoQuality.resY = sharedPreferences.getInt(
					"video_resY", 0);
		} else if (key.equals("video_framerate")) {
			Session.defaultVideoQuality.frameRate = Integer
					.parseInt(sharedPreferences.getString("video_framerate",
							"0"));
		} else if (key.equals("video_bitrate")) {
			Session.defaultVideoQuality.bitRate = Integer
					.parseInt(sharedPreferences.getString("video_bitrate", "0")) * 1000;
		} else if (key.equals("stream_audio") || key.equals("audio_encoder")) {
			Session.setDefaultAudioEncoder(sharedPreferences.getBoolean(
					"stream_audio", true) ? Integer.parseInt(sharedPreferences
					.getString("audio_encoder", "3")) : 0);
		} else if (key.equals("stream_video") || key.equals("video_encoder")) {
			Session.setDefaultVideoEncoder(sharedPreferences.getBoolean(
					"stream_video", true) ? Integer.parseInt(sharedPreferences
					.getString("video_encoder", "2")) : 0);
		} else if (key.equals("enable_http")) {
    		if (sharedPreferences.getBoolean("enable_http", true)) {
    			if (httpServer == null) httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);
    		} else {
    			if (httpServer != null) httpServer = null;
    		}
    	}
		else if (key.equals("enable_rtsp")) {
			if (sharedPreferences.getBoolean("enable_rtsp", true)) {
				if (rtspServer == null)
					rtspServer = new RtspServer(Constants.RTSP_PORT, handler);
			} else {
				if (rtspServer != null)
					rtspServer = null;
			}
		}
	}

	public void onResume() {
		super.onResume();
		if (!streaming) displayIpAddress();
		registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		startServers();
    	startConnect();
	}

	public void onPause() {
		super.onPause();
		if (rtspServer != null) rtspServer.stop();
		CustomHttpServer.setScreenState(false);
		unregisterReceiver(wifiStateReceiver);
	}

	public void onDestroy() {
		super.onDestroy();
		if (httpServer != null) httpServer.stop();
		if (rtspServer != null) rtspServer.stop();
	}

	public void onBackPressed() {
		Intent i = new Intent(this, MainActivity.class);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(i);
    	finish();
    }
	
	private void startServers() {
		if (rtspServer != null) {
			try {
				rtspServer.start();
			} catch (IOException e) {
				log("RtspServer could not be started : "
						+ (e.getMessage() != null ? e.getMessage()
								: "Unknown error"));
			}
		}
		if (httpServer != null) {
    		CustomHttpServer.setScreenState(true);
    		try {
    			httpServer.start();
    		} catch (IOException e) {
    			log("HttpServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
	}

	// BroadcastReceiver that detects wifi state changements
	private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// This intent is also received when app resumes even if wifi state
			// hasn't changed :/
			if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				if (!streaming)
					displayIpAddress();
			}
		}
	};

	private boolean streaming = false;

	// The Handler that gets information back from the RtspServer and Session
	private final Handler handler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case RtspServer.MESSAGE_LOG:
				log((String) msg.obj);
				break;
			case RtspServer.MESSAGE_ERROR:
				log((String) msg.obj);
				break;
			case Session.MESSAGE_START:
				streaming = true;
				streamingState(1);
				break;
			case Session.MESSAGE_STOP:
				streaming = false;
				displayIpAddress();
				break;
			case Session.MESSAGE_ERROR:
				log((String) msg.obj);
				break;
			case Session.MESSAGE_CLIENT_IP:
				if (host == null && !isStreaming) {
					host = (String) msg.obj;
					startConnect();
				}
				break;
			}
		}

	};
	
	private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		if (info != null && info.getNetworkId() > -1) {
			int i = info.getIpAddress();
			String ip = String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,
					i >> 16 & 0xff, i >> 24 & 0xff);
			tvServerInfo.setText("Server IP: ");
			tvServerInfo.append(ip);
			streamingState(0);
		} else {
			streamingState(2);
		}
	}
	
	private void streamingState(int state) {

	}

	public void log(String s) {
		Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
	}

	// Client
	static class MyVideoView extends VideoView {
		public MyVideoView(Context context) {
			super(context);
		}
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int width = getDefaultSize(0, widthMeasureSpec);
			int height = getDefaultSize(0, heightMeasureSpec);
			setMeasuredDimension(width, height);
		}
	}
	private void getCurrentConfiguration() {
		new AsyncTask<Void,Void,String>() {
			@Override
			protected String doInBackground(Void... params) {
				HttpParams httpParameters = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
				HttpConnectionParams.setSoTimeout(httpParameters, 3000);
		        HttpClient client = new DefaultHttpClient(httpParameters);
		        HttpGet request = new HttpGet("http://"+ host +":8080/config.json?get");
		        ResponseHandler<String> responseHandler = new BasicResponseHandler();
		        String response="";
				try {
					response = client.execute(request, responseHandler);
				} catch (ConnectTimeoutException e) {
					Log.i("getCurrentConfiguration","Connection timeout ! ");
					onCompletion(null);
				} catch (Exception e) {
					Log.e("getCurrentConfiguration","Could not fetch current configuration on remote device !");
					e.printStackTrace();
				}
				return response;
			}
			@Override
			protected void onPostExecute(String response) {
		        try {
					JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
					((CheckBox)findViewById(R.id.checkbox1)).setChecked(object.getBoolean("streamVideo"));
					((CheckBox)findViewById(R.id.checkbox2)).setChecked(object.getBoolean("streamAudio"));
					for (int spinner : new int[]{R.id.spinner1,R.id.spinner2,R.id.spinner3,R.id.spinner4,R.id.spinner5}) {
						Spinner view = (Spinner) findViewById(spinner);
						SpinnerAdapter adapter = view.getAdapter();
						for (int i=0;i<adapter.getCount();i++) {
							Iterator<String> keys = object.keys();
							while (keys.hasNext()) {
								String key = keys.next();
								if (adapter.getItem(i).equals(object.get(key))) {
									view.setSelection(i);
								}
										
							}
						}
					}
					generateURI();
					connectToServer();
				} catch (Exception e) {
					stopStreaming();
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	private void updateSettings() {
		final String oldVideoParameters = videoParameters, oldAudioParameters = audioParameters;
		generateURI();
		if (oldVideoParameters==videoParameters && oldAudioParameters==audioParameters) return;
		stopStreaming();
		progressBar.setVisibility(View.VISIBLE);
		new AsyncTask<Void,Void,Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				HttpClient client = new DefaultHttpClient();
		        //HttpGet request = new HttpGet("http://"+editTextIP.getText().toString()+":8080?set&"+uriParameters);
		        try {
					Thread.sleep(2000);
				} catch (InterruptedException ignore) {}
				return null;
			}
			@Override
			protected void onPostExecute(Void weird) {
				Log.d("updateSettings", "Reconnecting to server...");
				connectToServer();
			}
		}.execute();
	}
	
	/** Generates the URL that will be used to configure the client **/
	private void generateURI() {
		int[] spinners = new int[]{R.id.spinner1,R.id.spinner2,R.id.spinner3,R.id.spinner4,R.id.spinner5};
		videoParameters = "";
		audioParameters = "";
		// Video streaming enabled ?
		if (((CheckBox)findViewById(R.id.checkbox1)).isChecked()) {
			int fps = 0, br = 0, resX = 0, resY = 0;
			Pattern p; Matcher m;
			// User has changed the resolution
			try {
				p = Pattern.compile("(\\d+)x(\\d+)");
				m = p.matcher(((String)((Spinner)findViewById(spinners[0])).getSelectedItem())); m.find();
				resX = Integer.parseInt(m.group(1));
				resY = Integer.parseInt(m.group(2));
			} catch (Exception ignore) {}
			// User has changed the framerate
			try {
				p = Pattern.compile("(\\d+)[^\\d]+");
				m = p.matcher(((String)((Spinner)findViewById(spinners[1])).getSelectedItem())); m.find();
				fps = Integer.parseInt(m.group(1));
			} catch (Exception ignore) {}
			// User has changed the bitrate
			try {
				p = Pattern.compile("(\\d+)[^\\d]+");
				m = p.matcher(((String)((Spinner)findViewById(spinners[2])).getSelectedItem())); m.find();
				br = Integer.parseInt(m.group(1));
			} catch (Exception ignore) {}

			videoParameters += "h264";//((String)((Spinner)findViewById(spinners[3])).getSelectedItem()).equals("H.264")?"h264":"h263";
			videoParameters += "="+br+"-"+fps+"-"+resX+"-"+resY;
		} else {
			videoParameters = "novideo";
		}
		// Audio streaming enabled ?
		if (((CheckBox)findViewById(R.id.checkbox2)).isChecked()) {
			audioParameters += ((String)((Spinner)findViewById(spinners[4])).getSelectedItem()).equals("AMR-NB")?"amr":"aac";
		}
		Log.d("generateURI","Cient configuration: video="+videoParameters+" audio="+audioParameters);
		
	}
	
	/**
	 *  Connect to the RTSP server of the remote phone
	 */
	private void connectToServer() {
		// Start video streaming
		if (videoParameters.length()>0) {
			try {
				videoView = new MyVideoView(this);
				videoView.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.FILL_PARENT,
						LinearLayout.LayoutParams.FILL_PARENT));
				flCameraClient.addView(videoView);
				videoView.setOnPreparedListener(this);
				videoView.setOnCompletionListener(this);
				videoView.setVideoURI(Uri.parse("rtsp://"+host+":" + Constants.RTSP_PORT +"/"+(videoParameters.length()>0?("?"+videoParameters):"")));
				videoView.requestFocus();
			} catch (Exception e) {
				Log.e("connectToServer","connectToServer:videoView: " +e.getMessage());
				e.printStackTrace();
			}
		}
		
		// Start audio streaming
		if (audioParameters.length() > 0) {
			try {
				audioStream.reset();
				audioStream.setDataSource(this, Uri.parse("rtsp://"+"connectToServer" + host+":" + Constants.RTSP_PORT +"/"+(audioParameters.length()>0?("?"+audioParameters):"")));
				audioStream.setAudioStreamType(AudioManager.STREAM_MUSIC);
				audioStream.setOnPreparedListener(new OnPreparedListener() {
					public void onPrepared(MediaPlayer mp) {
						audioStream.start();	
					}
				});
				audioStream.prepareAsync();
			} catch (Exception e) {
				Log.e("connectToServer","audioStream: " +e.getMessage());
				e.printStackTrace();
			} 
		}
		Log.d("connectToServer","connect to rtsp://"+host+":" + Constants.RTSP_PORT + (videoParameters.length()>0?("?"+videoParameters):""));
		
	}
	
	private void stopStreaming() {
		try {
			if (videoView != null && videoView.isPlaying()) {
				flCameraClient.removeView(videoView);
				videoView.stopPlayback();
				videoView = null;
			}
		} catch (Exception ignore) {}
		try {
			if (audioStream != null && audioStream.isPlaying()) {
				audioStream.stop();
				audioStream.reset();
			}
		} catch (Exception ignore) {}
	}
	
	public void onPrepared(MediaPlayer mp) {
		runOnUiThread(new Runnable() {
			public void run() {
				progressBar.setVisibility(View.GONE);
				//layoutControl.setVisibility(View.VISIBLE);
				try {
					videoView.start();
				} catch (Exception e) {
					Log.e("onPrepared", e.getMessage());
				}
			}
		});
	}

	public void onCompletion(MediaPlayer mp) {
		runOnUiThread(new Runnable() {
			public void run() {
				//layoutControl.setVisibility(View.GONE);
				progressBar.setVisibility(View.GONE);
				//layoutForm.setVisibility(View.VISIBLE);
				stopStreaming();
			}
		});
	}
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		
	}
	
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}
	
}
