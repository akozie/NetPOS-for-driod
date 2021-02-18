package com.woleapp.netpos.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;


//,indices = {@Index(value = {"user_id"})}
public class Biller {

    public Biller(@NonNull Integer id, String service_type, String biller_name, String biller_code, String operation, String status, String verification_status, String imageUrl) {
        this.id = id;
        this.service_type = service_type;
        this.biller_name = biller_name;
        this.biller_code = biller_code;
        this.operation = operation;
        this.status = status;
        this.verification_status = verification_status;
        this.imageUrl = imageUrl;
    }

    public Biller(@NonNull Integer id, String service_type, String biller_name, String biller_code, String operation, String status, String verification_status) {
        this.id = id;
        this.service_type = service_type;
        this.biller_name = biller_name;
        this.biller_code = biller_code;
        this.operation = operation;
        this.status = status;
        this.verification_status = verification_status;
    }

    @NonNull
    @SerializedName(value = "id")
    @Expose
    private Integer id;

    @SerializedName("service_type")
    @Expose
    private String service_type;

    @SerializedName("biller_name")
    @Expose
    private String biller_name;

    @SerializedName("biller_code")
    @Expose
    private String biller_code;

    @SerializedName("operation")
    @Expose
    private String operation;

    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("verification_status")
    @Expose
    private String verification_status;
    @Ignore
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    public Integer getId() {
        return id;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    public String getService_type() {
        return service_type;
    }

    public void setService_type(String service_type) {
        this.service_type = service_type;
    }

    public String getBiller_name() {
        return biller_name;
    }

    public void setBiller_name(String biller_name) {
        this.biller_name = biller_name;
    }

    public String getBiller_code() {
        return biller_code;
    }

    public void setBiller_code(String biller_code) {
        this.biller_code = biller_code;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerification_status() {
        return verification_status;
    }

    public void setVerification_status(String verification_status) {
        this.verification_status = verification_status;
    }


    public static class ElectricityBillers {
        public String billerName;
        public String prepaidCode;
        public String postpaidCode;
        public String imageUrl;

        public ElectricityBillers(String billerName, String prepaidCode, String postpaidCode, String imageUrl) {
            this.billerName = billerName;
            this.prepaidCode = prepaidCode;
            this.postpaidCode = postpaidCode;
            this.imageUrl = imageUrl;
        }
    }

    public static class BillerPlans {
        @SerializedName("SPECTRANET DATA PLANS")
        public List<SpectranetInternet> spectranetInternetList;
        @SerializedName("SMILE DATA PLANS")
        public List<SmileInternet> smileInternetList;
        @SerializedName("MULTICHOICE SUBSCRIPTION PLANS")
        public List<MultichoicePlan> multichoicePlans;
        @SerializedName("STARTIMES SUBSCRIPTION PLANS")
        public List<StartTimesPlan> startTimesPlans;
    }


    public static class SmileInternet {
        @SerializedName("Unit Credit ID")
        public int unitCreditId;
        @SerializedName("Bundle Name")
        public String bundleName;
        @SerializedName("Validity")
        public String validity;
        @SerializedName("Price")
        public String price;
        @SerializedName("Volume")
        public String volume;
    }


    public static class SpectranetInternet {
        @SerializedName("Plan Name")
        public String planName;
        @SerializedName("Benefit Details")
        public String benefitDetails;
        @SerializedName("Customer Price (New)")
        public String price;
    }


    public static class MultichoicePlan {
        @SerializedName("Products")
        public String product;
        @SerializedName("Product Code")
        public String productCode;
        @SerializedName("Current Price")
        public int currentPrice;
        @SerializedName("New Price")
        public int newPrice;
    }


    public static class StartTimesPlan {
        @SerializedName("PLANS")
        public String plan;
        @SerializedName("Daily")
        public int dailyFee;
        @SerializedName("Weekly")
        public int weeklyFee;
        @SerializedName("Monthly")
        public int monthlyFee;
    }

    public static class ServiceProviderDataPlans {
        @SerializedName("9Mobile")
        public List<DataPlan> etisalatPlans;
        @SerializedName("AIRTEL")
        public List<DataPlan> airtelPlans;
        @SerializedName("GLO")
        public List<DataPlan> gloPlans;
        @SerializedName("MTN")
        public List<DataPlan> mtnPlans;
    }

    public static class DataPlan {
        @SerializedName("Price(n)")
        public String price;
        @SerializedName("Duration")
        public String duration;
        @SerializedName("Data")
        public String data;
    }
}
