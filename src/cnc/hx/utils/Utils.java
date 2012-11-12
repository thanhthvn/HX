package cnc.hx.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cnc.hx.WiFiDirectActivity;

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
	
}
