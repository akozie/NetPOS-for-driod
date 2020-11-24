package com.netpluspay.terminalcore;

/**
 * @param <T> The ReceiptBuilder class which extends this class
 * @param <K> The return value of the final print function
 */
public abstract class AndroidTerminalReceiptBuilderFactory<T, K> {
    protected String title;
    protected String merchantName;
    protected String terminalId;
    protected String transactionType;
    protected String stan;
    protected String dateTime;
    protected String amount;
    protected String cardScheme;
    protected String cardNumber;
    protected String cardHolderName;
    protected String authorizationCode;
    protected String transactionStatus;
    protected String responseCode;
    protected String aid;
    protected String rrn;
    protected String appName;
    protected String appVersion;
    protected String receiptCopy;

    abstract protected T getThis();

    public T appendTitle(String title) {
        this.title = title;
        return getThis();
    }

    public T appendAddress(String merchantName) {
        this.merchantName = merchantName;
        return getThis();
    }

    public T appendTerminalId(String terminalId) {
        this.terminalId = terminalId;
        return getThis();
    }

    public T appendTransactionType(String transactionType) {
        this.transactionType = transactionType;
        return getThis();
    }

    public T appendStan(String stan) {
        this.stan = stan;
        return getThis();
    }

    public T appendDateTime(String dateTime) {
        this.dateTime = dateTime;
        return getThis();
    }

    public T appendAmount(String amount) {
        this.amount = amount;
        return getThis();
    }

    public T appendCardScheme(String cardScheme) {
        this.cardScheme = cardScheme;
        return getThis();
    }

    public T appendCardNumber(String number) {
        this.cardNumber = number;
        return getThis();
    }

    public T appendCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
        return getThis();
    }

    public T appendAuthorizationCode(String authCode) {
        this.authorizationCode = authCode;
        return getThis();
    }

    public T appendTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
        return getThis();
    }

    public T appendResponseCode(String responseCode) {
        this.responseCode = responseCode;
        return getThis();
    }

    public T appendAID(String aid) {
        this.aid = aid;
        return getThis();
    }

    public T appendRRN(String rrn) {
        this.rrn = rrn;
        return getThis();
    }

    public T appendAppName(String appName) {
        this.appName = appName;
        return getThis();
    }

    public T appendAppVersion(String appVersion) {
        this.appVersion = appVersion;
        return getThis();
    }

    public T isCustomerCopy() {
        this.receiptCopy = "**** CUSTOMER COPY ****";
        return getThis();
    }

    public T isMerchantCopy() {
        this.receiptCopy = "**** MERCHANT COPY ****";
        return getThis();
    }

    public abstract void appendLogo();

    protected void printLine() {
        appendTextEntity("\n");
        appendTextEntity("-------------------------------");
        appendTextEntity("\n");
    }

    protected void build() {
        if (title != null)
            appendTextEntity(title);

        if (merchantName != null)
            appendTextEntity(merchantName);

        if (terminalId != null) {
            appendTextEntity("TERMINAL ID: " + terminalId);
            appendTextEntity("\n");
        }

        if (transactionType != null) {
            appendTextEntityFontSixteenCenter(transactionType);
            appendTextEntity("\n");
        }

        if (stan != null)
            appendTextEntityBold("STAN: " + stan);

        if (dateTime != null) {
            appendTextEntity("DATE/TIME: " + dateTime);
            //appendTextEntity("\n");
        }

        if (amount != null) {
            appendTextEntityBold("AMOUNT: " + amount);
            //appendTextEntity("\n");
        }

        if (rrn != null) {
            appendTextEntity("TRANS REF: " + rrn);
            appendTextEntity("\n");
        }

        if (cardScheme != null)
            appendTextEntity(cardScheme);

        if (cardNumber != null) {
            appendTextEntity(cardNumber);
            //appendTextEntity("\n");
        }

        if (cardHolderName != null) {
            appendTextEntity(cardHolderName);
            appendTextEntity("EXPIRY DATE: **/**");
        }

        if (authorizationCode != null) {
            appendTextEntity("AUTHORIZATION CODE: " + authorizationCode);
            appendTextEntity("\n");
        }

        if (transactionStatus != null) {
            appendTextEntityFontSixteenCenter(transactionStatus);
            appendTextEntity("\n");
        }

        if (responseCode != null)
            appendTextEntity("RESPONSE CODE: " + responseCode);

        if (aid != null)
            appendTextEntity("AID: " + aid);

        if (rrn != null)
            appendTextEntity("RRN: " + rrn);

        if (appName != null && appVersion != null) {
            appendTextEntity(appName + " " + appVersion);
            appendTextEntity("\n");
        }

        if (receiptCopy != null) {
            appendTextEntity("Powered by NetPlus");
            appendTextEntityCenter(receiptCopy);
        }
        printLine();
    }

    public abstract void appendTextEntity(String str);

    public abstract void appendTextEntityBold(String str);

    public abstract void appendTextEntityFontSixteen(String str);

    public abstract void appendTextEntityFontSixteenCenter(String str);

    public abstract void appendTextEntityCenter(String str);

    public abstract void appendImageCenter();

    public abstract K print();
}
