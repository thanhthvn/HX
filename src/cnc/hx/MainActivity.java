package cnc.hx;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import cnc.hx.adapters.DeviceAdapter;
import cnc.hx.entities.Device;
import cnc.hx.utils.Constants;
import cnc.hx.utils.CustomHttpServer;
import cnc.hx.utils.Utils;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener, 
		OnCompletionListener, OnPreparedListener, OnItemSelectedListener {

	Button btBuzzer, btAvOnOff, btSettings, btConnectOnOff, btExit;
	String clientIp;
	AlertDialog deviceDialog;
	DeviceAdapter adapter;
	ArrayList<Device> deviceList;
	TextView tvInfo, tvServerInfo;
	private SurfaceHolder holder;
	private SurfaceView svCameraServer;

	LinearLayout llConnectForm, llConversation;
	RelativeLayout rlDashboard;
	
	private MediaPlayer mediaPlayer;
	private SurfaceView svCameraClient;
	private SurfaceHolder shCameraClient;
	SurfaceHolder.Callback callback;
	private int mVideoWidth;
    private int mVideoHeight;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
	ProgressDialog progressDialog, searchProgressDialog;

	CustomHttpServer httpServer = null;
	RtspServer rtspServer = null;
	boolean streaming = false;
	
	// Client
	private SharedPreferences settings;
	Button btConnect;
	private EditText etServerIp;
	private VideoView videoView;
	private MediaPlayer audioStream;
	private FrameLayout flCameraClient;
	private ProgressBar progressBar;
	private String videoParameters = "", audioParameters = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btBuzzer = (Button) findViewById(R.id.btBuzzer);
		btAvOnOff = (Button) findViewById(R.id.btAvOnOff);
		btSettings = (Button) findViewById(R.id.btSettings);
		btConnectOnOff = (Button) findViewById(R.id.btConnectOnOff);
		btExit = (Button) findViewById(R.id.btExit);
		tvInfo = (TextView) findViewById(R.id.tvInfo);
		tvServerInfo = (TextView) findViewById(R.id.tvServerInfo);

		llConversation = (LinearLayout) findViewById(R.id.llConversation);
		rlDashboard = (RelativeLayout) findViewById(R.id.rlDashboard);
		
		btBuzzer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				createDeviceDialog();
			}
		});
		btAvOnOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

			}
		});
		btConnectOnOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopStreaming();
			}
		});
		btSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,OptionsActivity.class);
		        startActivityForResult(intent, 0);
			}
		});
		btExit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		rtspServer = new RtspServer(Constants.RTSP_PORT, handler);
		httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);

		// Server
		svCameraServer = (SurfaceView) findViewById(R.id.svCameraServer);
		tvServerInfo = (TextView) findViewById(R.id.tvServerInfo);
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

		// Client
		btConnect = (Button) findViewById(R.id.btConnect);
		etServerIp = (EditText) findViewById(R.id.etServerIp);
		flCameraClient = (FrameLayout) findViewById(R.id.flCameraClient);
		llConnectForm = (LinearLayout) findViewById(R.id.llConnectForm);
		progressBar = (ProgressBar)findViewById(R.id.progress);
		
		// llConnectForm.setVisibility(View.GONE);
		
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
					clientIp = etServerIp.getText().toString();
					startConnect();
				}
		});
		
		/*btStop = (ImageView) findViewById(R.id.btStop);
		btStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		        stopServer();
			}
		}); */
		
		createMediaPlayer();	
	}

	void createMediaPlayer() {
		svCameraClient = (SurfaceView) findViewById(R.id.svCameraClient);
		shCameraClient = svCameraClient.getHolder();
		//shCameraClient.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		shCameraClient.setKeepScreenOn(true);
		mediaPlayer = new MediaPlayer();
		
		callback = new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				try {
					mediaPlayer.setDisplay(shCameraClient);
				} catch (Exception e) {
					
				}
				Log.d("SurfaceHolder.Callback", "surfaceCreated");
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (mediaPlayer != null) {
					try {
						releaseMediaPlayer();
					} catch (Exception e) {
						
					}
				}
				Log.d("SurfaceHolder.Callback", "surfaceDestroyed");
				
			}
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
					int height) {
				Log.d("SurfaceHolder.Callback", "surfaceChanged");
			}
		};
		shCameraClient.addCallback(callback); 
		//mediaPlayer.setLooping(true);
		mediaPlayer.setScreenOnWhilePlaying(true);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setOnInfoListener(new OnInfoListener() {
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				Log.d("mediaPlayer", "OnInfoListener: what: " + what + ", extra: " + extra);
				return false;
			}
		});
		mediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				Log.d("mediaPlayer", "OnBufferingUpdateListener, percent: " + percent);
			}
		});
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				Log.d("mediaPlayer", "OnPreparedListener");
				mIsVideoReadyToBePlayed = true;
		        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
		            startVideoPlayback();
		        }
			}
		});
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			public boolean onError(MediaPlayer mp, int what, int extra) {
				mediaPlayer.reset();
				Log.d("mediaPlayer", "OnErrorListener: what: " + what + ", extra: " + extra);
				return false;
			}
		});
		mediaPlayer.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {
			public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
				if (width == 0 || height == 0) {
		            Log.e("mediaPlayer", "invalid video width(" + width + ") or height(" + height + ")");
		            return;
		        }
		        mIsVideoSizeKnown = true;
		        mVideoWidth = width;
		        mVideoHeight = height;
		        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
		            startVideoPlayback();
		        }
			}
		});
		mediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			public void onSeekComplete(MediaPlayer mp) {
				Log.d("SurfaceHolder.Callback", "onSeekComplete");
			}
		});
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				Log.d("SurfaceHolder.Callback", "onCompletion");
			}
		});
		

	}
	
	private void startVideoPlayback() {
        Log.v("startVideoPlayback", "startVideoPlayback");
        progressBar.setVisibility(View.GONE);
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        mediaPlayer.start();
    }
	 
	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		startServers();
		displayIpAddress();
	}

	public void onPause() {
		super.onPause();
		if (progressDialog != null) progressDialog.dismiss();
		if (searchProgressDialog != null) searchProgressDialog.dismiss();
		if (rtspServer != null) rtspServer.stop();
		//if (httpServer != null) httpServer.stop();
		releaseMediaPlayer();
		doCleanUp();
		CustomHttpServer.setScreenState(false);
		unregisterReceiver(wifiStateReceiver);
	}

	public void onDestroy() {
		super.onDestroy();
		releaseMediaPlayer();
		doCleanUp();
		if (httpServer != null) httpServer.stop();
		if (rtspServer != null) rtspServer.stop();
	}

	void startMediaPlayer() {
		showConversation(true);
		new Thread(new Runnable() {
			public void run() {
				try {
					if (mediaPlayer == null) createMediaPlayer();
					//clientIp = "10.0.2.30";
					String mediaUrl = "rtsp://" + clientIp + ":" + Constants.RTSP_PORT
							+ "/" + (videoParameters.length() > 0 ? ("?" + videoParameters) : "");
					Log.d("createMediaPlayer", mediaUrl);
					//mediaPlayer.setDataSource("http://sv1.moviehd.vn/new/The.Punisher.2004/The.Punisher.2004.mHD.BluRay.DD-EX.x264-EPiK_clip1.mp4");
					mediaPlayer.setDataSource(MainActivity.this, Uri.parse(mediaUrl));
					//mediaPlayer.prepare();
					mediaPlayer.prepareAsync();
					mediaPlayer.start();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		
	}
	
	private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
        	mediaPlayer.release();
        	mediaPlayer = null;
        }
    }

    private void doCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }
    
	class SeachDeviceTask extends AsyncTask<Void, Void, Void> {
		
		Handler handlerSearch = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.what == 1) {
					if (searchProgressDialog != null) {
						String m = "seaching...\nIP: " + msg.obj;
						searchProgressDialog.setMessage(m);
						searchProgressDialog.show();
					}
				}
			}
		};
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			searchProgressDialog = new ProgressDialog(MainActivity.this);
			searchProgressDialog.setMessage("seaching...");
			searchProgressDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			Looper.prepare();
			deviceList = Utils.getHxDevices(MainActivity.this, 8080, handlerSearch);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (searchProgressDialog != null) {
				try {
					searchProgressDialog.dismiss();
				} catch (Exception e) {}
			}
			displayIpAddress();
			createDeviceDialog();
		}

	}
	
	private void displayIpAddress() {
		tvInfo.setText("My IP: " + Utils.getIpAddress(this));
		tvServerInfo.setText("My IP: " + Utils.getIpAddress(this));
	}

	private void startServers() {
		if (rtspServer != null) {
			try {
				rtspServer.start();
			} catch (IOException e) {
				log("RtspServer could not be started : " + e.getMessage());
			}
		}
		if (httpServer != null) {
			CustomHttpServer.setScreenState(true);
			try {
				httpServer.start();
			} catch (IOException e) {
				log("HttpServer could not be started : " + e.getMessage());
			}
		}
	}

	public void startConnect() {
		showConversation(true);
		if (deviceDialog != null) deviceDialog.dismiss();
		if (clientIp != null) {
			if (!clientIp.equalsIgnoreCase("")) {
				etServerIp.setText(clientIp);
				if (!streaming) {
					Editor editor = settings.edit();
					editor.putString("last_server_ip", clientIp);
					editor.commit();
					progressBar.setVisibility(View.VISIBLE);
					getCurrentConfiguration();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(etServerIp.getWindowToken(), 0);
					btConnect.setText(R.string.disconnect);	
					Toast.makeText(this, "Streaming from " + clientIp, Toast.LENGTH_LONG).show();
					streaming = true;
				} else {
					stopStreaming();
					btConnect.setText(R.string.connect);
					progressBar.setVisibility(View.GONE);
					streaming = false;
				}
			}
		}
	}

	private void createDeviceDialog() {
		deviceList = new ArrayList<Device>();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.device_list);
		Device d1 = new Device();
		d1.setIp("10.0.2.37");
		Device d2 = new Device();
		d2.setIp("10.0.2.71");
		Device d3 = new Device();
		d3.setIp("10.0.2.41");
		Device d4 = new Device();
		d4.setIp("192.168.1.115");
		deviceList.add(d1);
		deviceList.add(d2);
		deviceList.add(d3);
		deviceList.add(d4);
		adapter = new DeviceAdapter(this, deviceList);
		if (deviceList.isEmpty()) {
			builder.setMessage(R.string.no_device);
		} else {
			builder.setAdapter(adapter, null);
		}
		builder.setPositiveButton(R.string.search,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					new SeachDeviceTask().execute();
				}
			});
		builder.setNegativeButton(R.string.close,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		deviceDialog = builder.create();
		deviceDialog.show();
	}

	// BroadcastReceiver that detects wifi state changements
	private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// This intent is also received when app resumes even if wifi state
			// hasn't changed :/
			if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				if (!streaming) displayIpAddress();
			}
		}
	};

	public void log(String s) {
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}

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
				progressBar.setVisibility(View.GONE);
				break;
			case Session.MESSAGE_ERROR:
				log((String) msg.obj);
				break;
			case RtspServer.MESSAGE_CONNECT:
				showConversation(true);
				log((String) msg.obj);
				break;
			case RtspServer.MESSAGE_DISCONNECT:
				showConversation(false);
				log((String) msg.obj);
				break;	
			case RtspServer.MESSAGE_CLIENT_IP:
				if (clientIp == null && !streaming) {
					clientIp = (String) msg.obj;
					// startConnect();
				}
				break;
			}
		}

	};
	
	private void streamingState(int state) {

	}

	void showConversation(Boolean isConversation) {
		if (isConversation) {
			//llConversation.setVisibility(View.VISIBLE);
			//rlDashboard.setVisibility(View.GONE);
		} else {
			//llConversation.setVisibility(View.GONE);
			//rlDashboard.setVisibility(View.VISIBLE);
		}
		
	}
	
	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("video_resX")) {
			Session.defaultVideoQuality.resX = sharedPreferences.getInt( "video_resX", 0);
		} else if (key.equals("video_resY")) {
			Session.defaultVideoQuality.resY = sharedPreferences.getInt("video_resY", 0);
		} else if (key.equals("video_framerate")) {
			Session.defaultVideoQuality.frameRate = Integer
					.parseInt(sharedPreferences.getString("video_framerate", "0"));
		} else if (key.equals("video_bitrate")) {
			Session.defaultVideoQuality.bitRate = Integer
					.parseInt(sharedPreferences.getString("video_bitrate", "0")) * 1000;
		} else if (key.equals("stream_audio") || key.equals("audio_encoder")) {
			Session.setDefaultAudioEncoder(sharedPreferences.getBoolean(
					"stream_audio", true) ? Integer.parseInt(sharedPreferences.getString("audio_encoder", "3")) : 0);
		} else if (key.equals("stream_video") || key.equals("video_encoder")) {
			Session.setDefaultVideoEncoder(sharedPreferences.getBoolean(
					"stream_video", true) ? Integer.parseInt(sharedPreferences.getString("video_encoder", "2")) : 0);
		} else if (key.equals("enable_http")) {
			if (sharedPreferences.getBoolean("enable_http", true)) {
				if (httpServer == null)
					httpServer = new CustomHttpServer(8080, this.getApplicationContext(), handler);
			} else {
				if (httpServer != null) httpServer = null;
			}
		} else if (key.equals("enable_rtsp")) {
			if (sharedPreferences.getBoolean("enable_rtsp", true)) {
				if (rtspServer == null)
					rtspServer = new RtspServer(Constants.RTSP_PORT, handler);
			} else {
				if (rtspServer != null) rtspServer = null;
			}
		}
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
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				HttpParams httpParameters = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
				HttpConnectionParams.setSoTimeout(httpParameters, 3000);
				HttpClient client = new DefaultHttpClient(httpParameters);
				HttpGet request = new HttpGet("http://" + clientIp + ":8080/config.json?get");
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String response = "";
				try {
					response = client.execute(request, responseHandler);
				} catch (ConnectTimeoutException e) {
					Log.i("getCurrentConfiguration", "Connection timeout ! ");
					onCompletion(null);
				} catch (Exception e) {
					Log.e("getCurrentConfiguration", "Could not fetch current configuration on remote device !");
					e.printStackTrace();
				}
				return response;
			}

			@Override
			protected void onPostExecute(String response) {
				try {
					JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
					((CheckBox) findViewById(R.id.checkbox1)).setChecked(object.getBoolean("streamVideo"));
					((CheckBox) findViewById(R.id.checkbox2)).setChecked(object.getBoolean("streamAudio"));
					for (int spinner : new int[] { R.id.spinner1, R.id.spinner2, R.id.spinner3, R.id.spinner4, R.id.spinner5 }) {
						Spinner view = (Spinner) findViewById(spinner);
						SpinnerAdapter adapter = view.getAdapter();
						for (int i = 0; i < adapter.getCount(); i++) {
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
		if (oldVideoParameters == videoParameters
				&& oldAudioParameters == audioParameters)
			return;
		stopStreaming();
		progressBar.setVisibility(View.VISIBLE);
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				HttpClient client = new DefaultHttpClient();
				// HttpGet request = new // HttpGet("http://"+editTextIP.getText().toString()+":8080?set&"+uriParameters);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignore) {
				}
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
		int[] spinners = new int[] { R.id.spinner1, R.id.spinner2, R.id.spinner3, R.id.spinner4, R.id.spinner5 };
		videoParameters = "";
		audioParameters = "";
		// Video streaming enabled ?
		if (((CheckBox) findViewById(R.id.checkbox1)).isChecked()) {
			int fps = 0, br = 0, resX = 0, resY = 0;
			Pattern p;
			Matcher m;
			// User has changed the resolution
			try {
				p = Pattern.compile("(\\d+)x(\\d+)");
				m = p.matcher(((String) ((Spinner) findViewById(spinners[0])).getSelectedItem()));
				m.find();
				resX = Integer.parseInt(m.group(1));
				resY = Integer.parseInt(m.group(2));
			} catch (Exception ignore) {
			}
			// User has changed the framerate
			try {
				p = Pattern.compile("(\\d+)[^\\d]+");
				m = p.matcher(((String) ((Spinner) findViewById(spinners[1])).getSelectedItem()));
				m.find();
				fps = Integer.parseInt(m.group(1));
			} catch (Exception ignore) {
			}
			// User has changed the bitrate
			try {
				p = Pattern.compile("(\\d+)[^\\d]+");
				m = p.matcher(((String) ((Spinner) findViewById(spinners[2])).getSelectedItem()));
				m.find();
				br = Integer.parseInt(m.group(1));
			} catch (Exception ignore) {
			}
			videoParameters += ((String) ((Spinner) findViewById(spinners[3])).getSelectedItem()).equals("H.264") ? "h264" : "h263";
			// "h264";
			videoParameters += "=" + br + "-" + fps + "-" + resX + "-" + resY;
		} else {
			videoParameters = "novideo";
		}
		// Audio streaming enabled ?
		if (((CheckBox) findViewById(R.id.checkbox2)).isChecked()) {
			audioParameters += ((String) ((Spinner) findViewById(spinners[4])).getSelectedItem()).equals("AMR-NB") ? "amr" : "aac";
		}
		Log.d("generateURI", "Cient configuration: video=" + videoParameters + " audio=" + audioParameters);

	}

	/**
	 * Connect to the RTSP server of the remote phone
	 */
	private void connectToServer() {
		startMediaPlayer(); 
		// Start video streaming
		if (videoParameters.length() > 0) {
			try {
				
				/*videoView = new VideoView(this);
				videoView.setLayoutParams(
					new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.MATCH_PARENT));
				flCameraClient.addView(videoView);
				videoView.setOnPreparedListener(this);
				videoView.setOnCompletionListener(this);
				videoView.setVideoURI(Uri.parse("rtsp://" + clientIp + ":" + Constants.RTSP_PORT
								+ "/" + (videoParameters.length() > 0 ? ("?" + videoParameters) : "")));
				//videoView.setVideoURI(Uri.parse("http://sv1.moviehd.vn/new/The.Punisher.2004/The.Punisher.2004.mHD.BluRay.DD-EX.x264-EPiK_clip1.mp4"));
				videoView.requestFocus();*/
			} catch (Exception e) {
				Log.e("connectToServer", "connectToServer:videoView: " + e.getMessage());
				e.printStackTrace();
			}
		}
		/*
		// Start audio streaming
		if (audioParameters.length() > 0) {
			try {
				audioStream.reset();
				audioStream.setDataSource(this, Uri.parse("rtsp://" + "connectToServer" + clientIp+ ":"+ Constants.RTSP_PORT
						+ "/" + (audioParameters.length() > 0 ? ("?" + audioParameters) : "")));
				audioStream.setAudioStreamType(AudioManager.STREAM_MUSIC);
				audioStream.setOnPreparedListener(new OnPreparedListener() {
					public void onPrepared(MediaPlayer mp) {
						audioStream.start();
					}
				});
				audioStream.prepareAsync();
			} catch (Exception e) {
				Log.e("connectToServer", "audioStream: " + e.getMessage());
				e.printStackTrace();
			}
		}*/
		Log.d("connectToServer", "connect to rtsp://" + clientIp + ":" + Constants.RTSP_PORT + (videoParameters.length() > 0 ? ("?" + videoParameters) : ""));

	}

	private void stopStreaming() {
		releaseMediaPlayer();
		doCleanUp();
		try {
			Log.d("stopStreaming", "stopStreaming");
			if (videoView != null && videoView.isPlaying()) {
				flCameraClient.removeView(videoView);
				videoView.stopPlayback();
				videoView = null;
			}
		} catch (Exception ignore) {
		}
		try {
			if (audioStream != null && audioStream.isPlaying()) {
				audioStream.stop();
				audioStream.reset();
			}
		} catch (Exception ignore) {
		}
		progressBar.setVisibility(View.GONE);
		showConversation(false);
	}

	public void onPrepared(MediaPlayer mp) {
		runOnUiThread(new Runnable() {
			public void run() {
				progressBar.setVisibility(View.GONE);
				try {
					Log.d("onPrepared", "videoView.start()");
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
				progressBar.setVisibility(View.GONE);
				Log.d("onCompletion", "stopStreaming");
				stopStreaming();
			}
		});
	}

	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

	}

	public void onNothingSelected(AdapterView<?> arg0) {

	}
	
}
