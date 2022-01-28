package com.tuya.smart.sr.srdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuya.smart.android.common.utils.L
import com.tuya.smart.android.network.Business
import com.tuya.smart.android.network.http.BusinessResponse
import com.tuya.smart.android.user.api.ILogoutCallback
import com.tuya.smart.srsdk.api.site.bean.SiteBean
import com.tuya.smart.srsdk.sdk.TuyaSmartResidenceSdk
import com.tuya.smart.wrapper.api.TuyaWrapper
import java.util.*
import com.tuya.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService
import com.tuya.smart.api.service.MicroServiceManager
import com.tuya.smart.sr.R
import com.tuya.smart.sr.srdemo.access.*
import com.tuya.smart.sr.srdemo.account.SigninActivity
import com.tuya.smart.sr.srdemo.personal.PersonalManagerActivity
import com.tuya.smart.sr.srdemo.site.*
import com.tuya.smart.sr.srdemo.uibusiness.UiBusinessPackegeActivity
import com.tuya.smart.srsdk.api.site.interfaces.ITuyaSiteResultCallback
import com.tuyasmart.stencil.component.webview.config.GlobalConfig
import com.zj.refreshlayout.SwipeRefreshLayout


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwipeRefreshLayouts()
        }
        getSiteList()
    }
}

@Preview
@Composable
fun MainView() {
    val state = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = Color(242, 242, 242))
            .verticalScroll(state)
    ) {



        UserManagementView()

        SiteManagementView()

//        AccessManagementView()

        PanelView()

        LogOut()

    }

}

val rowModifier = Modifier
    .fillMaxWidth()
    .background(color = Color.White)
    .height(50.dp)
    .padding(8.dp)

@Composable
fun RowSpaceLine() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.3.dp)
            .background(color = Color.Gray)
    )
}

var refreshing = mutableStateOf(false)

@Composable
fun SwipeRefreshLayouts() {
    SwipeRefreshLayout(
        isRefreshing = refreshing.value,
        content = { MainView() },
        onRefresh = {
            refreshing.value = true
            getSiteList()
        }
    )

}


@Composable
fun RowSpacer() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
    )
}

@Composable
fun ArrowImage() {
    Image(
        painter = painterResource(id = R.drawable.icon_right_arrow),
        contentDescription = "",
        Modifier.size(24.dp)
    )
}

@Composable
fun TypeText(typeName: String) {
    Text(typeName, fontSize = 18.sp, modifier = Modifier.padding(8.dp))
}

@Composable
fun TextItem(
    text: String,
    bottomLine: Boolean = false,
    value: String = "",
    rightRrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = rowModifier
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 16.sp, modifier = Modifier.weight(1.0F, true))
        Text(value, fontSize = 14.sp)
        if (rightRrow) {
            ArrowImage()
        }
    }
    if (bottomLine) {
        RowSpaceLine()
    }
}

@Composable
fun UserManagementView() {
    val context = LocalContext.current
    RowSpacer()
    TypeText("User Management")
    TextItem(text = "Personal Information") {
        context.startActivity(Intent(context, PersonalManagerActivity::class.java))
    }
}

@Composable
fun SiteManagementView() {
    RowSpacer()
    TypeText("Site Management")
    CreateASite()
    CurrentSite()
    SiteList()
    SiteDetail()
//    ModifySiteInformation()
//    AddMember()
//    FetchExpiredList()
}

@Composable
fun CreateASite() {
    val context = LocalContext.current
    val registerForActivityResult = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.run {
            getSiteList()
        }
    }
    TextItem(text = "Create A Site", bottomLine = true) {
        registerForActivityResult.launch(Intent(context, CreateSiteActivity::class.java))
    }
}

var siteName = mutableStateOf("")

@Composable
fun CurrentSite() {
    siteName = rememberSaveable { mutableStateOf(Global.currentSite?.name ?: "") }
    TextItem(text = "Current Site", bottomLine = true, siteName.value, rightRrow = false) {}
}


@Composable
fun SiteList() {
    val context = LocalContext.current
    var siteId by rememberSaveable { mutableStateOf(Global.currentSite?.homeId ?: -1L) }
    val registerForActivityResult = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.run {
            siteName.value = getStringExtra("currentSiteName").toString()
            siteId = getLongExtra("currentSiteId", -1L)
            val service =
                MicroServiceManager.getInstance().findServiceByInterface<AbsBizBundleFamilyService>(
                    AbsBizBundleFamilyService::class.java.name
                )
            // 设置为当前家庭的 homeId 和 homeName
            service.shiftCurrentFamily(siteId, siteName.value)
        }
    }
    TextItem(text = "Select Site", bottomLine = true) {
        if (Global.siteMap.values.toList().isEmpty()) {
            Toast.makeText(context, "site list is empty ,you can create site", Toast.LENGTH_LONG)
                .show()
            return@TextItem
        }
        registerForActivityResult.launch(Intent(context, CurrentSiteActivity::class.java))
    }
}


fun getSiteList() {
    TuyaSmartResidenceSdk
        .siteManager()
        .fetchSiteList(object : Business
        .ResultListener<ArrayList<SiteBean?>?> {
            override fun onFailure(
                p0: BusinessResponse?,
                p1: ArrayList<SiteBean?>?,
                p2: String?,
            ) {
                refreshing.value = false;
                L.d(
                    TAG, "onFailure BusinessResponse = $p0, ArrayList<SiteBean?> = $p1, " +
                            "String = $p2"
                )
            }

            override fun onSuccess(
                p0: BusinessResponse?,
                siteList: ArrayList<SiteBean?>?,
                p2: String?,
            ) {
                refreshing.value = false;
                L.d(
                    TAG, "onSuccess BusinessResponse = $p0, ArrayList<SiteBean?> =" +
                            "$siteList, String = $p2"
                )
                if (!siteList.isNullOrEmpty()) {
                    //刷新当前列表
                    Global.siteMap.clear()
                    siteList.forEach {
                        if (it != null) {
                            Global.siteMap[it.homeId] = it
                        }
                    }

                    if(Global.currentSite==null){
                        Global.currentSite = siteList[0]
                    }else{
                        if(!Global.siteMap.keys.contains(Global.currentSite!!.homeId)){
                            Global.currentSite = siteList[0]
                        }
                    }

                    // 设置为当前家庭的 homeId 和 homeName
                    Global.currentSite?.let {
                        val service =
                            MicroServiceManager.getInstance()
                                .findServiceByInterface<AbsBizBundleFamilyService>(
                                    AbsBizBundleFamilyService::class.java.name
                                )
                        service.shiftCurrentFamily(
                            it.homeId,
                            it.name
                        )

                        siteName.value = it.name
                    }


                }else{
                    Global.clear()
                }
            }

        })
}

@Composable
fun SiteDetail() {
    val context = LocalContext.current
    TextItem(text = "Site Detail", bottomLine = true) {
        if(Global.currentSite!=null){
            context.startActivity(Intent(context, SiteDetailActivity::class.java))
        }else{
            Toast.makeText(context, "current site is empty , you can create a site", Toast.LENGTH_LONG).show()
        }

    }
}



@Composable
fun ModifySiteInformation() {
    val context = LocalContext.current
    val registerForActivityResult = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.run {
            getSiteList()
        }
    }
    TextItem(text = "Modify Site Information", bottomLine = true) {
        registerForActivityResult.launch(Intent(context, ModifySiteActivity::class.java))
    }
}

@Composable
fun AddMember() {
    val context = LocalContext.current
    TextItem(text = "Add Member", bottomLine = true) {
        context.startActivity(Intent(context, AddMemberActivity::class.java))
    }
}

@Composable
fun FetchExpiredList() {
    val context = LocalContext.current
    TextItem(text = "Fetch Expired List") {
        context.startActivity(Intent(context, ExpiredListActivity::class.java))
    }
}


@Composable
fun AccessManagementView() {
    RowSpacer()
    TypeText("Access Management")
    DeviceList()
    AddAppAccess()
    QueryAppAccess()
    AddPasswordAccess()
    QueryPasswordAccess()
    FetchAppAccessTotal()
    FetchPasswordAccessTotal()
    CheckAccessAccount()
    CheckAccessAuthorization()

}


@Composable
fun DeviceList() {
    val context = LocalContext.current
    TextItem(text = "access device List", bottomLine = true) {
        if (Global.currentSite == null) {
            Toast.makeText(context, "current site is empty,you can create site", Toast.LENGTH_SHORT)
                .show()
            return@TextItem
        }
        TuyaSmartResidenceSdk.siteManager().fetchSiteDetail(Global
            .currentSite!!, object : ITuyaSiteResultCallback {
            override fun onSuccess(bean: SiteBean?) {
                context.startActivity(Intent(context, DeviceListActivity::class.java))
            }
            override fun onError(errorCode: String?, errorMsg: String?) {
                Toast.makeText(GlobalConfig.context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        })
    }
}

@Composable
fun AddAppAccess() {
    val context = LocalContext.current
    TextItem(text = "Add App Access", bottomLine = true) {
        context.startActivity(Intent(context, AddAppAccessActivity::class.java))
    }
}

@Composable
fun QueryAppAccess() {
    val context = LocalContext.current
    TextItem(text = "Query App Access", bottomLine = true) {
        context.startActivity(Intent(context, QueryAppAccessActivity::class.java))
    }
}

@Composable
fun AddPasswordAccess() {
    val context = LocalContext.current
    TextItem(text = "Add Password Access", bottomLine = true) {
        context.startActivity(Intent(context, AddPasswordAccessActivity::class.java))
    }
}

@Composable
fun QueryPasswordAccess() {
    val context = LocalContext.current
    TextItem(text = "Query Password Access", bottomLine = true) {
        context.startActivity(Intent(context, QueryPasswordAccessActivity::class.java))
    }
}


@Composable
fun FetchAppAccessTotal() {
    var totalCount: String by remember { mutableStateOf("") }
    TextItem(text = "App Access Total Count", bottomLine = true, totalCount) {
        Global.currentSite?.homeId?.let {
            TuyaSmartResidenceSdk
                .access()
                .fetchAppAccessTotal(it.toString(), 1, object : Business.ResultListener<Int> {
                    override fun onFailure(p0: BusinessResponse?, p1: Int?, p2: String?) {
                        L.d(TAG, "fetchAppAccessTotal failure:${p0?.errorMsg}")
                    }

                    override fun onSuccess(p0: BusinessResponse?, p1: Int?, p2: String?) {
                        totalCount = p1
                            ?.toString()
                            .plus("个")
                    }

                })
        }
    }
}

@Composable
fun FetchPasswordAccessTotal() {
    var totalCount: String by remember { mutableStateOf("") }
    TextItem(text = "Password Access Total Count", bottomLine = true, totalCount) {
        Global.currentSite?.homeId?.let {
            TuyaSmartResidenceSdk
                .access()
                .fetchPasswordAccessTotal(it.toString(), 1, object : Business.ResultListener<Int> {
                    override fun onFailure(p0: BusinessResponse?, p1: Int?, p2: String?) {
                        L.d(TAG, "fetchPasswordAccessTotal failure:${p0?.errorMsg}")
                    }

                    override fun onSuccess(p0: BusinessResponse?, p1: Int?, p2: String?) {
                        totalCount = p1
                            ?.toString()
                            .plus("个")
                    }

                })
        }
    }
}

@Composable
fun CheckAccessAccount() {
    var isRegistered: String by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .height(50.dp)
            .padding(start = 8.dp, end = 8.dp)
            .clickable(onClick = {
                TuyaSmartResidenceSdk
                    .access()
                    .checkAccessAccount("", object : Business.ResultListener<Boolean> {
                        override fun onFailure(p0: BusinessResponse?, p1: Boolean?, p2: String?) {
                            L.d(TAG, "CheckAccessAccount failure:${p0?.errorMsg}")
                        }

                        override fun onSuccess(p0: BusinessResponse?, p1: Boolean?, p2: String?) {
                            isRegistered = if (p1 == true) "Registered" else "Not registered"
                        }

                    })

            }),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(modifier = Modifier.weight(1.0F, true)) {
            Text("Check Access Account", fontSize = 16.sp)
            Text("(Check whether the account is registered)", fontSize = 12.sp)
        }
        Text(isRegistered, fontSize = 12.sp)
        ArrowImage()
    }
    RowSpaceLine()
}

@Composable
fun CheckAccessAuthorization() {
    var isAuthorized: String by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .height(50.dp)
            .padding(start = 8.dp, end = 8.dp)
            .clickable(onClick = {
                Global.currentSite?.homeId?.let {
                    TuyaSmartResidenceSdk
                        .access()
                        .checkAccessAuthorization(it.toString(),
                            "",
                            object : Business.ResultListener<Int> {
                                override fun onFailure(
                                    p0: BusinessResponse?,
                                    p1: Int?,
                                    p2: String?
                                ) {
                                    L.d(TAG, "CheckAccessAuthorization failure:${p0?.errorMsg}")
                                }

                                override fun onSuccess(
                                    p0: BusinessResponse?,
                                    p1: Int?,
                                    p2: String?
                                ) {
                                    isAuthorized = if (p1 == 0) "Authorized" else "Not Authorized"
                                }

                            })
                }
            }),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(modifier = Modifier.weight(1.0F, true)) {
            Text("Check Access Authorization", fontSize = 16.sp)
            Text("(Check whether the account is authorized)", fontSize = 12.sp)
        }
        Text(isAuthorized, fontSize = 12.sp)
        ArrowImage()
    }

}


@Composable
fun PanelView() {
    val context = LocalContext.current
    RowSpacer()
    TypeText("UI")
    TextItem(text = "UI Business Packege", bottomLine = true) {
        context.startActivity(Intent(context, UiBusinessPackegeActivity::class.java))
    }
}


@Composable
fun LogOut() {
    val context = LocalContext.current
    Button(
        onClick = {
            TuyaSmartResidenceSdk.account().logout(object : ILogoutCallback {
                override fun onSuccess() {

                    Global.clear()

                    //退出
                    TuyaWrapper.onLogout(context);

                    context.startActivity(Intent(context, SigninActivity::class.java))
                    L.d(TAG, "Logout success")
                    (context as MainActivity).finish()


                }

                override fun onError(code: String?, error: String?) {
                    L.d(TAG, "Logout error: code = $code, error = $error")
                }

            })
        }, modifier =
        Modifier
            .fillMaxWidth()
            .padding(30.dp)
    ) {
        Text(text = "Logout")
    }
}

