package com.zeng.hui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zeng.hui.databinding.ActivityBrowserBinding;

public class BrowserActivity extends AppCompatActivity {

    private ActivityBrowserBinding mBinding = null;
    private boolean mWebError = false;
    private boolean mMasterMode = false;
    private boolean mGoBackIng = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_browser);

        mBinding.gobackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBinding.webView.canGoBack()) {
                    mGoBackIng = true;
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
                mGoBackIng = true;
                mBinding.webView.goBack();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
                mWebError = false;
                mGoBackIng = false;
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
        mBinding.webView.getSettings().setJavaScriptEnabled(true);
        mBinding.webView.getSettings().setDefaultTextEncodingName("utf8");
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

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (!mWebError) {
                    mBinding.titleTextView.setText(title);
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
                        mBinding.titleTextView.setText(url);
                        mGoBackIng = false;
                        view.loadUrl(url);
                    }
                } else {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException anfe) {
                    }
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (mGoBackIng) {
                    mBinding.titleTextView.setText(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mGoBackIng) {
                    mBinding.titleTextView.setText(view.getTitle());
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                mWebError = true;
                view.loadData(String.format(getString(R.string.web_error_info), errorCode, description), "text/html", "utf8");
            }
        });
        mBinding.webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });

        mBinding.webView.addJavascriptInterface(new JsInterface(), "hui");

        String url = getIntent().getStringExtra("url");
        mBinding.titleTextView.setText(url);
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
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                    }
                }
            });
        }

        @JavascriptInterface
        public void loadUrl(final String url) {
            mBinding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(BrowserActivity.this, BrowserActivity.class);
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
