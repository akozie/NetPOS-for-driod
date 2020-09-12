package com.netplus.sunyardlib;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.socsi.aidl.pinservice.OperationPinListener;
import com.socsi.smartposapi.card.CardReader;
import com.socsi.smartposapi.card.IcCardSearchCallback;
import com.socsi.smartposapi.card.MagCardSearchCallback;
import com.socsi.smartposapi.card.RfSearchCallback;
import com.socsi.smartposapi.emv2.EmvL2;
import com.socsi.smartposapi.icc.Icc;
import com.socsi.smartposapi.magcard.Magcard;
import com.socsi.smartposapi.ped.KeyBoardConstant;
import com.socsi.smartposapi.ped.Ped;
import com.socsi.smartposapi.systemupdate.util.CommonUtils;
import com.socsi.smartposapi.terminal.TerminalManager;
import com.socsi.utils.DateUtil;
import com.socsi.utils.HexUtil;
import com.socsi.utils.TlvUtil;
import com.socsi.utils.Utils;
import com.socsi.utils.log4j.Priority;
import com.socsi.smartposapi.emv2.AsyncEmvCallback;
import com.sunyard.i80.util.EncryptKey;
import com.sunyard.i80.util.StringUtil;
import com.sunyard.i80.util.TlvUtils;
import com.sunyard.i80.util.Util;
import com.sunyard.smartposapi.emv2.TwiceAuthorResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import socsi.middleware.emvl2lib.EmvAidCandidate;
import socsi.middleware.emvl2lib.EmvStartProcessParam;

public class CardReaderService2 {

    public static Observable<CardReaderEvent> readCard(Activity context) {
        return Observable.create(emitter -> searchCard(context, emitter));
    }

    private static void searchCard(Activity context, ObservableEmitter<CardReaderEvent> emitter) {
        try {
            CardReader cardReader = CardReader.getInstance();
            cardReader.setMagCardSearchCallback(ret -> {
                switch (ret) {
                    case 0:
                        updateResult(Magcard.getInstance().readUnEncryptTrack().toString());
                        break;
                    case 1:
                        updateResult("read timeout");
                        break;
                    default:
                        updateResult("read fail:" + ret);
                        break;
                }
            });
            cardReader.setIcCardSearchCallback(ret -> {
                switch (ret) {
                    case 0:
                        startProcess(0, context, emitter);
                        return;
                    case 1:
                        updateResult("read timeout");
                        break;
                    case 2:
                        updateResult("read fail: unknown card type");
                        break;
                    default:
                        updateResult("read fail:" + ret);
                        break;
                }
            });
            cardReader.setRfSearchCallback((ret, cardType, SerialNumber, ATQA) -> {
                if (ret == 0) {
                    startProcess(1, context, emitter);
                    return;
                }
                if (ret == 1) {
                    updateResult("read timeout");
                } else {
                    updateResult("read fail:" + ret);
                }
            });
            cardReader.searchCard((int) (byte) (((byte) (((byte) (0 | 1)) | 2)) | 4), (int) Priority.INFO_INT);
        } catch (Exception e) {
            e.printStackTrace();
            stopSearch(emitter);
            updateResult("search card fail");
        }
    }


    private static void startProcess(int channelType, Activity context, ObservableEmitter<CardReaderEvent> emitter) {
        emitter.onNext(new CardReaderEvent.CardDetected());
        int i = channelType;
        EmvL2.getInstance(context, context.getPackageName()).resetProcess();
        //EmvL2.getInstance(context,context.getPackageName()).addCapk();
        EmvStartProcessParam emvStartProcessParam = new EmvStartProcessParam();
        emvStartProcessParam.mIfdSerialNum = TerminalManager.getInstance().getSN().getBytes();
        if (i == 1) {
            emvStartProcessParam.mProcType = 2;
        } else {
            emvStartProcessParam.mProcType = 0;
        }
        emvStartProcessParam.mTransType = 0;
        emvStartProcessParam.mSeqNo = 100;
        emvStartProcessParam.mTransAmt = 1000;
        emvStartProcessParam.mCashbackAmt = 0;
        emvStartProcessParam.mTransDate = StringUtil.hexStr2Bytes("20180101");
        emvStartProcessParam.mTransTime = StringUtil.hexStr2Bytes("123344");
        emvStartProcessParam.mTag9CValue = 0;
        emvStartProcessParam.mIsSupportEC = true;
        emvStartProcessParam.mChannelType = i;
        emvStartProcessParam.mIsQpbocForceOnline = true;
        EmvL2.getInstance(context, context.getPackageName()).init();
        String[] aids = {"9f0607a0000000033010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0608a000000333010103df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0607a0000000651010df0101009f08020200df1105fc6024a800df1205fc60acf800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0607a0000000032010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0607a0000000031010df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0608a000000333010106df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0608a000000333010101df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0607a0000000043060df0101009f08020002df1105fc5058a000df1205f85058f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0608a000000333010102df0101009f08020020df1105d84000a800df1205d84004f800df130500100000009f1b0400000001df150400000000df160199df170199df14039f3704df1801019f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0607a0000000041010df0101009f08020002df1105fc5080a000df1205f85080f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0607A0000000049999df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f0607A0000000046000df0101009f08020140df1105d84000a800df1205d84004f800df130500100000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000100000df2006000000100000df2106000000100000", "9f660430008181"};
        if (EmvL2.getInstance(context, context.getPackageName()).clearAids() != 0) {
            com.socsi.utils.Log.e("clear aids fail");
        }
        if (EmvL2.getInstance(context, context.getPackageName()).addAids(aids) != 0) {
            com.socsi.utils.Log.e("add aids fail");
        }
        EmvL2.getInstance(context, context.getPackageName()).setDukptParam((byte) 1, (byte) 1, (byte) 2, (byte[]) null);
        List<byte[]> aids2 = EmvL2.getInstance(context, context.getPackageName()).getAids();
        EmvL2.getInstance(context, context.getPackageName()).startProcess(emvStartProcessParam, (AsyncEmvCallback) new AsyncEmvCallback() {
            @Override
            public void confirmTransType(byte[] aid, int oldTransType, EmvL2.ConfirmTransTypeHandler handler) {
                handler.onConfirmTransType(oldTransType);
            }

            @Override
            public void panCofirm(byte[] pan, EmvL2.PanConfirmHandler panConfirmHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess panCofirm");
                panConfirmHandler.onPanConfirm(0);
            }

            @Override
            public void getPin(int type, int retryTimes, EmvL2.GetPinHandler getPinHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess getPin");
                if (type != 3 && type != 4) {
                    if (type != 1 && type != 2) {
                        com.socsi.utils.Log.d("EMVDevice SyncEmvCallback getPin type error:" + type);
                    }
                }
                getPinHandler.onGetPin(0, new byte[]{0, 0});
            }

            @Override
            public int onVerifyOfflinePin(byte b, int i) {
                return 0;
            }

            public void aidSelect(EmvAidCandidate[] emvAidCandidates, int times, EmvL2.AidSelectHandler aidSelectHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess aidSelect");
                aidSelectHandler.onAidSelect(0);
            }

            public void termRiskManager(byte[] pan, int panSn, EmvL2.TermRiskManageHandler termRiskManageHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess termRiskManager");
                termRiskManageHandler.onTermRiskManager(0, 0);
            }

            public void issuerReference(byte[] bytes, EmvL2.IssuerReferenceHandler issuerReferenceHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess issuerReference");
                issuerReferenceHandler.onIssuerReference(0);
            }

            public void accountTypeSelect(EmvL2.AccountTypeSelectHandler accountTypeSelectHandler) {
                accountTypeSelectHandler.onAccountType(1);
            }

            public void certConfirm(int type, byte[] certNum, EmvL2.CertConfirmHandler certConfirmHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess certConfirm");
                certConfirmHandler.onCertConfirm(1);
            }

            public void lcdMsg(byte[] bytes, byte[] bytes1, boolean b, int i, EmvL2.LcdMsgHandler lcdMsgHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess lcdMsg");
                lcdMsgHandler.onLcdMsg(1);
            }

            public void confirmEC(EmvL2.ConfirmEcHandler confirmEcHandler) {
                com.socsi.utils.Log.d("EMVDevice process startProcess confirmEC");
                confirmEcHandler.onConfirmEc(-1);
            }

            public void processResult(int ret) {
                com.socsi.utils.Log.d("EMVDevice process startProcess processResult:" + ret);
                int code = 1;
                String desc = "";
                if (ret != -1334) {
                    if (ret != -14) {
                        switch (ret) {
                            case -1:
                                desc = "EMV_TRANS_TERMINATE";
                                break;
                            case 0:
                                desc = "EMV_ERR_BASE";
                                break;
                            case 1:
                                code = 0;
                                desc = "EMV_TRANS_ACCEPT";
                                break;
                            case 2:
                                desc = "EMV_TRANS_DENIAL";
                                break;
                            case 3:
                                updateICCardData(context, 3, emitter);
                                return;
                            default:
                                switch (ret) {
                                    case 13:
                                        code = 0;
                                        desc = "EMV_TRANS_QPBOC_ACCEPT";
                                        break;
                                    case 14:
                                        desc = "EMV_TRANS_QPBOC_DENIAL";
                                        break;
                                    case 15:
                                        updateICCardData(context, 15, emitter);
                                        return;
                                }
                        }
                    } else {
                        desc = "EMV_TRANS_FALLBACK";
                    }
                    updateResult("code:" + code + ",desc:" + desc);
                    return;
                }
                Icc.getInstance().close((byte) 4);
                //doMasterRf(context);
            }
        });
    }

    private static void doMasterRf(Activity context) {
        final com.sunyard.smartposapi.emv2.EmvL2 emvL2 = com.sunyard.smartposapi.emv2.EmvL2.getInstance(context, context.getPackageName());
        com.sunyard.middleware.emvl2lib.EmvStartProcessParam param = new com.sunyard.middleware.emvl2lib.EmvStartProcessParam();
        param.mTransAmt = 2;
        param.mTransDate = StringUtil.hexStr2Bytes(DateUtil.getCurDateStr("yyMMdd"));
        param.mTransTime = StringUtil.hexStr2Bytes(DateUtil.getCurDateStr(CommonUtils.HHMMSS));
        param.mChannelType = 1;
        param.mTag9CValue = 0;
        param.mProcType = 0;
        emvL2.setDukptParam((byte) 1, (byte) 1, (byte) 2, (byte[]) null);
        emvL2.startProcess(param, (com.sunyard.smartposapi.emv2.AsyncEmvCallback) new com.sunyard.smartposapi.emv2.AsyncEmvCallback() {

            public void panConfirm(final byte[] pan, final com.sunyard.smartposapi.emv2.EmvL2.PanConfirmHandler handler) {
                updateResult("[panConfirm]pan:" + new String(pan));
                context.runOnUiThread(new Runnable() {
                    public void run() {
                        new MaterialDialog.Builder(context).title((CharSequence) "Pan Confirm").content((CharSequence) new String(pan)).positiveText((CharSequence) "confirm").onPositive(new MaterialDialog.SingleButtonCallback() {
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                handler.onPanConfirm(0);
                            }
                        }).negativeText((CharSequence) "cancel").onNegative(new MaterialDialog.SingleButtonCallback() {
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                handler.onPanConfirm(1);
                            }
                        }).show();
                    }
                });
            }

            public void getPin(int type, int retryTimes, final com.sunyard.smartposapi.emv2.EmvL2.GetPinHandler handler) {
                updateResult("[getPin]type:" + type + ", retryTimes:" + retryTimes);
                new Thread(new Runnable() {
                    public void run() {
                        Bundle param = new Bundle();
                        param.putBoolean("isOnline", true);
                        param.putString("pan", EncryptKey.pan);
                        param.putString("promptString", "请输入银行卡密码");
                        param.putIntArray("pinLimit", new int[]{0, 6});
                        param.putInt("pinAlgMode", 0);
                        param.putInt(KeyBoardConstant.BUNDLE_KEY_KEYSTYPE, 0);
                        param.putInt("timeout", 60);
                        Ped.getInstance().startPinInput(context, 1, param, new OperationPinListener() {
                            public void onInput(int len, int key) {
                                com.socsi.utils.Log.e("Mainactivity", "onInput  len:" + len + "  key:" + key);
                            }

                            public void onError(int errorCode) {
                                com.socsi.utils.Log.e("Mainactivity", "onError   errorCode:" + errorCode);
                            }

                            public void onConfirm(final byte[] data, boolean isNonePin) {
                                com.socsi.utils.Log.e("Mainactivity", "onConfirm   data:" + HexUtil.toString(data) + "  isNonePin:" + isNonePin);
                                if (!isNonePin) {
                                    updateResult("密码:" + HexUtil.toString(data));
                                }
                                handler.onGetPin(0, data);
                            }

                            public void onCancel() {
                                com.socsi.utils.Log.e("Mainactivity", "onCancel");
                            }
                        });
                    }
                }).start();
            }

            public void aidSelect(String[] aidNames, final com.sunyard.smartposapi.emv2.EmvL2.AidSelectHandler handler) {
                String data = "";
                for (int i = 0; i < aidNames.length; i++) {
                    data = data + i + ":" + aidNames[i] + "\n";
                }
                updateResult("[aidSelect]aidNames:" + data);
                final String title = data;
                context.runOnUiThread(new Runnable() {
                    public void run() {
                        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
                        builder.title((CharSequence) "please select follow aids\n" + title).inputType(2).input((CharSequence) "", (CharSequence) "", (MaterialDialog.InputCallback) new MaterialDialog.InputCallback() {
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                handler.onAidSelect(Integer.parseInt(input.toString()));
                            }
                        }).negativeText((CharSequence) "cancel").onNegative(new MaterialDialog.SingleButtonCallback() {
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                handler.onAidSelect(-1);
                            }
                        }).show();
                    }
                });
            }

            public void termRiskManager(byte[] pan, int panSn, com.sunyard.smartposapi.emv2.EmvL2.TermRiskManageHandler handler) {
                updateResult("[termRiskManager]pan:" + new String(pan) + ", panSn:" + panSn);
                handler.onTermRiskManager(0, 0);
            }

            public void issuerReference(byte[] pan, com.sunyard.smartposapi.emv2.EmvL2.IssuerReferenceHandler handler) {
                updateResult("[issuerReference]pan:" + new String(pan));
                handler.onIssuerReference(0);
            }

            public void accountTypeSelect(com.sunyard.smartposapi.emv2.EmvL2.AccountTypeSelectHandler handler) {
                updateResult("[accountTypeSelect]");
                handler.onAccountType(1);
            }

            public void certConfirm(byte[] type, byte[] certNum, com.sunyard.smartposapi.emv2.EmvL2.CertConfirmHandler handler) {
                updateResult("[certConfirm]type:" + new String(type) + ", certNum:" + new String(certNum));
                handler.onCertConfirm(1);
            }

            public void lcdMsg(byte[] title, byte[] msg, boolean isYesNo, int timeout, com.sunyard.smartposapi.emv2.EmvL2.LcdMsgHandler handler) {
                updateResult("[lcdMsg]title:" + new String(title) + ", msg:" + new String(msg) + ", isYesNo:" + isYesNo + ", timeout:" + timeout);
                handler.onLcdMsg(1);
            }

            public void confirmEC(com.sunyard.smartposapi.emv2.EmvL2.ConfirmEcHandler handler) {
                updateResult("[accountTypeSelect]");
                handler.onConfirmEc(0);
            }

            public void processResult(int ret) {
                appendResult("[processResult]ret:" + ret);
                byte[] tag = StringUtil.hexStr2Bytes("579F269F279F109F379F36959A9F219C9F025F2A829F1A9F039F339F349F359F1E9F06849F099F419F535F345A575F249F079B9F119F125F20995F255F30509F4000");
                if (ret == 0 || ret == 1 || ret == 2) {
                    byte[] tlvData = emvL2.getTLVData(tag);
                    if (tlvData != null) {
                        updateResult("[getTLVData]" + StringUtil.byte2HexStr(tlvData));
                        com.socsi.utils.Log.d("WY", "[getTLVData]" + StringUtil.byte2HexStr(tlvData));
                    } else {
                        updateResult("[getTLVData]fail");
                    }
                } else if (ret == 3) {
                    tag = StringUtil.hexStr2Bytes("509F12849F119F6D9F24569F6B9F5DDF812ADF812BDF81159F6E");
                    byte[] tlvData2 = emvL2.getTLVData(tag);
                    if (tlvData2 != null) {
                        updateResult("[getTLVData]" + StringUtil.byte2HexStr(tlvData2));
                    } else {
                        updateResult("[getTLVData]fail");
                    }
                }
                if (ret == 1) {
                    try {
                        TwiceAuthorResult twiceAuthorResult = emvL2.twiceAuthorization("00", "720F860D84240000087B2C5A82C7FCAE5D71709F180441424344860D841E00000853A2C503898A7E9B860D841E000008386176C2CDDE4597860D841E000008225457FD8A602EE0860D841E00000860C17F2A97BD65E7860D841E0000085CE4991432364517860D841E000008A7676474474DD431860D841E0000082203E10993A13927");
                        updateResult("[TwiceAuthorResult]" + twiceAuthorResult.getStatus());
                        if ("00".equals(twiceAuthorResult.getStatus())) {
                            updateResult("[TwiceAuthorResult]success");
                            byte[] tlvData3 = emvL2.getTLVData(tag);
                            if (tlvData3 != null) {
                                updateResult("[getTLVData]" + StringUtil.byte2HexStr(tlvData3));
                                return;
                            }
                            updateResult("[getTLVData]fail");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        updateResult("twiceAuthorization Exception");
                    }
                }
            }
        });
    }

    private static void updateICCardData(Activity context, int cardReadResultCode, ObservableEmitter<CardReaderEvent> emitter) {
        byte[] cardData = getTlvData(context, context.getPackageName(), "5A575F345F20" + "9F269F279F109F379F36959A9C9F025F2A829F1A9F039F339F349F359F1E849F099F419F638E" +
                "917172DF32DF33DF345F249F129F53");
        if (cardData == null) {
            updateResult("get ic data fail");
            emitter.onError(new NetposException(ICCCardProcessHandler.GET_ICC_DATA_FAILED, ICCCardProcessHandler.errorMessage(ICCCardProcessHandler.GET_ICC_DATA_FAILED)));
            return;
        }
        Map<String, String> cardDataMap = TlvUtil.tlvToMap(cardData);
        String pan = cardDataMap.get("5A");
        if (!StringUtil.isEmpty(pan)) {
            pan = pan.replace("F", "");
        }
        String result = "card no: " + pan + "\ncardHolderName:" + cardDataMap.get("5F20") + "\ntrack 2:" + cardDataMap.get("57") + "\ntlv data: ";
        List<Tlv> tlvList = TlvUtils.builderTlvList(Util.BytesToString(getTlvData(context, context.getPackageName(), Util.BytesToString(cardData))));
        if (tlvList != null) {
            for (Tlv tlv : tlvList) {
                result = (result + "\ntag:" + tlv.getTag()) + " value:" + tlv.getValue();
            }
        }

        List<Tlv> list = TlvUtils.builderTlvList(Util.BytesToString(cardData));
        emitter.onNext(new CardReaderEvent.CardRead(new CardReadResult(cardReadResultCode, list, Util.BytesToString(cardData))));
        emitter.onComplete();
        //updateResult(result);
    }

    private static byte[] getTlvData(Activity context, String packageName, String tlvString) {
        try {
            Map<String, String> mMap = new HashMap<>();
            mMap.put("DF35", tlvString);
            return com.socsi.smartposapi.emv2.EmvL2.getInstance(context, packageName).getTLVData(TlvUtil.mapToTlv(mMap));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static void stopSearch(ObservableEmitter<CardReaderEvent> emitter) {
        CardReader.getInstance().stopSearch(7);
    }

    private static void updateResult(String message) {
        Log.e("TAG-UPDATE", message);
    }

    private static void appendResult(final String result) {
        Log.e("TAG-APPEND", result);
    }
}

