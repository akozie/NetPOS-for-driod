/*
 * *© Copyright 2020 Visa. All Rights Reserved.**
 *
 * NOTICE: The software and accompanying information and documentation (together, the “Software”) remain the property of
 * and are proprietary to Visa and its suppliers and affiliates. The Software remains protected by intellectual property
 * rights and may be covered by U.S. and foreign patents or patent applications. The Software is licensed and not sold.*
 *
 * By accessing the Software you are agreeing to Visa's terms of use (developer.visa.com/terms) and privacy policy (developer.visa.com/privacy).
 * In addition, all permissible uses of the Software must be in support of Visa products, programs and services provided
 * through the Visa Developer Program (VDP) platform only (developer.visa.com). **THE SOFTWARE AND ANY ASSOCIATED
 * INFORMATION OR DOCUMENTATION IS PROVIDED ON AN “AS IS,” “AS AVAILABLE,” “WITH ALL FAULTS” BASIS WITHOUT WARRANTY OR
 * CONDITION OF ANY KIND. YOUR USE IS AT YOUR OWN RISK.** All brand names are the property of their respective owners, used for identification purposes only, and do not imply
 * product endorsement or affiliation with Visa. Any links to third party sites are for your information only and equally
 * do not constitute a Visa endorsement. Visa has no insight into and control over third party content and code and disclaims
 * all liability for any such components, including continued availability and functionality. Benefits depend on implementation
 * details and business factors and coding steps shown are exemplary only and do not reflect all necessary elements for the
 * described capabilities. Capabilities and features are subject to Visa’s terms and conditions and may require development,
 * implementation and resources by you based on your business and operational details. Please refer to the specific
 * API documentation for details on the requirements, eligibility and geographic availability.*
 *
 * This Software includes programs, concepts and details under continuing development by Visa. Any Visa features,
 * functionality, implementation, branding, and schedules may be amended, updated or canceled at Visa’s discretion.
 * The timing of widespread availability of programs and functionality is also subject to a number of factors outside Visa’s control,
 * including but not limited to deployment of necessary infrastructure by issuers, acquirers, merchants and mobile device manufacturers.*
 */

package com.woleapp.netpos.contactless.taponphone.visa.PPSEv21;


public class Candidate {
    // ADF candidate application AID
    private byte[] aid;
    // ADF candidate application name
    private byte[] name;
    // ADF candidate application label
    private byte[] label;
    // ADF candidate application priority indicator
    private byte priorityIndicator;

    /**
     * ADF Candidate constructor that initializes fields internally
     * @param aid   Application AID in byte[]
     * @param name  Application name in byte[]
     * @param label Application label in byte[]
     * @param priorityIndicator priority indicator. internally masks out RFU bits
     */
    public Candidate(byte[] aid, byte[] name, byte[] label, byte priorityIndicator) {
        this.aid = aid;
        this.name = name;
        this.label = label;
        this.priorityIndicator = (byte)(priorityIndicator & 0x0F);
    }

    /**
     * Application AID getter
     * @return  AID in byte[]
     */
    public byte[] getAid() {
        return aid;
    }

    /**
     * Application name getter
     * @return name in byte[]
     */
    public byte[] getName() {
        return name;
    }

    /**
     * Application label getter
     * @return label in byte[]
     */
    public byte[] getLabel() {
        return label;
    }

    /**
     * priority indicator getter
     * @return priority indicator in byte
     */
    public byte getPriorityIndicator() {
        return priorityIndicator;
    }
}
