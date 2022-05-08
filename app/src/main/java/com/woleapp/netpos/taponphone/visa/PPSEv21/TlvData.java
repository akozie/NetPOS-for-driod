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

import java.util.Arrays;
import java.util.Vector;


public class TlvData
{
	static final byte PRIMITIVE = 0;
	static final byte CONSTRUCTED = 1;
	static final byte HEX = 0;
	static final byte ASCII = 1;

	short tag;
	short length;
	byte[] value = null;
	byte tagtype = PRIMITIVE;
	Vector<TlvData> moreTLVs = null;


	/**
	 * @param data
	 * @param offset
	 * @param len
	 */
	public TlvData(byte[] data, short offset, short len)
	{
		short[] TL = new short[2];
		short span = Utils.parseDOL(data, offset, TL);

		tag = TL[0];
		length = TL[1];
		short checktag = (short)((tag >> 8) & 0x00ff);

		if (checktag == 0x00)
			checktag = tag;

		if ((checktag & 0x20) > 0)
		{
			tagtype = CONSTRUCTED;
			moreTLVs = new Vector<TlvData>();

			offset += span;
			while (offset < len)
			{
				span = Utils.parseDOL(data, offset, TL);
				TlvData tlv = new TlvData(data, offset, (short)(offset + TL[1]));
				moreTLVs.add(tlv);
				offset += TL[1];
				offset += span;
			}
		}
		else
		{
			offset += span;
			value = Arrays.copyOfRange(data, offset, offset + TL[1]);
		}
	}


	/**
	 * @return
	 */
	public byte[] getValue()
	{
		if (value != null)
			return value;

		return null;
	}


	/**
	 * @param fndtag
	 * @return
	 */
	public TlvData searchTag(short fndtag)
	{
		if (tag == fndtag)
			return this;

		if (tagtype == CONSTRUCTED)
		{
			for (TlvData tlv : moreTLVs)
			{
				if (tlv.searchTag(fndtag) != null)
					return tlv;
			}
		}
		return null;
	}

	public TlvData searchTag(short fndTag, int searchDepth) {
		if (tag == fndTag) {
			return this;
		}

		if (tagtype == CONSTRUCTED && searchDepth > 0) {
			for (TlvData tlv : moreTLVs) {
				if (tlv.searchTag(fndTag, searchDepth - 1) != null) {
					return tlv;
				}
			}
		}

		return null;
	}
}
