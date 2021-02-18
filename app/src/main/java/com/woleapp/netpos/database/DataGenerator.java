/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.woleapp.netpos.database;


import android.content.Context;

import com.google.gson.Gson;
import com.woleapp.netpos.model.Biller;

import java.util.ArrayList;
import java.util.List;

import static com.woleapp.netpos.util.ExtensionFunctionsKt.getPlansJson;
import static com.woleapp.netpos.util.ExtensionFunctionsKt.getServiceProviderPlansJson;

/**
 * Generates data to pre-populate the database
 */
public class DataGenerator {

    /*private static final String[] FIRST = new String[]{
            "Special edition", "New", "Cheap", "Quality", "Used"};
    private static final String[] SECOND = new String[]{
            "Three-headed Monkey", "Rubber Chicken", "Pint of Grog", "Monocle"};
    private static final String[] DESCRIPTION = new String[]{
            "is finally here", "is recommended by Stan S. Stanman",
            "is the best sold product on Mêlée Island", "is \uD83D\uDCAF", "is ❤️", "is fine"};
    private static final String[] COMMENTS = new String[]{
            "Comment 1", "Comment 2", "Comment 3", "Comment 4", "Comment 5", "Comment 6"};*/

    public static List<Biller> generateBillers() {
        List<Biller> billerList = new ArrayList<>();
        Biller biller = new Biller(1, "Airtime", "MTN", "MTN", "VTU", "active", "Not Active", "https://seeklogo.com/images/M/MTN-logo-459AAF9482-seeklogo.com.png");
        billerList.add(biller);
        biller = new Biller(2, "Airtime", "GLO", "GLO", "VTU", "active", "Not Active", "https://upload.wikimedia.org/wikipedia/commons/8/86/Glo_button.png");
        billerList.add(biller);
        biller = new Biller(3, "Airtime", "ETISALAT", "ETISALAT", "VTU", "active", "Not Active", "https://live.mrf.io/statics/i/ps/cdn.punchng.com/wp-content/uploads/2017/07/19170207/9Mobile-Telecom-Logo.jpg");
        billerList.add(biller);
        biller = new Biller(4, "Airtime", "AIRTEL", "AIRTEL", "VTU", "active", "Not Active", "https://africa.airtel.com/assets/images/airtel-logo/airtel-logo-white-text-vertical.jpg");
        billerList.add(biller);
        biller = new Biller(5, "Airtime Prepaid", "AIRTEL", "AIRTEL", "VTU", "active", "Not Active", "https://africa.airtel.com/assets/images/airtel-logo/airtel-logo-white-text-vertical.jpg");
        billerList.add(biller);
        biller = new Biller(6, "Airtime Visafone", "VISAFONE", "VISAFONE", "VTU", "active", "Not Active", "https://i0.wp.com/mobilityarena.com/wp-content/uploads/2013/02/visafone-Logo.gif?fit=400%2C323&ssl=1");
        billerList.add(biller);
        biller = new Biller(7, "Electricity", "PHCN EKO", "PHCNEKO", "PayBill Prepaid", "active", "active", "https://3.bp.blogspot.com/-wQIFTaqFde8/W-2xq9tgDiI/AAAAAAAAAA0/wV3LuI1W2K0n63jystncE1IHaaTCj-8xQCK4BGAYYCw/s400/Eko%2BElectricity%2BDistribution%2BCompany.jpg");
        billerList.add(biller);
        biller = new Biller(8, "Electricity", "PHCN IKEJA", "PHCNIKJ", "PayBill Prepaid", "active", "active", "https://pbs.twimg.com/profile_images/661968066729738240/mKmGmNm0_400x400.jpg");
        billerList.add(biller);
        biller = new Biller(9, "Electricity", "PHCN KANO", "PHCNKAN", "PayBill Prepaid", "active", "active", "https://www.kedco.ng/assets/images/Kedco%20Logo%20web.png");
        billerList.add(biller);
        biller = new Biller(10, "Electricity", "PHCN KADUNA", "PHCNKAD", "PayBill Prepaid", "active", "Pending", "https://gadopress.com/wp-content/uploads/2020/01/Kaedco.jpg");
        billerList.add(biller);
        biller = new Biller(11, "Electricity", "PHCN JOS", "PHCNJOS", "PayBill Prepaid", "active", "active", "https://pbs.twimg.com/profile_images/898118928404295681/qqSdt2Ir_400x400.jpg");
        billerList.add(biller);
        biller = new Biller(12, "Electricity", "PHCN EKO", "PHCNPPEKO", "PayBill Postpaid", "pending", "Not Active", "https://3.bp.blogspot.com/-wQIFTaqFde8/W-2xq9tgDiI/AAAAAAAAAA0/wV3LuI1W2K0n63jystncE1IHaaTCj-8xQCK4BGAYYCw/s400/Eko%2BElectricity%2BDistribution%2BCompany.jpg");
        billerList.add(biller);
        biller = new Biller(13, "Electricity", "PHCN IKEJA", "PHCNPPIKJ", "PayBill Postpaid", "pending", "Not Active", "https://pbs.twimg.com/profile_images/661968066729738240/mKmGmNm0_400x400.jpg");
        billerList.add(biller);
        biller = new Biller(14, "Electricity", "PHCN KANO", "PHCNPPKAN", "PayBill Postpaid", "pending", "Not Active", "https://www.kedco.ng/assets/images/Kedco%20Logo%20web.png");
        billerList.add(biller);
        biller = new Biller(15, "Electricity", "PHCN KADUNA", "PHCNPPKAD", "PayBill Postpaid", "Pending", "Pending", "https://gadopress.com/wp-content/uploads/2020/01/Kaedco.jpg");
        billerList.add(biller);
        biller = new Biller(16, "Electricity", "PHCN JOS", "PHCNPPJOS", "PayBill Postpaid", "pending", "Not Active", "https://pbs.twimg.com/profile_images/898118928404295681/qqSdt2Ir_400x400.jpg");
        billerList.add(biller);
        biller = new Biller(17, "TV", "DSTV", "DSTV", "PayBill Prepaid", "active", "Pending", "https://i.pinimg.com/originals/b3/0a/86/b30a863d516e4acc035696d103b03cf3.jpg");
        billerList.add(biller);
        biller = new Biller(18, "TV", "TRENDTV", "TRENDTV", "PayBill Prepaid", "pending", "Pending", "");
        billerList.add(biller);
        biller = new Biller(19, "TV", "GOTV", "GOTV", "PayBill Prepaid", "active", "Pending", "https://dailyfamily.ng/wp-content/uploads/2017/09/GOtv-Logo.jpg");
        billerList.add(biller);
        biller = new Biller(20, "TV", "COWA TV", "COWATV", "PayBill Prepaid", "Pending", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(21, "TV", "STARTIMES", "STARTIMES", "PayBill Prepaid", "active", "Not Applicable", "https://technext.ng/wp-content/uploads/2017/10/TSTv-Fever-Startimes-Introduces-PayPerDay-option-To-Stay-Ahead-of-Competition-1024x665-400x200.jpg");
        billerList.add(biller);
        biller = new Biller(22, "Internet", "SWIFT", "SWIFT", "PayBill Postpaid", "Not Applicable", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(23, "Internet", "SMILE", "SMILE", "PayBill Postpaid", "Not Applicable", "Not Applicable", "https://i2.wp.com/bizwatchnigeria.ng/wp-content/uploads/2018/03/smile-nigeria.png?fit=680%2C365&ssl=1&resize=1280%2C720");
        billerList.add(biller);
        biller = new Biller(24, "Internet", "SWIFT", "SWIFT", "PayBill Prepaid", "pending", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(25, "Internet", "SMILE", "SMILE", "PayBill Prepaid", "active", "Not Applicable", "https://i2.wp.com/bizwatchnigeria.ng/wp-content/uploads/2018/03/smile-nigeria.png?fit=680%2C365&ssl=1&resize=1280%2C720");
        billerList.add(biller);
        biller = new Biller(26, "Internet", "SPECTRANET", "SPECTRANET", "PayBill Prepaid", "active", "Pending", "https://techafriqa.com/wp-content/uploads/2020/02/Spectranet-Data-Plans.png");
        billerList.add(biller);
        biller = new Biller(27, "Church (donation)", "Sam Amaga Ministries", "SAMAMAGA", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(28, "Church (donation)", "Dunamis Worldwide", "Dunamis", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(29, "Church (donation)", "Mountain of Fire and Miracles Ministries", "MFM", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(30, "Church (donation)", "VICTORY CHAPEL", "VICTORYCHAPEL", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(31, "Church (donation)", "Living Faith Church Worldwide International", "Winners", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(32, "Church (donation)", "SALVATION MINISTRIES", "SAMIN", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(33, "Church (donation)", "CHRIST EMBASSY", "CHRISTEMB", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(34, "Church (donation)", "HIGH LIFE CHURCH", "HLC", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(35, "Church (donation)", "HOUSE ON THE ROCK", "HOUSEONTHEROCK", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        biller = new Biller(36, "Church (donation)", "The Redeemed Evangelical Mission", "TREM", "PayBill Prepaid", "active", "Not Applicable", "");
        billerList.add(biller);
        return billerList;
    }

    public static Biller.BillerPlans getBillerPlans(Context context) {
        String plans = getPlansJson(context);
        return new Gson().fromJson(plans, Biller.BillerPlans.class);
    }

    public static List<Biller.ElectricityBillers> electricityBillersList() {
        List<Biller.ElectricityBillers> billerList = new ArrayList<>();
        billerList.add(new Biller.ElectricityBillers("PHCN EKO", "PHCNEKO", "PHCNPPEKO", "https://3.bp.blogspot.com/-wQIFTaqFde8/W-2xq9tgDiI/AAAAAAAAAA0/wV3LuI1W2K0n63jystncE1IHaaTCj-8xQCK4BGAYYCw/s400/Eko%2BElectricity%2BDistribution%2BCompany.jpg"));
        //billerList.add(new Biller.ElectricityBillers("PHCN IKEJA", "PHCNIKJ", "PHCNPPIKJ", "https://pbs.twimg.com/profile_images/661968066729738240/mKmGmNm0_400x400.jpg"));
        billerList.add(new Biller.ElectricityBillers("PHCN KANO", "PHCNKAN", "PHCNPPKAN", "https://www.kedco.ng/assets/images/Kedco%20Logo%20web.png"));
        billerList.add(new Biller.ElectricityBillers("PHCN KADUNA", "PHCNKAD", "PHCNPPKAD", "https://gadopress.com/wp-content/uploads/2020/01/Kaedco.jpg"));
        billerList.add(new Biller.ElectricityBillers("PHCN JOS", "PHCNJOS", "PHCNPPJOS", "https://pbs.twimg.com/profile_images/898118928404295681/qqSdt2Ir_400x400.jpg"));
        billerList.add(new Biller.ElectricityBillers("PHCN Abuja", "PHCNABJ", "PHCNABJ", "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTnKMMYPAMtpgqYFgPf0Xp6cOPmshafjnWj4Q&usqp=CAU"));
        billerList.add(new Biller.ElectricityBillers("PHCN IBADAN", "PHCNIBD", "PHCNIBD", "https://www.thedaily-ng.com/wp-content/uploads/2019/11/IBEDC.png"));
        billerList.add(new Biller.ElectricityBillers("PHCN ENUGU", "PHCNENU", "PHCNPPENU", "https://pbs.twimg.com/profile_images/1162387741910454275/WF1aMEiU.jpg"));
        return billerList;
    }

    public static List<List<Biller.DataPlan>> getServiceProviderPlans(Context context) {
        String dataPlans = getServiceProviderPlansJson(context);
        Biller.ServiceProviderDataPlans serviceProviderDataPlans = new Gson().fromJson(dataPlans, Biller.ServiceProviderDataPlans.class);
        List<List<Biller.DataPlan>> dataPlanList = new ArrayList<>();
        dataPlanList.add(serviceProviderDataPlans.mtnPlans);
        dataPlanList.add(serviceProviderDataPlans.gloPlans);
        dataPlanList.add(serviceProviderDataPlans.etisalatPlans);
        dataPlanList.add(serviceProviderDataPlans.airtelPlans);
        return dataPlanList;
    }

    public static List<Biller.SmileInternet> getSmileInternetPackages(Context context) {
        String plans = getPlansJson(context);
        Biller.BillerPlans billerPlans = new Gson().fromJson(plans, Biller.BillerPlans.class);
        return billerPlans.smileInternetList;
    }

    public static List<Biller.SpectranetInternet> getSpectranetPlans(Context context) {
        String plans = getPlansJson(context);
        Biller.BillerPlans billerPlans = new Gson().fromJson(plans, Biller.BillerPlans.class);
        return billerPlans.spectranetInternetList;
    }

    /*public static List<ProductEntity> generateProducts() {
        List<ProductEntity> products = new ArrayList<>(FIRST.length * SECOND.length);
        Random rnd = new Random();
        for (int i = 0; i < FIRST.length; i++) {
            for (int j = 0; j < SECOND.length; j++) {
                ProductEntity product = new ProductEntity();
                product.setName(FIRST[i] + " " + SECOND[j]);
                product.setDescription(product.getName() + " " + DESCRIPTION[j]);
                product.setPrice(rnd.nextInt(240));
                product.setId(FIRST.length * i + j + 1);
                products.add(product);
            }
        }
        return products;
    }*/

    /*public static List<CommentEntity> generateCommentsForProducts(
            final List<ProductEntity> products) {
        List<CommentEntity> comments = new ArrayList<>();
        Random rnd = new Random();

        for (Product product : products) {
            int commentsNumber = rnd.nextInt(5) + 1;
            for (int i = 0; i < commentsNumber; i++) {
                CommentEntity comment = new CommentEntity();
                comment.setProductId(product.getId());
                comment.setText(COMMENTS[i] + " for " + product.getName());
                comment.setPostedAt(new Date(System.currentTimeMillis()
                        - TimeUnit.DAYS.toMillis(commentsNumber - i) + TimeUnit.HOURS.toMillis(i)));
                comments.add(comment);
            }
        }

        return comments;
    }*/
}
