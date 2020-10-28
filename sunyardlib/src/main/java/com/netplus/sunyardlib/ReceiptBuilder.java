package com.netplus.sunyardlib;

import android.util.Log;

import com.netpluspay.terminal_core.AndroidTerminalReceiptBuilderFactory;
import com.socsi.smartposapi.printer.Align;
import com.socsi.smartposapi.printer.FontLattice;
import com.socsi.smartposapi.printer.FontType;
import com.socsi.smartposapi.printer.PrintRespCode;
import com.socsi.smartposapi.printer.Printer2;
import com.socsi.smartposapi.printer.TextEntity;

import io.reactivex.Single;

/**
 * ReceiptBuilder
 */
public class ReceiptBuilder extends AndroidTerminalReceiptBuilderFactory<ReceiptBuilder, Single<PrintRespCode>> {

    private final FontType mCh = FontType.SIMSUM;
    private final FontType mEn = FontType.SIMSUM;
    private final Printer2 printer;
    StringBuilder builder = new StringBuilder();
    public ReceiptBuilder() {
        this.printer = Printer2.getInstance();
    }

    @Override
    protected ReceiptBuilder getThis() {
        return this;
    }

    @Override
    public Single<PrintRespCode> print() {
        build();
        Log.e("TAG", builder.toString());
        PrintRespCode respCode = printer.startPrint();
        return Single.just(respCode);
    }

    @Override
    protected void appendTextEntity(String str) {
        builder.append(str);
        printer.appendTextEntity2(new TextEntity(str, mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT, true));
    }

    @Override
    protected void appendTextEntityBold(String str) {
        builder.append(str);
        printer.appendTextEntity2(new TextEntity(str, mCh, mEn, FontLattice.TWENTY_FOUR, true, Align.LEFT, true));
    }

    @Override
    protected void appendTextEntityFontSixteen(String str) {
        builder.append(str);
        printer.appendTextEntity2(new TextEntity(str, mCh, mEn, FontLattice.FORTY_EIGHT, true, Align.LEFT, true));
    }

    @Override
    protected void appendTextEntityFontSixteenCenter(String str) {
        builder.append(str);
        printer.appendTextEntity2(new TextEntity(str, mCh, mEn, FontLattice.FORTY_EIGHT, true, Align.CENTER, true));
    }

    @Override
    protected void appendTextEntityCenter(String str) {
        builder.append(str);
        printer.appendTextEntity2(new TextEntity(str, mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.CENTER, true));
    }
}
