package com.taskearn.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private TaskEarnBridge bridge;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // -----------------------------------------
        // GOOGLE MOBILE ADS INITIALIZATION
        // -----------------------------------------
        MobileAds.initialize(
                this,
                initializationStatus -> {
                    // AdMob SDK initialized
                }
        );

        // -----------------------------------------
        // WEBVIEW
        // -----------------------------------------
        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // The app's HTML pages use a native Android bridge for
        // Google Mobile Ads. File access is not needed for those
        // pages, but WebView must keep normal JavaScript/DOM
        // execution enabled.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setMediaPlaybackRequiresUserGesture(false);

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.LOLLIPOP) {

            settings.setMixedContentMode(
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
            );
        }

        WebView.setWebContentsDebuggingEnabled(false);

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        // -----------------------------------------
        // TASK EARN NATIVE BRIDGE
        // -----------------------------------------
        bridge =
                new TaskEarnBridge(this);

        webView.addJavascriptInterface(
                bridge,
                "AndroidAds"
        );

        webView.addJavascriptInterface(
                bridge,
                "TaskEarnAds"
        );

        // Exact name expected by ad-revenue-share.html.
        webView.addJavascriptInterface(
                bridge,
                "AndroidTaskEarnAds"
        );

        // VERY IMPORTANT:
        // Java bridge ko WebView reference dena
        // taaki native callbacks HTML/JS tak pahunch sake.
        bridge.setWebView(webView);

        // -----------------------------------------
        // LOAD APP
        // -----------------------------------------
        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.removeJavascriptInterface(
                    "AndroidAds"
            );

            webView.removeJavascriptInterface(
                    "TaskEarnAds"
            );

            webView.removeJavascriptInterface(
                    "AndroidTaskEarnAds"
            );

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}