package com.netpluspay.netpossdk

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import com.netpluspay.netpossdk.emv.param.EmvParam
import com.netpluspay.netpossdk.utils.DeviceConfig
import com.netpluspay.netpossdk.utils.TerminalParameters
import com.netpluspay.netpossdk.utils.TripleDES
import com.netpluspay.netpossdk.utils.tlv.HexUtil
import com.pos.sdk.accessory.POIGeneralAPI
import com.pos.sdk.emvcore.POIEmvCoreManager
import com.pos.sdk.emvcore.PosEmvAid
import com.pos.sdk.emvcore.PosEmvCapk
import com.pos.sdk.printer.POIPrinterManage
import com.pos.sdk.security.POIHsmManage
import com.pos.sdk.security.PedKcvInfo
import com.pos.sdk.security.PedKeyInfo

object NetPosSdk {
    private const val HAS_LOADED_PARAMS = "has_loaded_params"
    private lateinit var printerManager: POIPrinterManage
    private lateinit var context: Context

    @JvmStatic
    fun init(context: Context) {
        this.context = context
        initTerminalConfiguration()
        val poiGeneralAPI = POIGeneralAPI.getDefault()
        poiGeneralAPI.apply {
//            Log.e("TAG", "HW: ${getVersion(POIGeneralAPI.VERSION_TYPE_HW)}")
//            Log.e("TAG", "AP: ${getVersion(POIGeneralAPI.VERSION_TYPE_AP)}")
//            Log.e("TAG", "DSN: ${getVersion(POIGeneralAPI.VERSION_TYPE_DSN)}")
//            Log.e("TAG", "SP: ${getVersion(POIGeneralAPI.VERSION_TYPE_SP)}")

        }
        printerManager = POIPrinterManage.getDefault(context)
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(HAS_LOADED_PARAMS, false)
        )
            loadEmvParams(context)
    }
    
    @JvmStatic
    fun getDeviceSerial(): String = POIGeneralAPI.getDefault().getVersion(POIGeneralAPI.VERSION_TYPE_DSN)

    @JvmStatic
    private fun initTerminalConfiguration() {
//        Log.e("TAG", "Model ${Build.MODEL}")
//        Log.e("TAG", "Manufacturer ${Build.MANUFACTURER}")
//        Log.e("TAG", "Brand ${Build.BRAND}")
        if (Build.MODEL == "P3")
            DeviceConfig.InitDevice(DeviceConfig.DEVICE_P3)
        else
            DeviceConfig.InitDevice(DeviceConfig.DEVICE_P4)
//        if (Build.MODEL.endsWith("Z-600")) {
//            DeviceConfig.InitDevice(DeviceConfig.DEVICE_P4)
//        } else {
//            DeviceConfig.InitDevice(DeviceConfig.DEVICE_P3)
//        }
    }

    @JvmStatic
    internal fun getContext() = context

    @JvmStatic
    fun getPrinterManager(): POIPrinterManage {
        if (::printerManager.isInitialized.not())
            throw NullPointerException(
                "NetposSdk has not been initialized, you must call " +
                        "NetposSdk.init(context) first, this call needs to be made just once."
            )
        return printerManager
    }

    @JvmStatic
    private fun loadEmvParams(context: Context) {
        EmvParam.loadTerminalParam(TerminalParameters())
        EmvParam.loadVisaParam()
        EmvParam.loadMasterCardParam()
        //EmvParam.loadAmexParam()
        //EmvParam.loadDiscoverParam()
        //EmvParam.loadQuicsParam()
        EmvParam.loadVisaDrl()
        //EmvParam.loadAemxDrl()
        // EmvParam.loadRuPayService()
        EmvParam.loadAddAids()
        EmvParam.loadAddCapks()
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putBoolean(HAS_LOADED_PARAMS, true).apply()
    }

    @JvmStatic
    fun loadProvidedCapksAndAids(){
        EmvParam.loadAddAids()
        EmvParam.loadAddCapks()
    }

    @JvmStatic
    fun loadAids(aidList: List<PosEmvAid>){
        val mEmvCoreManage = POIEmvCoreManager.getDefault()
        aidList.forEach {
            mEmvCoreManage.EmvAddAid(it)
        }
    }

    @JvmStatic
    fun clearAids(){
        val mEmvCoreManage = POIEmvCoreManager.getDefault()
        mEmvCoreManage.EmvDelAllAid()
    }

    @JvmStatic
    fun clearCapks(){
        val mEmvCoreManage = POIEmvCoreManager.getDefault()
        mEmvCoreManage.EmvDelAllCapk()
    }

    @JvmStatic
    fun loadCapks(capkLisk: List<PosEmvCapk>){
        val mEmvCoreManager = POIEmvCoreManager.getDefault()
        capkLisk.forEach {
            mEmvCoreManager.EmvAddCapk(it)
        }
    }

    @JvmStatic
    fun writeTpkKey(keyIndex: Int, keyData: String): Int {
        val pedKeyInfo = PedKeyInfo(
            0, 0, POIHsmManage.PED_TPK, keyIndex, 0, 16,
            HexUtil.parseHex(keyData)
        )
        return POIHsmManage.getDefault().PedWriteKey(pedKeyInfo, PedKcvInfo(0, ByteArray(5)))
    }

    @JvmStatic
    fun writeTpkKey(keyIndex: Int, encryptedKeyData: String, masterKey: String){
        writeTpkKey(keyIndex, keyData = TripleDES.decrypt(encryptedKeyData, masterKey))
    }
}