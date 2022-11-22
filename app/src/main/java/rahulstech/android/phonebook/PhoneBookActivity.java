package rahulstech.android.phonebook;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import rahulstech.android.phonebook.util.Check;

public abstract class PhoneBookActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasContactPermissions()) {
            requestContactPermissions();
        }
        else {
            onAllPermissionsGranted(CONTACT_PERMISSION_CODE);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                  Runtime Permission                                     ///
    //////////////////////////////////////////////////////////////////////////////////////////////

    public static final int CONTACT_PERMISSION_CODE = 1;

    public static final int CALL_PERMISSION_CODE = 2;

    public static final String[] CONTACT_PERMISSIONS = new String[]{
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
    };

    public static final String[] CALL_PERMISSIONS = new String[]{
            Manifest.permission.CALL_PHONE
    };

    public boolean hasContactPermissions() {
        return hasPermissions(CONTACT_PERMISSIONS);
    }

    public boolean hasCallPermission() {
        return hasPermissions(CALL_PERMISSIONS);
    }

    public boolean hasPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // ask runtime permission for sdk >= 23
            for (String permission : permissions) {
                if (PackageManager.PERMISSION_DENIED
                        == ActivityCompat.checkSelfPermission(this,permission))
                    return false;
            }
        }
        return true;
    }

    public void requestContactPermissions() {
        ActivityCompat.requestPermissions(this, CONTACT_PERMISSIONS, CONTACT_PERMISSION_CODE);
    }

    public void requestCallPermission() {
        ActivityCompat.requestPermissions(this, CALL_PERMISSIONS, CALL_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            onAllPermissionsGranted(requestCode);
        }
        else {
            onSomePermissionsDenied(requestCode);
        }
    }

    public void onAllPermissionsGranted(int requestCode) {}

    public void onSomePermissionsDenied(int requestCode) {
        Toast.makeText(this,R.string.message_permission_not_granted,Toast.LENGTH_SHORT).show();
        if (CONTACT_PERMISSION_CODE == requestCode) {
            finish();
        }
    }
}
