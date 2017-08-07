package com.zeng.hui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.zeng.hui.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;
    private String mShareTitle;
    private String mShareContent;
    private boolean mWebError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        initStatusBar();
        initToolbar();
        initWebView();
    }

    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
            if((System.currentTimeMillis()-exitTime) > 2000){
                Toast.makeText(getApplicationContext(), R.string.exit_hint, Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @TargetApi(19)
    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // 生成一个状态栏大小的矩形
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            int statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            View statusView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    statusBarHeight);
            statusView.setLayoutParams(params);
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
            statusView.setBackgroundColor(typedValue.data);

            // 添加 statusView 到布局中
            ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
            decorView.addView(statusView);

            // 设置根布局的参数
            ViewGroup rootView = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
            rootView.setFitsSystemWindows(true);
            rootView.setClipToPadding(true);
        }
    }

    private void initToolbar(){
        mBinding.shareImageView.setVisibility(View.GONE);
        mBinding.shareImageView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, mShareContent);
                startActivity(Intent.createChooser(share, mShareTitle));
            }
        });

        mBinding.refreshImageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mWebError = false;
                mBinding.webView.loadUrl(mBinding.webView.getUrl());
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
                if (mWebError) {
                    mBinding.titleTextView.setText(R.string.app_name);
                } else {
                    mBinding.titleTextView.setText(title);
                }
            }
        });
        mBinding.webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (URLUtil.isNetworkUrl(url)) {
                    Intent intent = new Intent(MainActivity.this, BrowserActivity.class);
                    intent.putExtra("url", url);
                    startActivity(intent);
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

        mBinding.webView.loadUrl(getString(R.string.home_url));
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
        public void setupShare(final String title, final String content) {
            mBinding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    mShareTitle = title;
                    mShareContent = content;
                    mBinding.shareImageView.setVisibility(View.VISIBLE);
                }
            });
        }

        @JavascriptInterface
        public void loadUrl(final String url) {
            mBinding.getRoot().post(new Runnable() {
                @Override
                public void run() {
                    mBinding.webView.loadUrl(url);
                }
            });
        }
    }
}
