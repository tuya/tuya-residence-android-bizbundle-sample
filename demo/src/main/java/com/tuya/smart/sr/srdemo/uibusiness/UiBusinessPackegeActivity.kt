package com.tuya.smart.sr.srdemo.uibusiness

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.tuya.smart.activator.config.api.ITuyaDeviceActiveListener
import com.tuya.smart.activator.config.api.TuyaDeviceActivatorManager
import com.tuya.smart.api.MicroContext
import com.tuya.smart.api.router.UrlBuilder
import com.tuya.smart.api.router.UrlRouter
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.panelcaller.api.AbsPanelCallerService
import com.tuya.smart.sdk.bean.DeviceBean
import com.tuya.smart.srsdk.api.site.bean.SiteBean
import com.tuya.smart.srsdk.api.site.interfaces.ITuyaSiteResultCallback
import com.tuya.smart.sr.srdemo.*
import com.tuya.smart.srsdk.sdk.TuyaSmartResidenceSdk
import android.content.DialogInterface
import com.tuya.group_ui_api.GroupState
import com.tuya.group_ui_api.TuyaGroupManager
import com.tuya.smart.sr.srdemo.Global
import com.tuya.smart.sr.srdemo.TextItem
import com.tuya.smart.utils.ProgressUtil

import kotlinx.coroutines.*


/**
 *@desc  biz  ui
 */
class UiBusinessPackegeActivity : AppCompatActivity() {

    var initSuccess = false;

    var TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UIView()
        }
        initdata()
    }

    @Preview
    @Composable
    fun UIView() {
        val state = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(color = Color(242, 242, 242))
                .verticalScroll(state)
        ) {
            scanDevices()
            openDevicePanel()
            openGroupDevicePanel()
            openDeviceDetail()
            groupControl()
            editeGroupControl()
        }
    }


    private fun initdata() {
        Global
            .currentSite?.let {
                TuyaSmartResidenceSdk.siteManager().fetchSiteDetail(
                    it, object : ITuyaSiteResultCallback {
                        override fun onSuccess(bean: SiteBean?) {
                            initSuccess = true
                        }

                        override fun onError(errorCode: String?, errorMsg: String?) {
                            Toast.makeText(
                                this@UiBusinessPackegeActivity,
                                errorMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
    }


    @Composable
    fun scanDevices() {
        val context = LocalContext.current
        TextItem(text = "scan devices", bottomLine = true) {
            if (!cando()) return@TextItem
            Global.currentSite?.let {
                TuyaDeviceActivatorManager.startDeviceActiveAction(
                    context as Activity,
                    it.homeId
                )
                TuyaDeviceActivatorManager.setListener(object :
                    ITuyaDeviceActiveListener {
                    override fun onDevicesAdd(list: List<String>) {
                        if (list.isNotEmpty()) {
                            Toast.makeText(
                                context,
                                "add devices success",
                                Toast.LENGTH_SHORT
                            ).show()
                            initSuccess = false
                            initdata()
                        }
                    }

                    override fun onRoomDataUpdate() {}
                    override fun onOpenDevicePanel(s: String) {
                        Log.d(TAG, "onOpenDevicePanel: ")
                    }

                    fun onExitConfigBiz() {}
                })
            }
        }
    }

    @Composable
    fun openDevicePanel() {
        val context = LocalContext.current
        TextItem(text = "open device panel", bottomLine = true) {
            if (!cando()) return@TextItem
            ProgressUtil.showLoading(context,"loading...")
            getdevices(object : CallBack<List<DeviceBean>> {
                override fun callBack(device: List<DeviceBean>) {
                    ProgressUtil.hideLoading()
                    showChoise(device, object : CallBack<DeviceBean> {
                        override fun callBack(device: DeviceBean) {
                            device?.devId?.let {
                                val service: AbsPanelCallerService =
                                    MicroContext.getServiceManager().findServiceByInterface(
                                        AbsPanelCallerService::class.java.getName()
                                    )
                                // device id
                                service.goPanelWithCheckAndTip(
                                    context as Activity?,
                                    it
                                )
                            }
                        }
                    })
                }
            })

        }
    }

    @Composable
    fun openGroupDevicePanel() {
        val context = LocalContext.current
        TextItem(text = "open group device panel", bottomLine = true) {
            if (!cando()) return@TextItem
            //获取最新的群组信息
            Global
                .currentSite?.let {
                    ProgressUtil.showLoading(context,"loading...")
                    TuyaSmartResidenceSdk.siteManager().fetchSiteDetail(
                        it, object : ITuyaSiteResultCallback {
                            override fun onSuccess(bean: SiteBean?) {
                                ProgressUtil.hideLoading()
                                if (bean != null && !bean.groupList.isNullOrEmpty()) {
                                    var groupName = mutableListOf<String>()
                                    for (item in bean.groupList) {
                                        groupName.add(item.name)
                                    }
                                    showchoise(
                                        groupName,
                                        title = "select group",
                                        object : CallBack<Int> {
                                            override fun callBack(select: Int) {
                                                bean.groupList.get(select)?.id?.let {
                                                    val service = MicroContext.getServiceManager()
                                                        .findServiceByInterface<AbsPanelCallerService>(
                                                            AbsPanelCallerService::class.java.name
                                                        )
                                                    service?.goPanelWithCheckAndTip(
                                                        context as Activity,
                                                        it,
                                                        true
                                                    )
                                                }
                                            }
                                        });
                                } else {
                                    Toast.makeText(
                                        this@UiBusinessPackegeActivity,
                                        "group list is empty,you can create a group",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onError(errorCode: String?, errorMsg: String?) {
                                ProgressUtil.hideLoading()
                                Toast.makeText(
                                    this@UiBusinessPackegeActivity,
                                    errorMsg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }

        }
    }


    @Composable
    fun openDeviceDetail() {
        val context = LocalContext.current
        TextItem(text = "open device detail", bottomLine = true) {
            if (!cando()) return@TextItem
            ProgressUtil.showLoading(context,"loading...")
            getdevices(object : CallBack<List<DeviceBean>> {
                override fun callBack(devices: List<DeviceBean>) {
                    ProgressUtil.hideLoading()
                    showChoise(devices, object : CallBack<DeviceBean> {
                        override fun callBack(device: DeviceBean) {
                            device?.devId?.let {
                                val urlBuilder = UrlBuilder(context, "panelMore")
                                val deviceBean: DeviceBean? =
                                    TuyaHomeSdk.getDataInstance().getDeviceBean(it)
                                val bundle = Bundle()
                                bundle.putString("extra_panel_dev_id", deviceBean?.getDevId())
                                bundle.putString("extra_panel_name", deviceBean?.getName())
//                              bundle.putLong("extra_panel_group_id", groupId)
                                urlBuilder.putExtras(bundle)
                                UrlRouter.execute(urlBuilder)
                            }
                        }
                    })
                }
            })
        }
    }

    @Composable
    fun groupControl() {
        val context = LocalContext.current
        TextItem(text = "create group ", bottomLine = true) {
            if (!cando()) return@TextItem
            ProgressUtil.showLoading(context,"loading...")
            getdevices(object : CallBack<List<DeviceBean>> {
                override fun callBack(devices: List<DeviceBean>) {
                    ProgressUtil.hideLoading()
                    showChoise(devices, object : CallBack<DeviceBean> {
                        override fun callBack(device: DeviceBean) {
                            device?.devId?.let {
                                val createGroup = TuyaGroupManager.getInstance()
                                    .createGroup(context as Activity, it)
                                when (createGroup) {
                                    GroupState.NONE -> Toast.makeText(
                                        context,
                                        "device is not exist",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    GroupState.NOT_SUPPORT -> Toast.makeText(
                                        context,
                                        "device is not support",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                        }
                    })
                }
            })
        }
    }

    @Composable
    fun editeGroupControl() {
        val context = LocalContext.current
        TextItem(text = "edite group ", bottomLine = true) {
            if (!cando()) return@TextItem
            //获取最新的群组信息
            Global
                .currentSite?.let {
                    ProgressUtil.showLoading(context,"loading...")
                    TuyaSmartResidenceSdk.siteManager().fetchSiteDetail(
                        it, object : ITuyaSiteResultCallback {
                            override fun onSuccess(bean: SiteBean?) {
                                ProgressUtil.hideLoading()
                                if (bean != null && !bean.groupList.isNullOrEmpty()) {
                                    var groupName = mutableListOf<String>()
                                    for (item in bean.groupList) {
                                        groupName.add(item.name)
                                    }
                                    showchoise(
                                        groupName,
                                        title = "select group",
                                        object : CallBack<Int> {
                                            override fun callBack(select: Int) {
                                                bean.groupList.get(select)?.id?.let {
                                                    val editGroup = TuyaGroupManager.getInstance()
                                                        .editGroup(context as Activity, it)
                                                    when (editGroup) {
                                                        GroupState.NONE -> Toast.makeText(
                                                            context,
                                                            "device is not exist",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                            .show()
                                                        GroupState.NOT_SUPPORT -> Toast.makeText(
                                                            context,
                                                            "device is not support",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                            .show()
                                                    }
                                                }
                                            }
                                        });
                                } else {
                                    Toast.makeText(
                                        this@UiBusinessPackegeActivity,
                                        "group list is empty,you can create a group",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onError(errorCode: String?, errorMsg: String?) {
                                ProgressUtil.hideLoading()
                                Toast.makeText(
                                    this@UiBusinessPackegeActivity,
                                    errorMsg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
        }
    }


    fun cando(): Boolean {
        if (Global.currentSite == null) {
            Toast.makeText(
                this,
                "current site is empty,you can create site",
                Toast.LENGTH_SHORT
            )
                .show()
            return false
        }

        if (!initSuccess) {
            Toast.makeText(
                this,
                "watting init..",
                Toast.LENGTH_SHORT
            )
            return false
        }

        return true

    }


    private fun showChoise(devices: List<DeviceBean>, call: CallBack<DeviceBean>) {
        if (devices.isNotEmpty()) {
            val devicename = mutableListOf<String>()
            for (device in devices) {
                device?.name?.let { devicename.add(it) }
            }
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(this, R.style.Theme_Material_Light_Dialog)
            builder.setTitle("select device")
            builder.setItems(devicename.toTypedArray(),
                DialogInterface.OnClickListener { dialog, which ->
                    call.callBack(devices.get(which))
                })
            builder.show()
        } else {
            Toast.makeText(
                this,
                "devices is empty,you can scan devices on first menu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun showchoise(strings: List<String>, title: String, call: CallBack<Int>) {
        if (strings.isNotEmpty()) {
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(this, R.style.Theme_Material_Light_Dialog)
            builder.setTitle(title)
            builder.setItems(strings.toTypedArray(),
                DialogInterface.OnClickListener { dialog, which ->
                    call.callBack(which)
                })
            builder.show()
        } else {
            Toast.makeText(
                this,
                "strings is empty",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun getdevices(call: CallBack<List<DeviceBean>>) {
        val tempDeviceList = mutableListOf<DeviceBean>()
        //获取全部设备列表
        Global
            .currentSite?.let {
                TuyaSmartResidenceSdk.siteManager().fetchSiteDetail(
                    it, object : ITuyaSiteResultCallback {
                        override fun onSuccess(bean: SiteBean?) {
                            bean?.deviceList?.let {
                                tempDeviceList.addAll(it)
                            }
                            call.callBack(tempDeviceList)
                        }
                        override fun onError(errorCode: String?, errorMsg: String?) {
                            Toast.makeText(
                                this@UiBusinessPackegeActivity,
                                errorMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
    }


    interface CallBack<T> {
        fun callBack(select: T)
    }


}


