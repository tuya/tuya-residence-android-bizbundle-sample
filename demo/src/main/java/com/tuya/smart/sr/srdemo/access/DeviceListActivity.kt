package com.tuya.smart.sr.srdemo.access

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuya.sdk.core.PluginManager
import com.tuya.smart.android.common.utils.L
import com.tuya.smart.android.network.Business
import com.tuya.smart.android.network.http.BusinessResponse
import com.tuya.smart.api.MicroContext
import com.tuya.smart.interior.api.ITuyaDevicePlugin
import com.tuya.smart.panelcaller.api.AbsPanelCallerService
import com.tuya.smart.srsdk.api.access.bean.DeviceData
import com.tuya.smart.sr.srdemo.*
import com.tuya.smart.srsdk.sdk.TuyaSmartResidenceSdk

/**
 *
 * @description:
 * @author: mengzi.deng
 * @since: 2021/11/10 15:42
 */
class DeviceListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeviceListView()
        }
    }
}

@Preview
@Composable
fun DeviceListView(viewModel: DeviceListModel = viewModel()) {
    LazyColumn {
        itemsIndexed(viewModel.deviceList) { _, deviceData ->
            DeviceRow(deviceData)
        }
    }
}


class DeviceListModel : ViewModel() {
    val deviceList = mutableStateListOf<DeviceData>()
    init {
        fetchDeviceList()
    }
    private fun fetchDeviceList() {
        Global.currentSite?.homeId?.let {
            TuyaSmartResidenceSdk.access()
                .fetchDeviceListWithSiteId(
                    it.toString(),
                    object : Business.ResultListener<ArrayList<String>> {
                        override fun onFailure(
                            p0: BusinessResponse?,
                            p1: ArrayList<String>?,
                            p2: String?
                        ) {
                            L.d(TAG, "Fetch device list failure")
                        }

                        override fun onSuccess(
                            p0: BusinessResponse?,
                            p1: ArrayList<String>?,
                            p2: String?
                        ) {
                            p1?.run {
                                val tempDeviceList = mutableListOf<DeviceData>()
                                val iTuyaDevicePlugin =
                                    PluginManager.service(ITuyaDevicePlugin::class.java)
                                for (deviceId in this) {
                                    //warning : you neet to fetchSiteDetail first before get the device in cache
                                    val dev = iTuyaDevicePlugin.devListCacheManager.getDev(deviceId)
                                        ?: continue
                                    tempDeviceList.add(DeviceData(dev))
                                }
                                deviceList.clear()
                                deviceList.addAll(tempDeviceList)
                            }
                        }
                    })
        }
    }
}


@Composable
fun DeviceRow(deviceData: DeviceData) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .height(50.dp)
            .padding(8.dp)
            .clickable(onClick = {

                val service: AbsPanelCallerService =
                    MicroContext
                        .getServiceManager()
                        .findServiceByInterface(
                            AbsPanelCallerService::class.java.getName()
                        )
                // device id
                service.goPanelWithCheckAndTip(
                    context as Activity,
                    deviceData.deviceBean?.devId
                )

            }),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            deviceData.deviceBean?.name ?: "",
            fontSize = 16.sp,
            modifier = Modifier.weight(1.0F, true)
        )
        ArrowImage()
    }
    RowSpaceLine()
}
