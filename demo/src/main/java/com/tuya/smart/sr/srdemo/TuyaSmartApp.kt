package com.tuya.smart.sr.srdemo

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.tuya.smart.android.common.utils.L
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.optimus.sdk.TuyaOptimusSdk
import com.tuya.smart.sdk.TuyaSdk
import com.tuya.smart.api.service.RedirectService

import com.tuya.smart.api.MicroContext

import com.tuya.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService
import com.tuya.smart.sr.srdemo.account.SigninActivity

import com.tuya.smart.wrapper.api.TuyaWrapper

import com.tuya.smart.sr.srdemo.impl.BizBundleFamilyServiceImpl


class TuyaSmartApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        L.d(TAG, "TuyaSmartApp:onCreate " + getProcessName(this))
        L.setSendLogOn(true)

        // Product Environment
        // TuyaHomeSdk.init(this)

        // Pre Environment
        TuyaSdk.init(this)


        TuyaHomeSdk.setDebugMode(true)
        TuyaSdk.setOnNeedLoginListener { context ->
            //token fail listener
            val intent = Intent(context, SigninActivity::class.java)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }


        // 请不要修改初始化顺序
        Fresco.initialize(this);

        // SDK 初始化
        TuyaHomeSdk.init(this);

        // 业务包初始化
        TuyaWrapper.init(this, { errorCode, urlBuilder -> // 路由未实现回调
            // 点击无反应表示路由未现实，需要在此实现， urlBuilder.target 目标路由， urlBuilder.params 路由参数
            Log.e("router not implement", urlBuilder.target + urlBuilder.params.toString())
        }) { serviceName -> // 服务未实现回调
            Log.e("service not implement", serviceName)
        }
        TuyaOptimusSdk.init(this)

        // 注册家庭服务，商城业务包可以不注册此服务
        TuyaWrapper.registerService(
            AbsBizBundleFamilyService::class.java,
            com.tuya.smart.sr.srdemo.impl.BizBundleFamilyServiceImpl()
        )
        //拦截已存在的路由，通过参数跳转至自定义实现页面
        val service = MicroContext.getServiceManager().findServiceByInterface<RedirectService>(
            RedirectService::class.java.name
        )
        service.registerUrlInterceptor { urlBuilder, interceptorCallback -> //Such as:
            //例如：拦截点击面板右上角按钮事件，通过 urlBuilder 的参数跳转至自定义页面
            if (urlBuilder.target == "panelAction" && urlBuilder.params.getString("action") == "gotoPanelMore") {
//                interceptorCallback.interceptor("interceptor")
                Log.e("interceptor", urlBuilder.params.toString())
                Toast.makeText(this, "jump to device detail", Toast.LENGTH_SHORT).show()
                interceptorCallback.onContinue(urlBuilder)
            } else {
                interceptorCallback.onContinue(urlBuilder)
            }
        }


    }

    companion object {
        fun getProcessName(context: Context): String {
            val pid = Process.myPid()
            val mActivityManager = context
                .getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (appProcess in mActivityManager
                .runningAppProcesses) {
                if (appProcess.pid == pid) {
                    return appProcess.processName
                }
            }
            return ""
        }

        var appContext: Context? = null
            private set
    }
}