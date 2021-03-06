package com.tuya.smart.sr.srdemo.site

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuya.smart.android.common.utils.L
import com.tuya.smart.srsdk.api.site.bean.SiteBean
import com.tuya.smart.srsdk.api.site.interfaces.ITuyaSiteResultCallback
import com.alibaba.fastjson.JSON
import com.tuya.sdk.home.bean.MemberResponseBean
import com.tuya.smart.android.network.Business
import com.tuya.smart.android.network.http.BusinessResponse
import com.tuya.smart.home.sdk.bean.RoomBean
import com.tuya.smart.srsdk.api.site.bean.InvitationMemberBean
import com.tuya.smart.sr.srdemo.*
import java.util.ArrayList
import com.tuya.smart.srsdk.sdk.TuyaSmartResidenceSdk

class SiteDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SiteDetail()
        }
    }
}

@Preview
@Composable
fun SiteDetail(viewModel: SiteDetailModel = viewModel()) {
    val context = LocalContext.current
    val siteName by rememberSaveable { mutableStateOf(Global.currentSite?.name?:"null") }
    val siteID by rememberSaveable { mutableStateOf(Global.currentSite?.homeId.toString()) }
    Column(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(color = Color(242, 242, 242))
    ) {
        RowSpacer()
        Row(modifier = rowModifier
            .clickable(onClick = {

            }),
        verticalAlignment = Alignment.CenterVertically,) {
        Text(text = "Current Site Name: ", fontSize = 18.sp)
        Text(text = siteName, fontSize = 18.sp)
    }
        RowSpaceLine()

        Row(modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,) {
            Text(text = "Current Site ID: ", fontSize = 18.sp)
            Text(text = siteID, fontSize = 18.sp)
        }
        RowSpacer()

        Row(modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,) {
            Text(text = "SiteDetail", fontSize = 18.sp)
            Spacer(modifier = Modifier
                .fillMaxHeight()
                .width(10.dp))
            Button(onClick = { if (Global
                    .currentSite != null) {
                viewModel.load()
            } else {
                Toast.makeText(context, "Current SiteBean is null", Toast.LENGTH_LONG).show()
            }
            }) {
                Text(text = "GET", fontSize = 12.sp)
            }
        }
        RowSpacer()
        Text("room", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        LazyColumn(content = {
            itemsIndexed(viewModel.roomList) {_, item ->
                SiteDetailRoomItemView(item)
            }
            item {
                RowSpacer()
                Text("member list", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
            }
            itemsIndexed(viewModel.memberList) {_, item ->
                SiteDetailMemberItemView(item)
            }
            item {
                RowSpacer()
                Text("current member", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
            }
            itemsIndexed(viewModel.currentMember) {_, item ->
                SiteDetailCurrentMemberView(item)
            }
        })
    }
}

class SiteDetailModel : ViewModel() {

    val roomList = mutableStateListOf<RoomBean>()
    val memberList = mutableStateListOf<InvitationMemberBean?>()
    val currentMember = mutableStateListOf<MemberResponseBean?>()

    init {
        load()
    }

    fun load() {
        fetchSiteDetailList()
        fetchMemberList()
        fetchCurrentMember()
    }

    private fun fetchMemberList() {
        TuyaSmartResidenceSdk.siteManager().fetchSiteResidentList(Global.currentSite
            ?.homeId?:return, object : Business.ResultListener<ArrayList<InvitationMemberBean?>?> {
            override fun onFailure(
                p0: BusinessResponse?,
                p1: ArrayList<InvitationMemberBean?>?,
                p2: String?
            ) {
                L.d(TAG, "fetchMemberList: onFailure: ${p0?.errorMsg}")
            }

            override fun onSuccess(
                p0: BusinessResponse?,
                p1: ArrayList<InvitationMemberBean?>?,
                p2: String?
            ) {
                memberList.clear()
                p1?.run { memberList.addAll(this) }
            }

        })
    }

    private fun fetchCurrentMember() {
        TuyaSmartResidenceSdk.siteManager().fetchLoginedSiteMemberInfo(Global.currentSite?.homeId?:return,
            object : Business.ResultListener<ArrayList<MemberResponseBean?>?> {
                override fun onFailure(
                    p0: BusinessResponse?,
                    p1: ArrayList<MemberResponseBean?>?,
                    p2: String?
                ) {
                    L.d(TAG, "fetchCurrentMember: onFailure: ${p0?.errorMsg}")
                }

                override fun onSuccess(
                    p0: BusinessResponse?,
                    p1: ArrayList<MemberResponseBean?>?,
                    p2: String?
                ) {
                    L.d(TAG, "fetchCurrentMember: onSuccess: ${p1.toString()}")
                    p1?.run { currentMember.addAll(this) }
                }

            })
    }

    private fun fetchSiteDetailList() {
        TuyaSmartResidenceSdk.siteManager().fetchSiteDetail(Global
            .currentSite!!, object : ITuyaSiteResultCallback {
            override fun onSuccess(bean: SiteBean?) {
                L.d(TAG, JSON.toJSONString(bean))
                roomList.clear()
                bean?.rooms?.run { roomList.addAll(this) }
            }

            override fun onError(errorCode: String?, errorMsg: String?) {
                L.d(TAG, "fetchSiteDetailList: errorCode = $errorCode, errorMsg = $errorMsg")
            }

        })
    }
}

@Composable
fun SiteDetailRoomItemView(bean: RoomBean) {
    Column {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(bean.name?:"", fontSize = 16.sp, modifier = Modifier.weight(1.0F, true))
        }
        RowSpaceLine()
    }
}
@Composable
fun SiteDetailMemberItemView(bean: InvitationMemberBean?) {
    Column {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(bean?.getName()?:"", fontSize = 16.sp, modifier = Modifier.weight(1.0F, true))
        }
        RowSpaceLine()
    }
}
@Composable
fun SiteDetailCurrentMemberView(bean: MemberResponseBean?) {
    Column {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(bean?.getName()?:"", fontSize = 16.sp, modifier = Modifier.weight(1.0F, true))
        }
        RowSpaceLine()
    }
}




