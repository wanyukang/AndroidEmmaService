package com.wanyukang.emma;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * Emma代码覆盖率记录Service 
 * @author wanyukang
 *
 */
public class EmmaService extends Service {

	private static final String TAG = "EmmaService";
//	private static final String DEFAULT_COVERAGE_FILE_PATH = "/mnt/sdcard/coverage.ec";
	private final Bundle mResults = new Bundle();
//	private String mCoverageFilePath;
	private String DEFAULT_COVERAGE_FILE_PATH_MEMORY_DIR;		//日志文件在内存中的路径(日志文件在安装目录中的路径)  
	private String DEFAULT_COVERAGE_FILE_PATH_SDCARD_DIR;		//日志文件在sdcard中的路径
	private final int SDCARD_TYPE = 0;		//当前的日志记录类型为存储在SD卡下面  
    private final int MEMORY_TYPE = 1;		//当前的日志记录类型为存储在内存中 
    private int CURR_TYPE = SDCARD_TYPE;    //当前的日志记录位置
	private SDStateMonitorReceiver sdStateReceiver;		//SDcard状态监测 
    private static boolean IS_Coverage = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		
		Log.i(TAG, "EmmaService==>>onCreate");
		super.onCreate();
		init();
		register();
	}
	
	private void init() {
		
		DEFAULT_COVERAGE_FILE_PATH_MEMORY_DIR = getFilesDir().getAbsolutePath() + File.separator + "Emma";  
		DEFAULT_COVERAGE_FILE_PATH_SDCARD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator  +   "MyApp" + File.separator + "Emma";  
        createLogDir();  
        CURR_TYPE = getCurrLogType();
	}

	private void register(){  
        IntentFilter sdCarMonitorFilter = new IntentFilter();  
        sdCarMonitorFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);  
        sdCarMonitorFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);  
        sdCarMonitorFilter.addDataScheme("file");  
        sdStateReceiver = new SDStateMonitorReceiver();  
        registerReceiver(sdStateReceiver, sdCarMonitorFilter);  
          
          
    }  
	
	/**
	 * 获取当前应存储在内存中还是存储在SDCard中
	 * @return
	 */
	private int getCurrLogType() {
		
		if (!Environment.getExternalStorageState().equals(  
                Environment.MEDIA_MOUNTED)) {  
            return MEMORY_TYPE;  
        }else{  
            return SDCARD_TYPE;  
        }  
	}

	/**
	 * 创建目录
	 */
	private void createLogDir() {
		
		File file = new File(DEFAULT_COVERAGE_FILE_PATH_MEMORY_DIR);  
        boolean mkOk;  
        if (!file.isDirectory()) {  
            mkOk = file.mkdirs();  
            if (!mkOk) {  
                mkOk = file.mkdirs();  
            }  
        }  
          
        if (Environment.getExternalStorageState().equals(  
                Environment.MEDIA_MOUNTED)) {  
            file = new File(DEFAULT_COVERAGE_FILE_PATH_SDCARD_DIR);  
            if (!file.isDirectory()) {  
                mkOk = file.mkdirs();  
                if (!mkOk) {  
                    Log.i(TAG, "move file failed, dir is not created succ");  
                    return;  
                }  
            }  
        }
	}
	
	/**
	 * 监控SDCard状态  
	 * @author wanyukang
	 *
	 */
    class SDStateMonitorReceiver extends BroadcastReceiver {  
        public void onReceive(Context context, Intent intent) {  
              
            if(Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())){   //存储卡被卸载  
                if(CURR_TYPE == SDCARD_TYPE){  
                    Log.i(TAG, "SDcard 已卸载！");  
                    CURR_TYPE = MEMORY_TYPE;  
                }  
            }else{                                                          //存储卡被挂载  
                if(CURR_TYPE == MEMORY_TYPE){  
                    Log.i(TAG, "SDcard 已挂载！");  
                    CURR_TYPE = SDCARD_TYPE;  
                }  
            }  
        }  
    }  

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.i(TAG, "EmmaService==>>onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		
		generateCoverageReport();
		Log.i(TAG, "EmmaServie==>>onDestroy");
		super.onDestroy();
		unregisterReceiver(sdStateReceiver); 
	}
	
	private void generateCoverageReport() {
		
		Log.i(TAG, "generateCoverageReport()");
		
		java.io.File coverageFile = new java.io.File(getCoverageFilePath());

		try {
			Class<?> emmaRTClass = Class.forName("com.vladium.emma.rt.RT");
			Method dumpCoverageMethod = emmaRTClass.getMethod(
					"dumpCoverageData", coverageFile.getClass(), boolean.class,
					boolean.class);
			dumpCoverageMethod.invoke(null, coverageFile, true, false);
		} catch (ClassNotFoundException e) {
			reportEmmaError("Emma.jar not in the class path?", e);
		} catch (SecurityException e) {
			reportEmmaError(e);
		} catch (NoSuchMethodException e) {
			reportEmmaError(e);
		} catch (IllegalArgumentException e) {
			reportEmmaError(e);
		} catch (IllegalAccessException e) {
			reportEmmaError(e);
		} catch (InvocationTargetException e) {
			reportEmmaError(e);
		}
	}
	
	/**
	 * 获取当前存储位置的绝对路径
	 * @return
	 */
	private String getCoverageFilePath() {
//		if (mCoverageFilePath == null) {
//			return DEFAULT_COVERAGE_FILE_PATH;
//		} else {
//			return mCoverageFilePath;
//		}
		
		if (CURR_TYPE == MEMORY_TYPE) {
			Log.i(TAG, "CoverageFile in memory, the path is:" + DEFAULT_COVERAGE_FILE_PATH_MEMORY_DIR);
			return DEFAULT_COVERAGE_FILE_PATH_MEMORY_DIR + File.separator + "coverage.ec";
		} else {
			Log.i(TAG, "CoverageFile in SDCard, the path is:" + DEFAULT_COVERAGE_FILE_PATH_SDCARD_DIR);
			return DEFAULT_COVERAGE_FILE_PATH_SDCARD_DIR + File.separator + "coverage.ec";
		}
		
	}
	
	private void reportEmmaError(Exception e) {
		reportEmmaError("", e);
	}
	
	private void reportEmmaError(String hint, Exception e) {
		String msg = "Failed to generate emma coverage. " + hint;
		Log.e(TAG, msg, e);
		mResults.putString(Instrumentation.REPORT_KEY_STREAMRESULT, "\nError: " + msg);
	}
	
	public static void startEmmaService(Context context) {
		if (IS_Coverage == true) {
			Intent intent = new Intent(context, EmmaService.class);
			context.startService(intent);
		}
	}
	
	public static void stopEmmaService(Context context) {
		if (IS_Coverage == true) {
			Intent intent = new Intent(context, EmmaService.class);
			context.stopService(intent);
		}
	}
}
