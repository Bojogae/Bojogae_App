package com.bojogae.bojogae_app.basic_code.application

import android.app.Application
import com.bojogae.bojogae_app.basic_code.utils.CrashHandler

/**application class
 *
 * Created by jianddongguo on 2017/7/20.
 */
class MyApplication : Application() {
    private var mCrashHandler: CrashHandler? = null
    override fun onCreate() {
        super.onCreate()
        mCrashHandler = CrashHandler.instance
        mCrashHandler?.init(applicationContext, javaClass)
    }

    companion object {
        // File Directory in sd card
        const val DIRECTORY_NAME = "USBCamera"
    }
}