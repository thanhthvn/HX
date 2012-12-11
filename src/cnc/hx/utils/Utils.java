package cnc.hx.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import cnc.hx.entities.Device;

public class Utils {
	
	private final static String p2pInt = "p2p-p2p0";
	
	public static ArrayList<Device> getHxDevices(Context context, int port, Handler handler) {
		long start = System.currentTimeMillis();
		ArrayList<String> existIPs = scanIPs(context, port, handler);
		ArrayList<Device> hxDeviceList = new ArrayList<Device>(); 
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 1000);
		HttpConnectionParams.setSoTimeout(httpParameters, 1000);
        HttpClient client;
        HttpGet request;
        ResponseHandler<String> responseHandler;
        for (int i= 0; i< existIPs.size(); i++) {
        	String ip = existIPs.get(i);
        	Log.d("getHxDevices", "Checking IP: " + ip);
	        client = new DefaultHttpClient(httpParameters);
	        String response = "";
			try {
            	request = new HttpGet("http://"+ ip +":8080/config.json?get");
    	        responseHandler = new BasicResponseHandler();
				response = client.execute(request, responseHandler);
				if (response != null) {
					JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
					Boolean isHxDevice = object.getBoolean(CustomHttpServer.HX_DETECt_TAG);
					if (isHxDevice) {
						Device d = new Device();
						d.setIp(ip);
						hxDeviceList.add(d);
						Log.d("getHxDevices", "Device " + ip + " result: " +object.toString());
					}
				}
			} catch (ConnectTimeoutException e) {
				Log.i("getHxDevices","Connection timeout!");
			} catch (Exception e) {
				Log.i("getHxDevices","Exception");
			}
        }
        long time = System.currentTimeMillis() - start;
        Toast.makeText(context, "Found: " + hxDeviceList.size() +  " available HX devices, total time: " + time, Toast.LENGTH_LONG).show();
		return hxDeviceList;
	}
	
	public static ArrayList<String>  scanIPs(Context context, int port, Handler handler) {
		int[] myIpArr = getIpArray(context);
		ArrayList<String> existIPs = new ArrayList<String>();
		if (myIpArr != null) {
			final int timeout = 300;
			String ipMask = myIpArr[0] + "." + myIpArr[1] + "." + myIpArr[2] + ".";
			int myIpHost = myIpArr[3];
			String searchIp;
			int count = 0;
			for (int i = 2; i < 255; i++) {
				if (i != myIpHost) {
					try {
						searchIp = ipMask + String.valueOf(i);
						handler.obtainMessage(1, searchIp).sendToTarget();
						Log.d("Check IP", "IP: " + searchIp);
						//Socket socket = new Socket(ip, port);
			        	//socket.setSoTimeout(timeout);
			            Socket socket = new Socket();
			        	socket.connect(new InetSocketAddress(searchIp, port), timeout);
			        	socket.close();
			        	socket = null;
			        	Log.d("Check IP", ">>>>> IP: " + searchIp + " is OK");
				        existIPs.add(searchIp);
						Toast.makeText(context, "Found device: "  + searchIp, Toast.LENGTH_SHORT).show();
						//	InetAddress in = InetAddress.getByName(searchIp);
						//	if (in.isReachable(timeout)) {
				        //		Log.d("Check IP", ">>>>> IP: " + searchIp + " is OK");
				        //		existIPs.add(searchIp);
				        //	}
					} catch (Exception e) {
						//e.printStackTrace();
					}
				}
			}
			Log.d("scanIPs", "Total exist IPs: " + count);
		}
		return existIPs;
	}
	
	public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                Log.d("copyFile", "Buf size: " + len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d("copyFile", e.toString());
            return false;
        }
        return true;
    }
	
	public static String getIpAddress(Context context) {
		int[] ips = getIpArray(context);
		String ip = String.format("%d.%d.%d.%d", ips[0], ips[1], ips[2], ips[3]);
    	return ip;
	}
	
	public static int[] getIpArray(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		int[] ips = new int[4];
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	ips[0] = i & 0xff;
	    	ips[1] = i >> 8 & 0xff;
	    	ips[2] = i >> 16 & 0xff;
	    	ips[3] = i >> 24 & 0xff;
    	}
    	return ips;
	}
	
	public static String getIPFromMac(String MAC) {
		/*
		 * method modified from:
		 * 
		 * http://www.flattermann.net/2011/02/android-howto-find-the-hardware-mac-address-of-a-remote-host/
		 * 
		 * */
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;
			while ((line = br.readLine()) != null) {

				String[] splitted = line.split(" +");
				if (splitted != null && splitted.length >= 4) {
					// Basic sanity check
					String device = splitted[5];
					if (device.matches(".*" +p2pInt+ ".*")){
						String mac = splitted[3];
						//if (mac.matches(MAC)) {
							return splitted[0];
						//}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}


	public static String getLocalIPAddress() {
		/*
		 * modified from:
		 * 
		 * http://thinkandroid.wordpress.com/2010/03/27/incorporating-socket-programming-into-your-applications/
		 * 
		 * */
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();

					String iface = intf.getName();
					if(iface.matches(".*" +p2pInt+ ".*")){
						if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
							return getDottedDecimalIP(inetAddress.getAddress());
						}
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		} catch (NullPointerException ex) {
			Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		}
		return null;
	}

	private static String getDottedDecimalIP(byte[] ipAddr) {
		/*
		 * ripped from:
		 * 
		 * http://stackoverflow.com/questions/10053385/how-to-get-each-devices-ip-address-in-wifi-direct-scenario
		 * 
		 * */
		String ipAddrStr = "";
		for (int i=0; i<ipAddr.length; i++) {
			if (i > 0) {
				ipAddrStr += ".";
			}
			ipAddrStr += ipAddr[i]&0xFF;
		}
		return ipAddrStr;
	}
	
}
