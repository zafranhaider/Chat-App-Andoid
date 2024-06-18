package com.example.codeora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int FILE_CHOOSER_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private WebView webView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request necessary permissions
        requestNecessaryPermissions();

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);

        if (!NetworkUtil.isNetworkAvailable(this)) {
            showErrorPage();
            return;
        }

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setLoadWithOverviewMode(true);

        // Enable built-in zoom controls, if needed
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Set the custom user agent string
        String defaultUserAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(defaultUserAgent + " MyApp");

        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl("https://chat-app-theta-puce-66.vercel.app/");
    }

    private void requestNecessaryPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // All permissions granted, proceed with the functionality
                Log.d(TAG, "Permissions granted");
            } else {
                // Permission denied, handle accordingly
                Log.d(TAG, "Permissions denied");
                Toast.makeText(this, "Permissions denied. The app needs these permissions to function properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void injectLazyLoadingScript() {
        webView.evaluateJavascript(
                "(function() {" +
                        "   window.addEventListener('scroll', function() {" +
                        "       var loader = document.getElementById('loader');" +
                        "       if (!loader) {" +
                        "           loader = document.createElement('div');" +
                        "           loader.id = 'loader';" +
                        "           loader.style.position = 'fixed';" +
                        "           loader.style.bottom = '10px';" +
                        "           loader.style.left = '50%';" +
                        "           loader.style.transform = 'translateX(-50%)';" +
                        "           loader.style.display = 'none';" +
                        "           loader.innerHTML = '<div style=\"width: 30px; height: 30px; border: 4px solid #f3f3f3; border-top: 4px solid #3498db; border-radius: 50%; animation: spin 2s linear infinite;\"></div>';" +
                        "           document.body.appendChild(loader);" +
                        "           var style = document.createElement('style');" +
                        "           style.type = 'text/css';" +
                        "           style.innerHTML = '@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }';" +
                        "           document.head.appendChild(style);" +
                        "       }" +
                        "       var content = document.getElementById('content');" +
                        "       if (!content) {" +
                        "           content = document.body;" +
                        "       }" +
                        "       var lastSection = content.lastElementChild;" +
                        "       var rect = lastSection.getBoundingClientRect();" +
                        "       if (rect.bottom <= window.innerHeight) {" +
                        "           loader.style.display = 'block';" +
                        "           setTimeout(function() {" +
                        "               var newSection = document.createElement('div');" +
                        "               newSection.className = 'section';" +
                        "               newSection.innerHTML = '';" +
                        "               content.appendChild(newSection);" +
                        "               loader.style.display = 'none';" +
                        "           }, 1000);" +
                        "       }" +
                        "   });" +
                        "})()", null);
    }

    private void showErrorPage() {
        Intent intent = new Intent(MainActivity.this, internet.class);
        startActivity(intent);
        finish();
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            Log.d(TAG, "Page started loading: " + url);

            new Handler().postDelayed(() -> {
                if (progressBar.getVisibility() == ProgressBar.VISIBLE) {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Log.d(TAG, "Progress bar hidden due to timeout");
                }
            }, 4000);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(@NonNull WebView view, @NonNull WebResourceRequest request, @NonNull WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e(TAG, "WebView error: " + error.getDescription());
            }
            if (request.isForMainFrame()) {
                showErrorPage();
            }
        }

        @Override
        public void onReceivedHttpError(@NonNull WebView view, @NonNull WebResourceRequest request, @NonNull WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            Log.e(TAG, "HTTP error: " + errorResponse.getReasonPhrase());
            if (request.isForMainFrame()) {
                showErrorPage();
            }
        }
    }

    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (MainActivity.this.filePathCallback != null) {
                MainActivity.this.filePathCallback.onReceiveValue(null);
            }
            MainActivity.this.filePathCallback = filePathCallback;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILE_CHOOSER_REQUEST_CODE);
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }
}
