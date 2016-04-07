package me.hammarstrom.imagerecognition.utilities;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

/**
 * Helper class to ask for permissions at run time.
 *
 * Created by Fredrik Hammarström on 01/04/16.
 */
public class PermissionUtils {

    /**
     * Request for permission
     *
     * @param activity The calling activity
     * @param requestCode A request code to identify the result of the request
     * @param permissions The permissions
     * @return true if permission is granted
     */
    public static boolean requestPermission(Activity activity, int requestCode, String... permissions) {
        boolean granted = true;
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        for (String s : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(activity, s);
            boolean hasPermission = (permissionCheck == PackageManager.PERMISSION_GRANTED);
            granted &= hasPermission;
            if (!hasPermission) {
                permissionsNeeded.add(s);
            }
        }

        if (granted) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activity,
                    permissionsNeeded.toArray(new String[permissionsNeeded.size()]),
                    requestCode);
            return false;
        }
    }
}
