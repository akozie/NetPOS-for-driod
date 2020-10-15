package com.netplus.sunyardlib;

import android.content.Context;
import android.os.Bundle;

import com.socsi.aidl.pinservice.OperationPinListener;
import com.socsi.smartposapi.card.CardReader;
import com.socsi.smartposapi.emv2.AsyncEmvCallback;
import com.socsi.smartposapi.emv2.EmvL2;
import com.socsi.smartposapi.icc.Icc;
import com.socsi.smartposapi.ped.KeyBoardConstant;
import com.socsi.smartposapi.ped.Ped;
import com.socsi.smartposapi.terminal.TerminalManager;
import com.socsi.utils.HexDump;
import com.socsi.utils.Log;
import com.socsi.utils.TlvUtil;
import com.socsi.utils.log4j.Priority;
import com.sunyard.i80.util.StringUtil;
import com.sunyard.i80.util.TlvUtils;
import com.sunyard.i80.util.Util;
import com.sunyard.middleware.emvl2lib.EmvCallbackGetPinResult;
import com.sunyard.middleware.emvl2lib.EmvErrorCode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import socsi.middleware.emvl2lib.EmvApi;
import socsi.middleware.emvl2lib.EmvCallback;
import socsi.middleware.emvl2lib.EmvStartProcessParam;

import static com.netplus.sunyardlib.ICCCardProcessHandler.EMV_ERR_BASE;
import static com.netplus.sunyardlib.ICCCardProcessHandler.EMV_TRANS_ACCEPT;
import static com.netplus.sunyardlib.ICCCardProcessHandler.EMV_TRANS_DENIAL;
import static com.netplus.sunyardlib.ICCCardProcessHandler.EMV_TRANS_FALLBACK;
import static com.netplus.sunyardlib.ICCCardProcessHandler.EMV_TRANS_QPBOC_ACCEPT;
import static com.netplus.sunyardlib.ICCCardProcessHandler.EMV_TRANS_QPBOC_DENIAL;
import static com.netplus.sunyardlib.ICCCardProcessHandler.EMV_TRANS_TERMINATE;
import static com.netplus.sunyardlib.ICCCardProcessHandler.UNVERIFIED_RESULT;
import static com.sunyard.i80.util.Keys.mainKeyDefault;
import static com.sunyard.i80.util.Keys.workKeyDefault;

/**
 *
 */
public class CardReaderService {

    private static String TAG = CardReaderService.class.getName();
    private static CardReader cardReader = CardReader.getInstance();

    public static Observable<CardReaderEvent> initiateICCCardPayment(Context context, long amount, long cashBackAmount, String clearKeyPin) {
        return Observable.create(emitter -> {
            CardReader cardReader = CardReader.getInstance();
            cardReader.setIcCardSearchCallback(ret -> {
                switch (ret) {
                    case 0:
                        Log.e("Start here", "initialting payment here");
                        initiateICCCardPayment(clearKeyPin, context, amount, cashBackAmount, emitter);
                        return;
                    case 1:
                        emitter.onError(new NetposException(CardSearchHandler.TIMEOUT, "Card read timeout"));
                        break;
                    case 2:
                        emitter.onError(new NetposException(CardSearchHandler.UNKNOWN_CARD, "Unknown card"));
                        break;
                    default:
                        emitter.onError(new NetposException(CardSearchHandler.READ_FAIL, "Card read failed"));
                        break;
                }
            });
            byte type = 0x00;
            //type = (byte) (type | 0x01);    //support magnetic card
            type = (byte) (type | 0x02);    //support ic card
            //type = (byte) (type | 0x04);    //support non-contact card
            cardReader.searchCard(type, 20 * 1000);
        });
    }

    private static void initiateICCCardPayment(String clearPinKey, Context context, long amount, long cashBackAmount, ObservableEmitter<CardReaderEvent> emitter) {
        emitter.onNext(new CardReaderEvent.CardDetected());
        android.util.Log.d(TAG, "readICCCard: in");
        EmvL2.getInstance(context, context.getPackageName()).resetProcess();
        EmvStartProcessParam emvStartProcessParam = new EmvStartProcessParam();
        String SN = TerminalManager.getInstance().getSN();
        emvStartProcessParam.mIfdSerialNum = SN.getBytes();
        emvStartProcessParam.mProcType = EmvStartProcessParam.EMV_API_PROC_PBOC_FULL;

        emvStartProcessParam.mTransType = EmvStartProcessParam.EMV_API_TRANS_TYPE_NORMAL;
        emvStartProcessParam.mSeqNo = NetPosUtils.nextTransactionCounterValue(context);
        emvStartProcessParam.mTransAmt = amount;
        emvStartProcessParam.mCashbackAmt = cashBackAmount;
        //emvStartProcessParam.mTransCurrCode = "0556".getBytes();
        //emvStartProcessParam.mTermCountryCode = "0556".getBytes();
        emvStartProcessParam.mTransCurrCode = HexDump.hexStringToByteArray("0566");
        emvStartProcessParam.mTermCountryCode = HexDump.hexStringToByteArray("0566");
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        emvStartProcessParam.mTransDate = StringUtil.hexStr2Bytes(dateFormat.format(date));
        DateFormat timeFormat = new SimpleDateFormat("HHmmss");
        emvStartProcessParam.mTransTime = StringUtil.hexStr2Bytes(timeFormat.format(date));
        emvStartProcessParam.mTag9CValue = 0x00;
        emvStartProcessParam.mIsSupportEC = true;
        emvStartProcessParam.mChannelType = EmvStartProcessParam.EMV_API_CHANNEL_FROM_ICC;
        emvStartProcessParam.mIsQpbocForceOnline = true;
        EmvL2.getInstance(context, context.getPackageName()).init();
        String[] aids = getAids();

        int ret = EmvL2.getInstance(context, context.getPackageName()).clearAids();
        if (ret != 0) {
            emitter.onError(new NetposException(ICCCardProcessHandler.CLEAR_AIDS_FAILED, "clear aids failed"));
            return;
        }

        ret = EmvL2.getInstance(context, context.getPackageName()).addAids(aids);
        if (ret != 0) {
            Log.d(TAG, "readICCCard: add aids error");

        }

        //check if des type is correct for nibss
        //EmvL2.getInstance(context, context.getPackageName()).setDukptParam((byte) 1, Ped.DES_TYPE_3DES, (byte) 0x02, null);

        EmvL2.getInstance(context, context.getPackageName()).startProcess(emvStartProcessParam, new AsyncEmvCallback() {

            private byte[] pan = null;

            @Override
            public void confirmTransType(byte[] aid, int oldTransType, EmvL2.ConfirmTransTypeHandler handler) {
                handler.onConfirmTransType(oldTransType);
            }

            @Override
            public void panCofirm(byte[] pan, EmvL2.PanConfirmHandler panConfirmHandler) {
                Log.d("EMVDevice process startProcess panConfirm");
                //0: success, others: fail
                this.pan = pan;
                panConfirmHandler.onPanConfirm(0);
            }

            @Override
            public void getPin(int type, int retryTimes, EmvL2.GetPinHandler getPinHandler) {
                Log.d("EMVDevice process startProcess getPin");
                //getPin.getPin(pan, getPinHandler);
                getPinHandler.onGetPin(1, pan);
            }

            @Override
            public int onVerifyOfflinePin(byte b, int i) {
                return EmvCallbackGetPinResult.CV_PIN_SUCC;
            }

            @Override
            public void aidSelect(socsi.middleware.emvl2lib.EmvAidCandidate[] emvAidCandidates, int i, EmvL2.AidSelectHandler aidSelectHandler) {
                Log.d("EMVDevice process startProcess aidSelect");
                aidSelectHandler.onAidSelect(0);
            }

            @Override
            public void termRiskManager(byte[] pan, int panSn, EmvL2.TermRiskManageHandler termRiskManageHandler) {
                Log.d("EMVDevice process startProcess termRiskManager");
                termRiskManageHandler.onTermRiskManager(0, 0);
            }

            @Override
            public void issuerReference(byte[] bytes, EmvL2.IssuerReferenceHandler issuerReferenceHandler) {
                Log.d("EMVDevice process startProcess issuerReference");
                issuerReferenceHandler.onIssuerReference(0);
            }

            @Override
            public void accountTypeSelect(EmvL2.AccountTypeSelectHandler accountTypeSelectHandler) {
                accountTypeSelectHandler.onAccountType(EmvCallback.EMV_ACCOUNT_TYPE_DEFAULT);
            }

            @Override
            public void certConfirm(int type, byte[] certNum, EmvL2.CertConfirmHandler certConfirmHandler) {
                Log.d("EMVDevice process startProcess certConfirm");
                certConfirmHandler.onCertConfirm(1);
            }

            @Override
            public void lcdMsg(byte[] bytes, byte[] bytes1, boolean b, int i, EmvL2.LcdMsgHandler lcdMsgHandler) {
                Log.d("EMVDevice process startProcess lcdMsg");
                lcdMsgHandler.onLcdMsg(1);
            }

            @Override
            public void confirmEC(EmvL2.ConfirmEcHandler confirmEcHandler) {
                Log.d("EMVDevice process startProcess confirmEC");
                confirmEcHandler.onConfirmEc(0);
            }

            @Override
            public void processResult(int ret) {
                Log.d("EMVDevice process startProcess processResult:" + ret);

                int code = 1;
                switch (ret) {
                    case EmvApi.EMV_TRANS_FALLBACK:
                        code = EMV_TRANS_FALLBACK;
                        break;
                    case EmvApi.EMV_TRANS_TERMINATE:
                        code = EMV_TRANS_TERMINATE;
                        break;
                    case EmvApi.EMV_TRANS_ACCEPT:
                        updateICCardData(context, EmvApi.EMV_TRANS_ACCEPT, emitter, clearPinKey);
                        return;
                    case EmvApi.EMV_TRANS_DENIAL:
                        code = EMV_TRANS_DENIAL;
                        break;
                    case EmvApi.EMV_TRANS_GOONLINE:
                        updateICCardData(context, EmvApi.EMV_TRANS_GOONLINE, emitter, clearPinKey);
                        return;
                    case EmvApi.EMV_TRANS_QPBOC_ACCEPT:
                        code = EMV_TRANS_QPBOC_ACCEPT;
                        break;
                    case EmvApi.EMV_TRANS_QPBOC_DENIAL:
                        code = EMV_TRANS_QPBOC_DENIAL;
                        break;
                    case EmvApi.EMV_TRANS_QPBOC_GOONLINE:
                        updateICCardData(context, EmvApi.EMV_TRANS_QPBOC_GOONLINE, emitter, clearPinKey);
                        return;
                    case EmvErrorCode.EMV_ERR_BASE:
                        code = EMV_ERR_BASE;
                        break;
                    case -1334:
                        Icc.getInstance().close(Icc.INIT_TYEP_RF);
                    default:
                        code = UNVERIFIED_RESULT;

                }

                emitter.onError(new NetposException(code, ICCCardProcessHandler.errorMessage(code)));

            }
        });


    }

    private static String[] getAids() {
        return new String[]{
                "9f0607a0000000033010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010103df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000651010df0101009f08020200df1105fc6024a800df1205fc60acf800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000032010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000031010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010106df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010101df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000043060df0101009f08020002df1105fc5058a000df1205f85058f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010102df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000041010df0101009f08020002df1105fc5080a000df1205f85080f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607A0000000049999df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607A0000000046000df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f660430008181"
        };
    }

    private static void updateICCardData(Context context, int cardReadResultCode, ObservableEmitter<CardReaderEvent> cardDataEmitter, String clearPinKey) {

        android.util.Log.d(TAG, "updateICCardData: in");
//        byte[] cardData = getTlvData(context, context.getPackageName(), "5A575F345F20" + "9F269F279F109F379F36959A9C9F025F2A829F1A9F039F339F349F359F1E849F099F419F635F309F208E" +
//                "917172DF32DF33DF345F249F129F53");
        byte[] cardData = getTlvData(context, context.getPackageName(), "5A575F345F20" + "9F269F279F109F379F36959A9C9F025F2A829F1A9F039F339F349F359F1E849F099F419F638E" +
                "917172DF32DF33DF345F249F129F53");

        if (cardData == null) {
            android.util.Log.d(TAG, "updateICCardData: error");
            cardDataEmitter.onError(new NetposException(ICCCardProcessHandler.GET_ICC_DATA_FAILED, ICCCardProcessHandler.errorMessage(ICCCardProcessHandler.GET_ICC_DATA_FAILED)));
            return;
        }
        List<Tlv> list = TlvUtils.builderTlvList(Util.BytesToString(cardData));

        showPinPad(context, new CardReadResult(cardReadResultCode, list, Util.BytesToString(cardData)), clearPinKey, cardDataEmitter);

    }

    /*@SuppressWarnings("deprecation")
    private static void showPinPad(Context context, CardReadResult cardReadResult, String clearPinKey, ObservableEmitter<CardReaderEvent> emitter) {
        Ped.getInstance().loadMKey((byte) 0xff, (byte) 1, mainKeyDefault, 1, Ped.KEY_TYPE_UNENCTYPTED_KEY, null, false);
        Ped.getInstance().loadWorkKeyByIdx((byte) 1, Ped.WORK_KEY_TYPE_PIN_KEY, (byte) 1, clearPinKey, null);
        Bundle param = new Bundle();
        param.putBoolean("isOnline", true);
        param.putString("pan", cardReadResult.getApplicationPrimaryAccountNumber());
        param.putString("promptString", "Please input the pin");
        param.putIntArray("pinLimit", new int[]{4, 5, 6, 7, 8, 9, 10});
        param.putByte("keysType", Ped.KEYS_TYPE_MK_SK);
        param.putByte("pinAlgMode", Ped.ALGORITHMTYPE_USE_PAN_SUPPLY_F);
        param.putByteArray("random", Util.StringToBytes("AACC9675BBA5EF44"));
        param.putInt(KeyBoardConstant.BUNDLE_KEY_DESTYPE, 1);
        //param.putInt(KeyBoardConstant.BUNDLE_KEY_KEYSTYPE, 1);
        param.putInt("timeout", 90);
        Ped.getInstance().startPinInput(context, 1, param, new OperationPinListener() {

            @Override
            public void onInput(int i, int i1) {

            }

            @Override
            public void onConfirm(byte[] bytes, boolean b) {
                String pin = Util.BytesToString(bytes);
                Log.e(TAG, "Pin: "+pin);
//                pin = "0" + (pin.length()) + pin + "FFFFFFFFFF";
//                String cardNum = "0000" + cardReadResult.getApplicationPrimaryAccountNumber().substring(3, 15);
//                //cardReadResult.pinBlock = NetPosUtils.xorHex(pin, cardNum);
                cardReadResult.setEncryptedPinBlock(pin);
                emitter.onNext(new CardReaderEvent.CardRead(cardReadResult));
                emitter.onComplete();
            }

            @Override
            public void onCancel() {
                emitter.onError(new NetposException(1, "PinPad was closed by user"));
            }

            @Override
            public void onError(int i) {
                emitter.onError(new NetposException(i, "PinPad ran into an unexpected error"));
            }
        });
    }*/

    @SuppressWarnings("deprecation")
    private static void showPinPad(Context context, CardReadResult cardReadResult, String clearPinKey, ObservableEmitter<CardReaderEvent> emitter) {
        Bundle param = new Bundle();
        param.putBoolean("isOnline", false);
        param.putString("pan", cardReadResult.getApplicationPrimaryAccountNumber());
        param.putString("promptString", "Please input the pin");
        param.putIntArray("pinLimit", new int[]{4, 5, 6, 7, 8, 9, 10});
        param.putByte("keysType", Ped.KEYS_TYPE_MK_SK);
        param.putByte("pinAlgMode", Ped.ALGORITHMTYPE_USE_PAN_SUPPLY_F);
        param.putByteArray("random", Util.StringToBytes("AACC9675BBA5EF44"));
        param.putInt(KeyBoardConstant.BUNDLE_KEY_DESTYPE, 1);
        //param.putInt(KeyBoardConstant.BUNDLE_KEY_KEYSTYPE, 1);
        param.putInt("timeout", 90);
        Ped.getInstance().startPinInput(context, 1, param, new OperationPinListener() {

            @Override
            public void onInput(int i, int i1) {

            }

            @Override
            public void onConfirm(byte[] bytes, boolean b) {
                String pin = Util.BytesToString(bytes);
                pin = "0" + (pin.length()) + pin + "FFFFFFFFFF";
                String cardNum = "0000" + cardReadResult.getApplicationPrimaryAccountNumber().substring(3, 15);
                //cardReadResult.pinBlock = NetPosUtils.xorHex(pin, cardNum);
                cardReadResult.setEncryptedPinBlock(TripleDes.encrypt(NetPosUtils.xorHex(pin, cardNum), clearPinKey));
                emitter.onNext(new CardReaderEvent.CardRead(cardReadResult));
                emitter.onComplete();
            }

            @Override
            public void onCancel() {
                emitter.onError(new NetposException(1, "PinPad was closed by user"));
            }

            @Override
            public void onError(int i) {
                emitter.onError(new NetposException(i, "PinPad ran into an unexpected error"));
            }
        });
    }

    private static byte[] getTlvData(Context context, String packageName, String tlvString) {
        try {
            Map<String, String> mMap = new HashMap<String, String>();
            mMap.put("DF35", tlvString);
            return EmvL2.getInstance(context, packageName).getTLVData(TlvUtil.mapToTlv(mMap));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Single<CardReadResult> readCard(Context context) {

        return Single.create(emitter -> {
            try {

                cardReader.setIcCardSearchCallback(ret -> {
                    switch (ret) {
                        case 0:
                            readICCCard(context, emitter);
                            return;
                        case 1:
                            emitter.onError(new NetposException(CardSearchHandler.TIMEOUT, "Card read timeout"));
                            break;
                        case 2:
                            emitter.onError(new NetposException(CardSearchHandler.UNKNOWN_CARD, "Unknown card"));
                            break;
                        default:
                            emitter.onError(new NetposException(CardSearchHandler.READ_FAIL, "Card read failed"));
                            break;
                    }
                });

                byte type = 0x00;
                type = (byte) (type | 0x01);    //support magnetic card
                type = (byte) (type | 0x02);    //support ic card
                type = (byte) (type | 0x04);    //support non-contact card
                cardReader.searchCard(type, 20 * 1000);

            } catch (Exception e) {

                e.printStackTrace();
                cardReader.stopSearch(7);
                emitter.onError(new NetposException(-99, "An unexpected error occurred"));

            }
        });

    }

    private static void readICCCard(Context context, SingleEmitter<CardReadResult> emitter) {

        int channelType = EmvStartProcessParam.EMV_API_CHANNEL_FROM_ICC;

        EmvL2.getInstance(context, context.getPackageName()).resetProcess();
        EmvStartProcessParam emvStartProcessParam = new EmvStartProcessParam();
        String SN = TerminalManager.getInstance().getSN();
        emvStartProcessParam.mIfdSerialNum = SN.getBytes();
        emvStartProcessParam.mProcType = EmvStartProcessParam.EMV_API_PROC_PBOC_FULL;

        emvStartProcessParam.mTransType = EmvStartProcessParam.EMV_API_TRANS_TYPE_NORMAL;
        //交易计数器
        emvStartProcessParam.mSeqNo = 100;
        //交易金额,单位分
        emvStartProcessParam.mTransAmt = 1000;
        //返现金额
        emvStartProcessParam.mCashbackAmt = 0;
        emvStartProcessParam.mTransDate = StringUtil.hexStr2Bytes("20180101");
        emvStartProcessParam.mTransTime = StringUtil.hexStr2Bytes("123344");
        emvStartProcessParam.mTransCurrCode = HexDump.hexStringToByteArray("0566");
        emvStartProcessParam.mTermCountryCode = HexDump.hexStringToByteArray("0566");
        //交易类型，TAG 9C的值
        emvStartProcessParam.mTag9CValue = 0x00;
        //是否支持电子现金
        emvStartProcessParam.mIsSupportEC = true;
        //通道类型,EMV_API_CHANNEL_FROM_ICC表示接触卡 EMV_API_CHANNEL_FORM_PICC表示非接触卡
        emvStartProcessParam.mChannelType = channelType;
        //非电子现金接口，强制联机
        emvStartProcessParam.mIsQpbocForceOnline = true;
        //init first
        EmvL2.getInstance(context, context.getPackageName()).init();
        //upload aid
        String[] aids = new String[]{
                "9f0607a0000000033010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010103df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000651010df0101009f08020200df1105fc6024a800df1205fc60acf800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000032010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000031010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010106df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010101df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000043060df0101009f08020002df1105fc5058a000df1205f85058f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0608a000000333010102df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607a0000000041010df0101009f08020002df1105fc5080a000df1205f85080f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607A0000000049999df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f0607A0000000046000df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000",
                "9f660430008181"
        };
        int ret = EmvL2.getInstance(context, context.getPackageName()).clearAids();
        if (ret != 0) {
            emitter.onError(new NetposException(ICCCardProcessHandler.CLEAR_AIDS_FAILED, "Clear aid failed"));
            return;
        }

        ret = EmvL2.getInstance(context, context.getPackageName()).addAids(aids);
        if (ret != 0) {
//            searchHandler.onError(ICCCardProcessHandler.ADD_AIDS_FAILED);
//            return;
        }

        EmvL2.getInstance(context, context.getPackageName()).setDukptParam((byte) 1, Ped.DES_TYPE_3DES, (byte) 0x02, null);

        List<byte[]> bt = EmvL2.getInstance(context, context.getPackageName()).getAids();
        EmvL2.getInstance(context, context.getPackageName()).startProcess(emvStartProcessParam, new AsyncEmvCallback() {

            private byte[] pan = null;

            @Override
            public void confirmTransType(byte[] aid, int oldTransType, EmvL2.ConfirmTransTypeHandler handler) {
                handler.onConfirmTransType(oldTransType);
            }

            @Override
            public void panCofirm(byte[] pan, EmvL2.PanConfirmHandler panConfirmHandler) {
                Log.d("EMVDevice process startProcess panConfirm");
                //0: success, others: fail
                this.pan = pan;
                panConfirmHandler.onPanConfirm(0);
            }

            @Override
            public void getPin(int type, int retryTimes, EmvL2.GetPinHandler getPinHandler) {
                Log.d("EMVDevice process startProcess getPin");

            }

            @Override
            public int onVerifyOfflinePin(byte b, int i) {
                return EmvCallbackGetPinResult.CV_PIN_SUCC;
            }

            @Override
            public void aidSelect(socsi.middleware.emvl2lib.EmvAidCandidate[] emvAidCandidates, int i, EmvL2.AidSelectHandler aidSelectHandler) {
                Log.d("EMVDevice process startProcess aidSelect");
                aidSelectHandler.onAidSelect(0);
            }

            @Override
            public void termRiskManager(byte[] pan, int panSn, EmvL2.TermRiskManageHandler termRiskManageHandler) {
                Log.d("EMVDevice process startProcess termRiskManager");
                termRiskManageHandler.onTermRiskManager(0, 0);
            }

            @Override
            public void issuerReference(byte[] bytes, EmvL2.IssuerReferenceHandler issuerReferenceHandler) {
                Log.d("EMVDevice process startProcess issuerReference");
                issuerReferenceHandler.onIssuerReference(0);
            }

            @Override
            public void accountTypeSelect(EmvL2.AccountTypeSelectHandler accountTypeSelectHandler) {
                accountTypeSelectHandler.onAccountType(EmvCallback.EMV_ACCOUNT_TYPE_DEFAULT);
            }

            @Override
            public void certConfirm(int type, byte[] certNum, EmvL2.CertConfirmHandler certConfirmHandler) {
                Log.d("EMVDevice process startProcess certConfirm");
                certConfirmHandler.onCertConfirm(1);
            }

            @Override
            public void lcdMsg(byte[] bytes, byte[] bytes1, boolean b, int i, EmvL2.LcdMsgHandler lcdMsgHandler) {
                Log.d("EMVDevice process startProcess lcdMsg");
                lcdMsgHandler.onLcdMsg(1);
            }

            @Override
            public void confirmEC(EmvL2.ConfirmEcHandler confirmEcHandler) {
                Log.d("EMVDevice process startProcess confirmEC");
                confirmEcHandler.onConfirmEc(0);
            }

            @Override
            public void processResult(int ret) {
                Log.d("EMVDevice process startProcess processResult:" + ret);
                int code = 1;
                switch (ret) {
                    case EmvApi.EMV_TRANS_FALLBACK:
                        code = EMV_TRANS_FALLBACK;
                        break;
                    case EmvApi.EMV_TRANS_TERMINATE:
                        code = EMV_TRANS_TERMINATE;
                        break;
                    case EmvApi.EMV_TRANS_ACCEPT:
                        code = 0;
                        code = EMV_TRANS_ACCEPT;
                        break;
                    case EmvApi.EMV_TRANS_DENIAL:
                        code = EMV_TRANS_DENIAL;
                        return;
                    case EmvApi.EMV_TRANS_GOONLINE:
                        updateICCardData(context, EmvApi.EMV_TRANS_GOONLINE, emitter);
                        return;
                    case EmvApi.EMV_TRANS_QPBOC_ACCEPT:
                        code = EMV_TRANS_QPBOC_ACCEPT;
                        break;
                    case EmvApi.EMV_TRANS_QPBOC_DENIAL:
                        code = EMV_TRANS_QPBOC_DENIAL;
                        break;
                    case EmvApi.EMV_TRANS_QPBOC_GOONLINE:
                        code = 0;
                        updateICCardData(context, EmvApi.EMV_TRANS_QPBOC_GOONLINE, emitter);
                        return;
                    case EmvErrorCode.EMV_ERR_BASE:
                        code = EMV_ERR_BASE;
                        break;
                    case -1334:
                        Icc.getInstance().close(Icc.INIT_TYEP_RF);
                    default:
                        code = UNVERIFIED_RESULT;

                }

                emitter.onError(new NetposException(code, "An unexpected error occurred"));

            }
        });
    }

    private static void updateICCardData(Context context, int cardReadResultCode, SingleEmitter<CardReadResult> cardDataEmitter) {

        android.util.Log.d(TAG, "updateICCardData: in");
        byte[] cardData = getTlvData(context, context.getPackageName(), "5A575F345F20" + "9F269F279F109F379F36959A9C9F025F2A829F1A9F039F339F349F359F1E849F099F419F638E" +
                "917172DF32DF33DF345F249F129F53");

        if (cardData == null) {
            android.util.Log.d(TAG, "updateICCardData: error");
            cardDataEmitter.onError(new NetposException(ICCCardProcessHandler.GET_ICC_DATA_FAILED, ICCCardProcessHandler.errorMessage(ICCCardProcessHandler.GET_ICC_DATA_FAILED)));
            return;
        }
        List<Tlv> list = TlvUtils.builderTlvList(Util.BytesToString(cardData));

        cardDataEmitter.onSuccess(new CardReadResult(cardReadResultCode, list, Util.BytesToString(cardData)));

    }

}
