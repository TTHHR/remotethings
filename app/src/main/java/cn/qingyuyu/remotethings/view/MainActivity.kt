package cn.qingyuyu.remotethings.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import cn.qingyuyu.remotethings.R
import com.qmuiteam.qmui.util.QMUIStatusBarHelper
import kotlinx.android.synthetic.main.activity_main.*
import cn.qingyuyu.remotethings.Constant
import java.io.File
import android.util.Log
import android.webkit.*
import cn.qingyuyu.remotethings.presenter.ThingsPresenter
import cn.qingyuyu.remotethings.view.inter.MainActivityInterface

import com.qmuiteam.qmui.widget.dialog.QMUIDialog

import android.app.AlarmManager

import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.widget.Toast


class MainActivity : AppCompatActivity(),MainActivityInterface {

    lateinit var tp:ThingsPresenter

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        QMUIStatusBarHelper.translucent(this)//沉浸式状态栏
        setContentView(R.layout.activity_main)
        topBar.setBackgroundColor(Color.rgb(33,150,243))
        topBar!!.setTitle(getString(R.string.app_name))
        tp=ThingsPresenter(this)

        init()

        webView.webViewClient=object : WebViewClient() {
            //确保JS加载完毕再执行
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                topBar.setTitle(webView.title)
                    runJs()

            }
        }

    }
    fun init(){
        val webSettings = webView.settings

        // 设置与Js交互的权限
        webSettings.javaScriptEnabled = true
        // 设置允许JS弹窗
        webSettings.javaScriptCanOpenWindowsAutomatically = true

        //js 弹窗处理
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                val tipDialog = QMUIDialog.MessageDialogBuilder(this@MainActivity)
                        .setTitle(view.title)
                        .setMessage(message)
                        .addAction("确定") { dialog, _->
                            dialog.dismiss()
                        }
                        .create()
                tipDialog.show()
                return true
            }
        }

        //js调用android接口
        webView.addJavascriptInterface( JavaScriptinterface(), "android")

        //加载网页
        if(File(Constant.localhtml).exists())
        {
           // webView.loadUrl("http://blog.qingyuyu.cn")
            webView.loadUrl("file://"+Constant.localhtml)
        }

        else
        {
            val tipDialog = QMUIDialog.MessageDialogBuilder(this@MainActivity)
                    .setTitle(webView.title)
                    .setMessage("some thing error")
                    .addAction("确定") { dialog, _->
                        dialog.dismiss()
                        finish()
                    }
                    .create()
            tipDialog.show()
        }
    }
    fun runJs()
    {
                    webView.evaluateJavascript("javascript:mainJS()") {
                        //此处为 js 返回的结果
                        Log.e("运行JS成功", it)
                    }
    }

    override fun onDestroy() {
        tp.exit()//关闭IO
        super.onDestroy()
    }

    override fun restart() {
        runOnUiThread {
            Toast.makeText(this,"发现新版本，即将重启",Toast.LENGTH_LONG).show()
            val mPendingIntentId = 123456
            val mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, Intent(this,SplashActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT)
            val mgr = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 3000, mPendingIntent)//3秒后重启
            Thread(Runnable {
                tp.exit()//关闭所有IO
                Thread.sleep(1000)
                System.exit(0)//然后退出
            }).start()
        }
    }
    inner class JavaScriptinterface {
        /**
         * 与js交互时用到的方法，在js里直接调用的
         */
        @JavascriptInterface
        fun pinMode(ioNumber: Int,mode:String) {
            tp.pinMode(ioNumber,mode)
        }
        @JavascriptInterface
        fun digitalWrite(ioNumber: Int,ioStatus:Boolean) {
            Log.e("thread",Thread.currentThread().name)
            tp.digitalWrite(ioNumber,ioStatus)
        }
        @JavascriptInterface
        fun digitalRead(ioNumber: Int):Int {
           return tp.digitalRead(ioNumber)
        }
        @JavascriptInterface
        fun analogWrite(ioNumber: Int,value:Int) {
            tp.analogWrite(ioNumber,value)
        }
        @JavascriptInterface
        fun openSerial(baudrate:Int)
        {
            tp.openSerial(baudrate)
        }
        @JavascriptInterface
        fun openSHT3x()
        {
            tp.openSHT3X()
        }
        @JavascriptInterface
        fun getHumidity():Float
        {
            return tp.humidity
        }
        @JavascriptInterface
        fun getTemperature():Float
        {
            return tp.temperature
        }
        @JavascriptInterface
        fun getAppVersion():Int
        {
            return tp.getAppVersion(this@MainActivity)
        }
        @JavascriptInterface
        fun getSystemVersion():String
        {
            return tp.systemVersion
        }
        @JavascriptInterface
        fun getWindowSize():String
        {
            Log.e("js","getwindowsize")
            return tp.getWindowSize(this@MainActivity)
        }
        @JavascriptInterface
        fun getGpioList():String
        {
            Log.e("js","gpiolist")
            return tp.gpioList
        }
        @JavascriptInterface
        fun getIICList():String
        {
            return tp.iicList
        }
        @JavascriptInterface
        fun getUartList():String
        {
            return tp.uartList
        }
        @JavascriptInterface
        fun getSpiList():String
        {
            return tp.spiList
        }
        @JavascriptInterface
        fun ioClose(ioNumber: Int) {
            tp.ioClose(ioNumber)
        }
        @JavascriptInterface
        fun setTopBarColor(alpha:Int,red:Int,green:Int,blue:Int) {
            runOnUiThread {
                topBar.setBackgroundColor(Color.argb(alpha,red,green,blue))
            }
        }
        @JavascriptInterface
        fun dismissTopBar() {
            runOnUiThread {
                topBar.visibility= View.GONE
            }
        }
        @JavascriptInterface
        fun toast(text:String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity,text,Toast.LENGTH_SHORT).show()
            }
        }
    }
}
