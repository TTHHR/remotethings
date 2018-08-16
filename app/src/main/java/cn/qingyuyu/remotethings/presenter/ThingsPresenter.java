package cn.qingyuyu.remotethings.presenter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.things.AndroidThings;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.android.things.pio.UartDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.qingyuyu.remotethings.Constant;
import cn.qingyuyu.remotethings.view.inter.MainActivityInterface;
import cn.qingyuyu.remotethings.things.SHT3x;

public class ThingsPresenter {
    private Map<Integer,Gpio> gpioMap =new HashMap<>();
    private Map<Integer,Pwm> pwmMap =new HashMap<>();
    private UartDevice mUartDevice=null;
    private boolean running=true;
    private MainActivityInterface mai;
    private SHT3x sht3x=null;
    public ThingsPresenter(final MainActivityInterface mai){

        this.mai=mai;
        //开启服务线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running)
                {
                    try {
                        Thread.sleep(20000);

                        URL url = new URL( Constant.serverVersion);
                        HttpURLConnection conn = (HttpURLConnection)url.openConnection() ;
                        conn.setConnectTimeout(3000);

                        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                            File file= new File(Constant.localVersion);

                            if(!file.exists())
                                file.createNewFile();

                            BufferedReader br=new BufferedReader(new FileReader(file));

                            InputStream ins = conn.getInputStream();
                            BufferedReader conreader=new BufferedReader(new InputStreamReader(ins));
                            String v=br.readLine();
                            br.close();


                            if(v==null)
                                v="";
                            String newv=conreader.readLine();

                            FileOutputStream os=new FileOutputStream(file);
                            os.write(newv.getBytes());//新版本写入
                            os.write("\n".getBytes());//新版本写入
                            os.flush();
                            os.close();
                            ins.close();
                            conn.disconnect();
                            Log.e("update","newv "+newv+" old "+v);
                            if(newv.compareTo(v)>0) {
                                mai.restart();
                            }
                        }


                    } catch (Exception e) {
                        Log.e("update",e.toString());
                    }
                }
            }
        }).start();
    }

    public boolean pinMode(int ioNumber,String mode)
    {
        Log.i("pinMode",mode+ioNumber);
        boolean b=true;
        PeripheralManager pm=PeripheralManager.getInstance();
        String ioName="BCM"+ioNumber;
        try {
            switch (ioNumber) {//先打开IO口
                case 4:
                case 5:
                case 6:
                    gpioMap.put(ioNumber,pm.openGpio(ioName));
                    break;

                case 12:gpioMap.put(ioNumber,pm.openGpio(ioName));
                    break;
                case 13:pwmMap.put(ioNumber,pm.openPwm(ioName));
                    break;

                case 16:
                case 17:gpioMap.put(ioNumber,pm.openGpio(ioName));
                    break;
                case 18:pwmMap.put(ioNumber,pm.openPwm(ioName));
                    break;

                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:gpioMap.put(ioNumber,pm.openGpio(ioName));
                    break;
                    default: b=false;
            }
            if(!b)//如果b为false说明ioNumber错误
            {
                return b;//直接返回
            }
            //然后进行模式设置
            if(ioNumber==13||ioNumber==18)//pwm口暂时只能作为输出
            {
                if(pwmMap.containsKey(ioNumber))//确保io已经被打开
                {
                    Pwm p=pwmMap.get(ioNumber);
                    p.setPwmFrequencyHz(255);
                    p.setEnabled(false);//先关闭信号
                }
            }
            else if(mode.startsWith("o")||mode.startsWith("O")){//输出模式
                if(gpioMap.containsKey(ioNumber)) {//确保io已经被打开
                    Gpio gpio = gpioMap.get(ioNumber);
                    //初始电压为真
                    gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    // 高电压为真
                    gpio.setActiveType(Gpio.ACTIVE_HIGH);
                }
            }
            else if(mode.startsWith("i")||mode.startsWith("I")){//输入模式
                if(gpioMap.containsKey(ioNumber)) {//确保io已经被打开
                    Gpio gpio = gpioMap.get(ioNumber);
                    gpio.setDirection(Gpio.DIRECTION_IN);
                    // 高电压为真
                    gpio.setActiveType(Gpio.ACTIVE_HIGH);
                }
            }

        }catch (Exception e)
        {
            b=false;
            Log.e("pinmode",e.toString());
        }

        return b;
    }



    public boolean  digitalWrite(int ioNumber,boolean ioStatus) {
        Log.i("digitalWrite",""+ioNumber+ioStatus);
        boolean b=true;
        switch (ioNumber) {//先判断IO口
            case 4:
            case 5:
            case 6:
            case 12:
            case 16:
            case 17:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:b=true;
                break;
            default: b=false;//这里的都不是gpio口
        }
        if(!b)//如果b为false说明ioNumber错误
        {
            return b;//直接返回
        }
        //注意，这里无法判断io是输入模式还是输出模式
        if(gpioMap.containsKey(ioNumber))
        {
            try {
                Gpio g = gpioMap.get(ioNumber);
                g.setValue(ioStatus);
            }
            catch (Exception e)
            {
                Log.e("digitalWrite",e.toString());
                b=false;
            }
        }

    return b;

    }

    public int digitalRead(int ioNumber) {

        int b=1;
        switch (ioNumber) {//先判断IO口
            case 4:
            case 5:
            case 6:
            case 12:
            case 16:
            case 17:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:b=1;
                break;
            default: b=2;//这里的都不是gpio口
        }
        if(b==2)//如果b为2说明ioNumber错误
        {
            return b;//直接返回
        }
        //注意，这里无法判断io是输入模式还是输出模式
        if(gpioMap.containsKey(ioNumber))
        {
            try {
                Gpio g = gpioMap.get(ioNumber);
                b=g.getValue()?1:0;
            }
            catch (Exception e)
            {
                Log.e("digitalRead",e.toString());
                b=2;
            }
        }
        Log.i("digitalRead",""+ioNumber+" "+b);
        return b;
    }
    public boolean analogWrite(int ioNumber,int value)
    {
        Log.i("analogWrite",""+ioNumber+" value="+value);
        boolean b=true;
        if(ioNumber!=13&&ioNumber!=18)
            return false;
        if(value>255||value<0)
            return false;
        try {
            if (pwmMap.containsKey(ioNumber)) {
                Pwm p = pwmMap.get(ioNumber);
                p.setEnabled(false);
                p.setPwmDutyCycle(value);
                p.setEnabled(true);
            } else
                b= false;
        }catch (Exception e)
        {
            b=false;
            Log.e("analogWrite",e.toString());
        }
        return b;
    }

    public boolean openSerial(int baudrate)
    {
        Log.i("openSerial",""+baudrate);
        boolean b=true;

        try {
            mUartDevice=PeripheralManager.getInstance().openUartDevice("UART0");
            mUartDevice.setBaudrate(baudrate);
            mUartDevice.setDataSize(8);
            mUartDevice.setParity(UartDevice.PARITY_NONE);
            mUartDevice.setStopBits(1);
        } catch (IOException e) {
            b=false;
            Log.e("opSerial",e.toString());
        }
        return b;
    }

    public void closeSerial()
    {
        try {
            if(mUartDevice!=null)
                mUartDevice.close();
        }
        catch (Exception e)
        {
            Log.e("close Serial",e.toString());
        }
    }

    public void openSHT3X()
    {
        try {
            sht3x = new SHT3x("I2C1", (byte)0x44);
            sht3x.readFromDevice();
        }catch (Exception e)
        {
            Log.e("opensht3x",e.toString());
        }

    }

public float getHumidity()
{
    if(sht3x!=null) {
        try {
            sht3x.readFromDevice();
            return sht3x.getHumidity();
        } catch (Exception e) {
            Log.e("read sht3x",e.toString());
        }
    }
    return 0f;
}
    public float getTemperature()
    {
        if(sht3x!=null) {
            try {
                sht3x.readFromDevice();
                return sht3x.getTemperature();
            } catch (Exception e) {
                Log.e("read sht3x",e.toString());
            }
        }
        return 0f;
    }

    public  int getAppVersion(Context mContext) {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = mContext.getPackageManager().
                    getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
    public String getSystemVersion(){
        return AndroidThings.RELEASE;
    }
    public String getWindowSize(Context c) {
        String s="";
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        s+= dm.widthPixels;
        s+="*";
        s+= dm.heightPixels;
        return s;
    }
    public String getUartList() {
        String s="";
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> portList = manager.getUartDeviceList();
        s=portList.toString();
        return s.substring(1,s.length()-1);
    }
    public String getIICList() {
        String s="";
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> portList = manager.getI2cBusList();
        s=portList.toString();
        return s.substring(1,s.length()-1);
    }
    public String getSpiList() {
        String s="";
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> portList = manager.getSpiBusList();
        s=portList.toString();
        return s.substring(1,s.length()-1);
    }
    public String getGpioList() {
        String s="";
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> portList = manager.getGpioList();
        s=portList.toString();

        return s.substring(1,s.length()-1);
    }
    public void ioClose(int ioNumber)
    {
        if(gpioMap.containsKey(ioNumber))
        try {
            gpioMap.get(ioNumber).close();
        } catch (IOException e) {
            Log.e("ioClose",e.toString());
        }
        if(pwmMap.containsKey(ioNumber))
            try {
                pwmMap.get(ioNumber).close();
            } catch (IOException e) {
                Log.e("ioClose",e.toString());
            }
    }
    public void closeAllIo()
    {
        Set<Integer> key=gpioMap.keySet();
        for(int i:key)
        {
            ioClose(i);
        }
    }
    public void exit()
    {
        running=false;
        closeAllIo();
    }
}
