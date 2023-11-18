package com.bojogae.bojogae_app.basic_code.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.os.Process
import android.widget.Toast
import com.bojogae.bojogae_app.basic_code.application.MyApplication
import com.jiangdg.usbcamera.utils.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * UncaughtException handler class
 *
 * @author jiangdg on 2017/6/27.
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mContext: Context? = null
    var mainActivityClass: Class<*>? = null
    private val infos: MutableMap<String, String> = HashMap()
    fun init(context: Context?, activityClass: Class<*>?) {
        mContext = context
        mainActivityClass = activityClass
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler!!.uncaughtException(thread, ex)
        } else {
            println("uncaughtException--->" + ex.message)
            //			Log.e(TAG, ex.getMessage());
            logError(ex)
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
//				Log.e("debug", "error：", e);
            }
            exitApp()
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }
        Thread {
            Looper.prepare()
            Toast.makeText(
                mContext!!.applicationContext,
                "unknown exception and exiting...Please checking logs in sd card！",
                Toast.LENGTH_LONG
            ).show()
            Looper.loop()
        }.start()
        collectDeviceInfo(mContext!!.applicationContext)
        logError(ex)
        return true
    }

    private fun exitApp() {
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    fun collectDeviceInfo(ctx: Context) {
        try {
            val pm = ctx.packageManager
            val pi = pm.getPackageInfo(
                ctx.packageName,
                PackageManager.GET_ACTIVITIES
            )
            if (pi != null) {
                val versionName = if (pi.versionName == null) "null" else pi.versionName
                val versionCode = pi.versionCode.toString() + ""
                infos["versionName"] = versionName
                infos["versionCode"] = versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }
        val fields = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
                infos[field.name] = field[null].toString()
            } catch (e: Exception) {
            }
        }
    }

    private fun logError(ex: Throwable) {
        val sb = StringBuffer()
        for ((key, value) in infos) {
            sb.append("$key=$value\n")
        }
        val num = ex.stackTrace.size
        for (i in 0 until num) {
            sb.append(ex.stackTrace[i].toString())
            sb.append("\n")
        }
        val file = File(FileUtils.ROOT_PATH + "/" + MyApplication.DIRECTORY_NAME + "/log.txt")
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            fos.write((sb.toString() + "exception：" + ex.localizedMessage).toByteArray())
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val TAG = "CrashHandler"
        const val PROGRAM_BROKEN_ACTION = "com.teligen.wccp.PROGRAM_BROKEN"
        val instance = CrashHandler()
    }
}