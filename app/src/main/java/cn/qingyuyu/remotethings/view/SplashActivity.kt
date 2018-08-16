package cn.qingyuyu.remotethings.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import cn.qingyuyu.remotethings.Constant
import cn.qingyuyu.remotethings.R
import kotlinx.android.synthetic.main.activity_splash.*
import com.qmuiteam.qmui.widget.QMUIProgressBar
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection

import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.things.device.TimeManager




class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar.maxValue=100

        progressBar.qmuiProgressBarTextGenerator = QMUIProgressBar.QMUIProgressBarTextGenerator { _, value, maxValue -> value.toString() + "/" + maxValue }

        Thread(object : Runnable {
            private var percent: Int = 0

            override fun run() {
                try {
                    var date= Date(0)
                    try {
                        val url = URL("http://www.360.cn")
                        val uc = url.openConnection()//生成连接对
                        uc.connect() //发出连接
                        val ld = uc.date //取得网站日期时间

                        date= Date(ld)// 转换为标准时间对象

                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)// 输出北京时间

                        Log.e("当前时间",""+sdf.format(date))
                    } catch (e: Exception) {
                        Log.e("get net time",""+e)
                    }
                    runOnUiThread {
                        val timeManager = TimeManager.getInstance()
                        // Use 24-hour time
                        timeManager.setTimeFormat(TimeManager.FORMAT_24)

                        // Set time zone to Eastern Standard Time
                        timeManager.setTimeZone("Asia/Shanghai")

                        // Set clock time to noon

                        timeManager.setTime(date.time)
                    }



                    // 打开 URL 必须在子线程
                    var url = URL( Constant.jsurl)
                    var conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout=3000
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val contentLength = conn.contentLength
                        val file= File(Constant.localJs)

                        if(!file.exists())
                            file.createNewFile()
                        val os=FileOutputStream(file,false)
                        val ins = conn.inputStream
                        val buffer = ByteArray(10240)
                        var len: Int
                        var sum = 0f
                        len=ins.read(buffer)
                        while (len != -1) {
                            os.write(buffer,0,len)
                            sum += len
                            // 注意强转方式，防止一直为0
                            percent = ( sum / contentLength).toInt()*100
                            Log.e("sum","$sum")
                            Log.e("length","$contentLength")
                            // 在主线程上运行的子线程
                            runOnUiThread {
                                progressBar.progress = percent
                            }
                            len=ins.read(buffer)
                        }
                        os.close()
                        ins.close()
                        conn.disconnect()
                    }
                    // 打开 URL 必须在子线程
                    url = URL( Constant.htmlurl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout=3000
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {

                        val file = File(Constant.localhtml)

                        if (!file.exists())
                            file.createNewFile()
                        val os = FileOutputStream(file, false)
                        val ins = conn.inputStream
                        val buffer = ByteArray(1024)
                        var len: Int
                        len = ins.read(buffer)
                        while (len != -1) {
                            os.write(buffer, 0, len)
                            len = ins.read(buffer)
                        }
                        os.close()
                        ins.close()
                        conn.disconnect()
                    }





                } catch (e: Exception) {
                   Log.e("download",e.toString())
                    runOnUiThread {
                        textview.text = "some thing error"
                    }
                }
                finally {

                    Thread.sleep(500)
                    startActivity(Intent(this@SplashActivity,MainActivity::class.java))
                    finish()
                }

            }
        }).start()

    }


}
