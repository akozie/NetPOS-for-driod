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


package com.woleapp.netpos.taponphone.visa.PPSEv21;


import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.FCI_PROPRIETARY_TEMPLATE;
import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.FCI_TEMPLATE;
import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.TAG_AID;
import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.TAG_APPLICATION_LABEL;
import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.TAG_APPLICATION_PREFERRED_NAME;
import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.TAG_APPLICATION_PRIORITY_INDICATOR;
import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.TAG_DF_NAME;
import static com.woleapp.netpos.taponphone.visa.PPSEv21.EmvTags.TAG_FCI_ISSUER_DISCRETIONARY_DATA;

import android.util.Log;

import com.visa.app.ttpkernel.NfcTransceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class PPSEManager {
    /** SELECT command that selects PPSE application*/
    public static final byte[] SELECT_PPSE = new byte[] {
            //select
            (byte)0x00, (byte)0xA4, 0x04, 0x00,
            (byte)0x0E,
            //aid
            (byte) 0x32, (byte) 0x50, (byte) 0x41,
            (byte) 0x59, (byte) 0x2E, (byte) 0x53, (byte) 0x59, (byte) 0x53,
            (byte) 0x2E, (byte) 0x44, (byte) 0x44, (byte) 0x46, (byte) 0x30,
            (byte) 0x31, 0x00
    };

    /** Publicly defined PPSE Application AID */
    final byte[] PPSE_AID = new byte[] {
            (byte) 0x32, (byte) 0x50, (byte) 0x41, (byte) 0x59, (byte) 0x2E, (byte) 0x53, (byte) 0x59, (byte) 0x53,
            (byte) 0x2E, (byte) 0x44, (byte) 0x44, (byte) 0x46, (byte) 0x30, (byte) 0x31
    };

    /** supported AID lists. Use setSupportedAids() to initialize list of application AIDs that PPSEManager will filter from SELECT PPSE command*/
    private ArrayList<byte[]> supportedAids;
    /** List of Candidates parsed from SELECT PPSE response*/
    private Vector<Candidate> candidates;
    /** Current selected candidate index from list of candidates */
    private int candidateIndex = 0;

    /**
     * PPSEManager constructor initializes internal data objects
     */
    public PPSEManager() {
        if (supportedAids == null) {
            supportedAids = new ArrayList<byte[]>();
        }
        if (candidates == null) {
            candidates = new Vector<Candidate>();
        }
    }

    /**
     * Set the merchant app's supported AIDs
     *
     * @param supportedAid String List of supported AID
     */
    public void setSupportedApps(List<String> supportedAid) {
        // supportedAid data object not initialized, possible error
        if (supportedAid == null) {
            return;
        }

        // Validating input data
        for (String aid : supportedAid) {
            // AID cannot be less than 5 bytes.
            // Input AID is in String format, cannot parse odd number string representation of byte[]
            if (aid.length() < 10 || ((aid.length() % 2) != 0)) {
                Log.e("PPSE", "Invalid RID Length.");
                continue;
            }
            supportedAids.add(Utils.hexToByteArray(aid));
        }
    }

    /**
     * Get next candidate from the candidate list, automatically increment candidate index and discard previous candidate from the list
     * @return AID if next candidate is available, null otherwise
     */
    public byte[] nextCandidate() {
        if ((candidateIndex + 1)>= candidates.size() || candidates.isEmpty()) {
            return null;
        }

        return candidates.get(++candidateIndex).getAid();
    }

    /**
     * Performs the PPSE Select command and process response data to parse candidates
     * NOTE: This will reset PPSE candidate list.
     *
     * @param nfcTransceiver NFC transceiver object to communicate NFC data exchange
     * @return Candidate AID with highest priority in the list
     * @throws IOException
     */
    public byte[] selectPPSE(NfcTransceiver nfcTransceiver) throws IOException
    {
        // Reset candidate index to 0 every time SELECT PPSE is called
        candidateIndex = 0;

        // Clears previous PPSE candidates every time SELECT PPSE is called
        if (!candidates.isEmpty()) {
            candidates.clear();
        }

        // Reader supported AIDs not initialized. This means nothing will be added to the candidate list
        if (supportedAids.size() == 0) {
            throw new IOException("List of Reader Supported AIDs is missing. Please initialize using setSupportedApps()");
        }

        // Perform SELECT PPSE via NFC transceiver
        byte[] receiveData = nfcTransceiver.transceive(SELECT_PPSE);

        // Validate PPSE response and obtain FCI discretionary template Tlv object.
        // FCI discretionary template shall contain array of directory Tlvs
        TlvData tlvBF0C = validatePPSETemplate(receiveData);

        byte[] aid;
        byte[] name;
        byte[] label;
        byte priority;
        // VCPS 2.2.4 Req 5.45, process each Directory Entry
        for (TlvData entry : tlvBF0C.moreTLVs) {;
            // Obtain mandatory Tlv tag - AID
            TlvData temp = entry.searchTag(TAG_AID);
            aid = (temp == null) ? null : temp.getValue();

            // Obtain optional Tlv tag - Application Label
            temp = entry.searchTag(TAG_APPLICATION_LABEL);
            label = (temp == null) ? null : temp.getValue();

            // Obtain optional Tlv tag - Application Preferred Name
            temp = entry.searchTag(TAG_APPLICATION_PREFERRED_NAME);
            name = (temp == null) ? null : temp.getValue();

            // VCPS Req 5.50
            // Application with priority indicator with 0000b or no value at all, shall be considered lowest prioirty
            temp = entry.searchTag(TAG_APPLICATION_PRIORITY_INDICATOR);
            priority = (temp == null) ? 0 : temp.getValue()[0]; // If tlv not found, give it 0 priority

            // Does not need to add candidate without AID personalized
            if (aid != null) {
                insertCandidate(new Candidate(aid, name, label, priority));
            }
        }

        // Return candidate AID with highest priority
        if (getCandidate() != null) {
            return getCandidate().getAid();
        }
        return null;
    }

    /**
     * Validates mandatory tags from PPSE responses according to VCPS 2.2.4 Table G-1.
     * @param data ppse response data byte array
     * @return FCI Discretionary Data TlvData object
     * @throws IOException
     */
    TlvData validatePPSETemplate(byte[] data) throws IOException {
        if (data == null || data.length <= 2) {
            throw new IOException("Invalid PPSE Response Data.");
        }

        // Construct Tlv object
        TlvData tlv = new TlvData(data, (short)0, (short)(data.length - 2)); // Ignore returned SW

        // Look for mandatory template tags
        // Tag 6F, FCI Template
        if (tlv.searchTag(FCI_TEMPLATE, 0) == null) {
            throw new IOException("Invalid PPSE TLV Format. Missing FCI Template (0x6F).");
        }

        // Search Tlv (0x84) from response tlv
        TlvData tlv84 = tlv.searchTag(TAG_DF_NAME, 1);
        if (tlv84 == null) {
            throw new IOException("Invalide PPSE TLV Format. Missing DF Name Template (0x84).");
        } else {
            // VCPS Table G-1, DF Name (0x84) have strict length of 0x0E
            if (tlv84.length != (short)0x0E) {
                throw new IOException("Invalid PPSE TLV Format. Invalid DF Name Length (0x84).");
            }
            // VCPS Table G-1, DF Name (0x84) must equal to PPSE AID that is publicly defined
            if (tlv84.getValue().length != PPSE_AID.length) {
                throw new IOException("Invalid PPSE DF Name.");
            }
            if (!Utils.arrayCompare(tlv84.getValue(), (short)0, PPSE_AID, (short)0, PPSE_AID.length)) {
                throw new IOException("Invalid PPSE DF Name.");
            }
        }

        // Tlv FCI Proprietary Template (0xA5)
        TlvData tlvA5 = tlv.searchTag(FCI_PROPRIETARY_TEMPLATE, 1);
        if (tlvA5 == null) {
            throw new IOException("Invalid PPSE TLV Format. Missing FCI Proprietary Template (0xA5).");
        }

        // Tlv FCI Issuer Discretionary Template (0xBF0C) is within FCI Proprietary Template
        TlvData tlvBF0C = tlvA5.searchTag(TAG_FCI_ISSUER_DISCRETIONARY_DATA, 1);
        if (tlvBF0C == null) {
            throw new IOException("Invalid PPSE TLV Format. Missing FCI Issuer Discretionary Data Template (0xBF0C)");
        }

        // Returns 0xBF0C Tlv object
        return tlvBF0C;
    }

    /**
     * Insert candidate into internal candidate list sorted by priority indicator
     * @param candidate new Candidate to add
     * @return success/fail
     */
    private boolean insertCandidate(Candidate candidate) {
        // Sanity check, initializes candidate list if it's null
        if (candidates == null) {
            candidates = new Vector<Candidate>();
        }

        // Null candidate or candidate without AID does not add to candidate list
        if (candidate == null || candidate.getAid() == null) {
            return false;
        }

        // EMV Book 1 Req 12.2.1 Check ADF Name coded as EMV defined
        // RID (5 bytes)
        // PIX (up to 11 bytes)
        if (candidate.getAid().length < 5 || candidate.getAid().length > 16) {
            return false;
        }

        // VCPS 2.2.4 Req 5.47
        // If ADF Name matches AID supported by the reader
        boolean matched = false; // Supported AIDs should be initialized, and is checked in selectPPSE
        for (byte[] supported : supportedAids) {
            // Performs partial match search
            if (matchCandidate(candidate, supported)) {
                matched = true;
                break;
            }
        }

        // AID not supported
        if (!matched) {
            return false;
        }

        //Per VCPCS 2.1 Req 5.50,
        // If multiple applications are supported in the candidate list, then:
        //	- The reader shall select the application with the highest priority.
        //	- Applications with an Application Priority Indicator (tag '87', bits 4-1) value of 0000b, or no Application Priority Indicator (tag '87') at all, are considered to be of (equal) lowest priority.
        //	- In the case of multiple candidates with equal priority, the candidates shall be selected in the order listed in the PPSE.
        if (candidate.getPriorityIndicator() == 0) {
            candidates.add(candidate);
            return true;
        }

        int index = 0;
        for (index = 0; index < candidates.size(); ++index) {
            if (candidates.get(index).getPriorityIndicator() == 0) { break; } // Always add in front of AIP 0
            if (candidates.get(index).getPriorityIndicator() > candidate.getPriorityIndicator()) { break; }
        }
        candidates.add(index, candidate);

        return true;
    }

    /**
     * VCPS 2.2.4 Req 5.47
     * Matching Candidate AID with provided byte array AID
     * @param c Candidate
     * @param m AID to match with
     * @return
     */
    boolean matchCandidate(Candidate c, byte[] m) {
        // Sanity check
        if (c == null || c.getAid() == null) {
            return false;
        }

        // Check candidate length
        if (c.getAid().length < m.length) {
            return false;
        }

        // Partial match
        if (!Utils.arrayCompare(c.getAid(), (short)0, m, (short)0, m.length)) {
            return false;
        }

        return true;
    }

    Candidate getCandidate() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.get(candidateIndex);
    }

    /**
     * Clears candidate list, supported AIDs and resets candidate index
     */
    public void clear() {
        candidates.clear();
        candidateIndex = 0;
    }
}
