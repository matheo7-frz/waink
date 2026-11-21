package be.math0235.noxscan;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;

// WAINK — fait par math_0235
public class MainActivity extends Activity {
    private static final int PICK = 42, CAM = 43;
    private static final String HOME = "https://appassets.androidplatform.net/assets/index.html";
    private WebView web;
    private ValueCallback<Uri[]> callback;
    private File photoFile;
    private Uri photoUri;
    private PermissionRequest pending;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF000000);
        getWindow().setNavigationBarColor(0xFF000000);
        web = new WebView(this);
        web.setBackgroundColor(0xFF120B24);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        // Sert les fichiers de l'appli en https pour que la caméra en direct soit autorisée
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                    return true;
                }
                return false;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest req) {
                runOnUiThread(() -> {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        req.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        pending = req;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAM);
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (callback != null) callback.onReceiveValue(null);
                callback = cb;
                Intent[] extra = new Intent[0];
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        File dir = new File(getCacheDir(), "photos");
                        dir.mkdirs();
                        photoFile = File.createTempFile("scan_", ".jpg", dir);
                        photoUri = FileProvider.getUriForFile(MainActivity.this, "be.math0235.noxscan.files", photoFile);
                        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        camera.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        extra = new Intent[]{camera};
                    } catch (Exception e) {
                        photoFile = null;
                    }
                }
                Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
                gallery.addCategory(Intent.CATEGORY_OPENABLE);
                gallery.setType("image/*");
                Intent chooser = Intent.createChooser(gallery, "Photo de la voiture");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extra);
                try {
                    startActivityForResult(chooser, PICK);
                } catch (Exception e) {
                    callback = null;
                    return false;
                }
                return true;
            }
        });

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAM);
        }
        web.loadUrl(HOME);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req != CAM) return;
        boolean ok = res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED;
        if (pending != null) {
            if (ok) pending.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            else pending.deny();
            pending = null;
        } else if (ok) {
            web.reload(); // relance le viseur après l'autorisation
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK || callback == null) return;
        Uri[] result = null;
        if (res == RESULT_OK) {
            if (data != null && data.getData() != null) result = new Uri[]{data.getData()};
            else if (photoFile != null && photoFile.length() > 0) result = new Uri[]{photoUri};
        }
        callback.onReceiveValue(result);
        callback = null;
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
