package com.zeng.hui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zeng.hui.databinding.ActivityBrowserBinding;

public class BrowserActivity extends AppCompatActivity {

    private ActivityBrowserBinding mBinding = null;
    private boolean mMasterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_browser);

        mBinding.gobackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinding.webView.canGoBack()) {
                    mBinding.webView.goBack();
                } else {
                    finish();
                }
            }
        });

        initToolbar();
        initWebView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
            if (mBinding.webView.canGoBack()) {
                mBinding.webView.goBack();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding.webView.removeAllViews();
        mBinding.webView.destroy();
    }

    private void initToolbar(){
        mBinding.shareImageView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, mBinding.webView.getOriginalUrl());
                startActivity(Intent.createChooser(share, getString(R.string.share_title)));
            }
        });

        mBinding.refreshImageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mBinding.webView.loadUrl(mBinding.webView.getUrl());
            }
        });

        mBinding.closeImageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initWebView(){
        WebSettings settings = mBinding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("utf8");
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDisplayZoomControls(false);

        mBinding.webView.setHorizontalScrollBarEnabled(false);
        mBinding.webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mBinding.progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBinding.progressBar.setVisibility(View.GONE);
                        }
                    }, 500);
                }
                else {
                    mBinding.progressBar.setVisibility(View.VISIBLE);
                }
            }
        });
        mBinding.webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (URLUtil.isNetworkUrl(url)) {
                    if (mMasterMode) {
                        Intent intent = new Intent(BrowserActivity.this, BrowserActivity.class);
                        intent.putExtra("url", url);
                        startActivity(intent);
                    } else {
                        view.loadUrl(url);
                    }
                } else {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    PackageManager pkM = getPackageManager();
                    ResolveInfo info = pkM.resolveActivity(intent, 0);
                    if (info != null) {
                        String appName = pkM.getApplicationLabel(info.activityInfo.applicationInfo).toString();
                        Drawable appIcon = pkM.getApplicationIcon(info.activityInfo.applicationInfo);
                        new AlertDialog.Builder(BrowserActivity.this)
                                .setIcon(appIcon)
                                .setTitle(String.format(getString(R.string.active_external_app_note), appName))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                    }
                                })
                                .show();
                    }
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mBinding.titleTextView.setText(R.string.web_title_loading);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mBinding.titleTextView.setText(view.getTitle());
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                view.loadData(String.format(getString(R.string.web_error_info), errorCode, description), "text/html", "utf8");
            }
        });
        mBinding.webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        mBinding.webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String s, String s1, String s2, String s3, long l) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }catch (android.content.ActivityNotFoundException anfe){}
            }
        });

        mBinding.webView.addJavascriptInterface(new JsInterface(), "ajs");

        String url = getIntent().getStringExtra("url");
        mBinding.webView.loadUrl(url);
    }

    class JsInterface {

        @JavascriptInterface
        public void actionSend(final String title, final String content) {
            mBinding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    Intent share = new Intent(android.content.Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, content);
                    startActivity(Intent.createChooser(share, title));
                }
            });
        }

        @JavascriptInterface
        public void actionView(final String url) {
            mBinding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException anfe) {
                    }
                }
            });
        }

        @JavascriptInterface
        public String getVersionName() {
            String versionName = "";
            try {
                versionName = BrowserActivity.this.getPackageManager().getPackageInfo(BrowserActivity.this.getPackageName(), 0).versionName;
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            }

            return versionName;
        }

        @JavascriptInterface
        public String getPackageName() {
            return BrowserActivity.this.getPackageName();
        }

        @JavascriptInterface
        public void loadUrl(final String url) {
            mBinding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(BrowserActivity.this, BrowserActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    intent.putExtra("url", url);
                    startActivity(intent);
                }
            });
        }

        @JavascriptInterface
        public void setMasterMode(boolean enable) {
            mMasterMode = enable;
        }
    }
}
