/*
 * Created by chenru on 2019/06/20.
 * Copyright 2015－2020 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.demo.activity

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import com.sensorsdata.analytics.android.demo.R
import com.sensorsdata.analytics.android.sdk.PropertyBuilder
import com.sensorsdata.analytics.android.sdk.SALog
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI
import com.sensorsdata.analytics.android.sdk.util.JSONUtils
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils
import kotlinx.android.synthetic.main.activity_base_property.*
import java.util.*
import kotlin.collections.HashMap

class BasePropertyActivity : BaseActivity() {


    private val TAG: String = "BasePropertyActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_property)
        initView()
    }

    private fun initView() {
        removeSuperPropertyBtn.setOnClickListener {
            SensorsDataAPI.sharedInstance(this).unregisterSuperProperty("superkey")
            SensorsDataAPI.sharedInstance(this).unregisterSuperProperty("superkey2")
            showData()
        }

        //设置全局属性
        SensorsDataAPI.sharedInstance(this).registerSuperProperties(PropertyBuilder.newInstance()
                .append("superkey", "supervalue" + Random().nextInt())
                .append("superkey2", "supervalue2" + Random().nextInt()).toJSONObject())
        SensorsDataAPI.sharedInstance().trackFragmentAppViewScreen()

        showData()
    }


    private fun showData() {
        //显示一些通用设置
        val map = setupDeviceInfo()
        val jsonObject = SensorsDataAPI.sharedInstance(this).superProperties
        map.forEach {
            jsonObject.put(it.key, it.value)
        }
        SALog.i(TAG, JSONUtils.formatJson(jsonObject.toString()))
        basePropertyTV.text = JSONUtils.formatJson(jsonObject.toString())
    }

    private fun setupDeviceInfo(): Map<String, Any> {
        val deviceInfo = HashMap<String, Any>()
        deviceInfo["\$lib"] = "Android"
        //deviceInfo["\$lib_version"] = SensorsDataAPI.VERSION //TODO 用到 VERSION 需要将修饰符改成public， 待定
        deviceInfo["\$os"] = "Android"
        deviceInfo["\$os_version"] = if (Build.VERSION.RELEASE == null) "UNKNOWN" else Build.VERSION.RELEASE
        deviceInfo["\$manufacturer"] = SensorsDataUtils.getManufacturer()
        if (TextUtils.isEmpty(Build.MODEL)) {
            deviceInfo["\$model"] = "UNKNOWN"
        } else {
            deviceInfo["\$model"] = Build.MODEL.trim { it <= ' ' }
        }
        try {
            val manager = packageManager
            val info = manager.getPackageInfo(packageName, 0)
            deviceInfo["\$app_version"] = info.versionName
        } catch (e: Exception) {
            SALog.i(TAG, "Exception getting app version name", e)
        }

        //context.getResources().getDisplayMetrics()这种方式获取屏幕高度不包括底部虚拟导航栏
        val displayMetrics = resources.displayMetrics
        var screenWidth = displayMetrics.widthPixels
        var screenHeight = displayMetrics.heightPixels

        try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val rotation = display.rotation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val point = Point()
                display.getRealSize(point)
                screenWidth = point.x
                screenHeight = point.y
            }
            deviceInfo["\$screen_width"] = SensorsDataUtils.getNaturalWidth(rotation, screenWidth, screenHeight)
            deviceInfo["\$screen_height"] = SensorsDataUtils.getNaturalHeight(rotation, screenWidth, screenHeight)
        } catch (e: Exception) {
            deviceInfo["\$screen_width"] = screenWidth
            deviceInfo["\$screen_height"] = screenHeight
        }

        val carrier = SensorsDataUtils.getCarrier(this)
        if (!TextUtils.isEmpty(carrier)) {
            deviceInfo["\$carrier"] = carrier
        }

        val mAndroidId = SensorsDataUtils.getAndroidID(this)

        if (!TextUtils.isEmpty(mAndroidId)) {
            deviceInfo["\$device_id"] = mAndroidId
        }

        val zoneOffset = SensorsDataUtils.getZoneOffset()
        if (zoneOffset != null) {
            //deviceInfo.put("$timezone_offset", zone_offset);
        }

        val networkType = NetworkUtils.networkType(this)
        deviceInfo["\$wifi"] = networkType == "WIFI"
        deviceInfo["\$network_type"] = networkType

        return Collections.unmodifiableMap(deviceInfo)
    }

}