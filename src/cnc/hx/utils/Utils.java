package cnc.hx.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Utils {
	
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
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		String ip = "";
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	ip = String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
    	}
    	return ip;
	}
}
