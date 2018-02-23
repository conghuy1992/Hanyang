package dazone.com.hanyang;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private final int MY_PERMISSIONS_REQUEST_CODE = 1;

    private void setPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, MY_PERMISSIONS_REQUEST_CODE);
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != MY_PERMISSIONS_REQUEST_CODE) {
            return;
        }

        boolean isGranted = true;

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
                break;
            }
        }

        if (isGranted) {
            startApplication();
        } else {
            finish();
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return super.onJsAlert(view, url, message, result);
        }
    }

    private class UpdateRunnable implements Runnable {
        public void run() {
            try {
                URL txtUrl = new URL("http://update.ubimobile.co.kr/Package/Hanyang/android.txt");
                HttpURLConnection urlConnection = (HttpURLConnection) txtUrl.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String serverVersion = bufferedReader.readLine();
                inputStream.close();

                PackageInfo packageInfo = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0);
                String appVersion = packageInfo.versionName;

                if (serverVersion.equals(appVersion)) {
                    return;
                }

                URL apkUrl = new URL("http://update.ubimobile.co.kr/Package/Hanyang/hanyang.apk");
                urlConnection = (HttpURLConnection) apkUrl.openConnection();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());

                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/hanyang.apk";
                FileOutputStream fileOutputStream = new FileOutputStream(filePath);

                byte[] buffer = new byte[4096];
                int readCount;

                while (true) {
                    readCount = bufferedInputStream.read(buffer);
                    if (readCount == -1) {
                        break;
                    }

                    fileOutputStream.write(buffer, 0, readCount);
                    fileOutputStream.flush();
                }

                fileOutputStream.close();
                bufferedInputStream.close();

                mActivityHandler.sendEmptyMessage(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ----------------------------------------------------------------------------------------------

    private static class ActivityHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public ActivityHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.startUpdate();
            }
        }
    }

    private final ActivityHandler mActivityHandler = new ActivityHandler(this);

    private void startUpdate() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("업데이트");
        alert.setMessage("새로운 버전이 있습니다.\n확인 버튼을 터치하시면 설치를 시작합니다.");
        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/hanyang.apk";

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
                startActivity(intent);
            }
        });

        alert.show();
    }

    public void startApplication() {
        Thread updateThread = new Thread(new UpdateRunnable());
        updateThread.setDaemon(true);
        updateThread.start();

        mWebView = (WebView) findViewById(R.id.external_webview);

        WebSettings webSettings = mWebView.getSettings();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setSaveFormData(false);
        } else {
            webSettings.setSaveFormData(false);
            webSettings.setSavePassword(false);
        }

        webSettings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setWebChromeClient(new MyWebChromeClient());

        final Map<String, String> headers = new HashMap<>();
        headers.put("phoneToken", Utils.getUniqueDeviceId(this));

        try {
            PackageInfo packageInfo = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0);
            headers.put("appVersion", packageInfo.versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mWebView.loadUrl("http://hms.hycorp.co.kr/ui/hanyangmobile/default.aspx", headers);
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkPermissions()) {
            startApplication();
        } else {
            setPermissions();
        }


    }

    boolean mDoubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            if (mDoubleBackToExitPressedOnce) {
                mWebView.loadUrl("javascript:$('.lilogout').click()");
                finish();
            }

            this.mDoubleBackToExitPressedOnce = true;
            Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_LONG).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDoubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }
}