package dazone.com.hanyang;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.Random;

public class Utils {
    public static String getUniqueDeviceId(Context context) {
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        /*
        * returns the MacAddress
        */
        if (TextUtils.isEmpty(deviceId)) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wInfo = wifiManager.getConnectionInfo();
            deviceId = wInfo.getMacAddress();
        }

        if (TextUtils.isEmpty(deviceId)) {
            SharedPreferences pref = context.getSharedPreferences("UniqueDeviceId", Context.MODE_PRIVATE);

            if (!pref.contains("id")) {
                Random random = new Random();
                deviceId = "";

                for (int i = 0; i < 12; i++) {
                    deviceId += String.valueOf(random.nextInt(10));
                }

                SharedPreferences.Editor editor = pref.edit();
                editor.putString("id", deviceId);
                editor.apply();
            } else {
                deviceId = pref.getString("id", "");
            }
        }

        return deviceId;
    }
}