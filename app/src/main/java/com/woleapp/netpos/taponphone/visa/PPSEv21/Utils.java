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

import java.util.Locale;


public class Utils 
{
	/**
	 * Compares two byte arrays.
	 * 
	 * @param array1
	 * @param array1Offset
	 * @param array2
	 * @param array2Offset
	 * @param maxlength
	 * @return
	 */
	public static boolean arrayCompare(byte[] array1, int array1Offset, byte[] array2, int array2Offset, int maxlength) 
	{
        // For Application Version Number comparison.
        if (array1 == null)
        {
            return true;
        }

        // For CHV currency comparison.
        if (array2 == null)
        {
            return false;
        }

        int j = array2Offset;
        for (int i = array1Offset; i < (array1Offset + maxlength); i++) 
        {
            if (array2[j++] != array1[i])
            {
                return false;
            }
        }
        
        return true;
    }


	/**
	 * @param data
	 * @param offset
	 * @param len
	 * @param delimiter
	 * @return
	 */
	public static String getHexString(byte[] data, int offset, int len, String delimiter) 
	{
    	if (data != null) 
    	{
	    	StringBuffer str = new StringBuffer(len);
	    	for (int i=0; i<len; i++) 
	    	{
	    		if (i != 0 && i%16 == 0) 
	    		{
//	    			str.append("\n  ");
	    		}
	    		
	    		String digit = Integer.toHexString((data[i+offset] & 0x00ff));
	    		if (digit.length() == 1)
	    		{
	    			digit = '0' + digit;
	    		}
	    		
	    		digit = digit.toUpperCase(Locale.US);
	    		str.append( digit + delimiter);
	    	}
	    	return str.toString();
    	}
    	return "";
	}

	
	/**
	 * @param data
	 * @return
	 */
	public static String getString(byte[] data) 
	{
    	if (data != null) 
    	{
    		return getHexString(data, 0, data.length, " ");
    	}
    	return "";
    }

	static final byte TAG = 0;
	static final byte LEN = 1;
	
    /**
     * This method parses a byte array (DOL) into its Tag and Length components. 
     * 
     * @param arr
     * @param offset
     * @param result 
     * @return - Returns number of bytes processed (span)
     * 				result[0] will hold the TAG
     * 				result[1] will hold the LEN 
     */
    public static short parseDOL(byte[] arr, short offset, short[] result) 
    { 
        short off = offset; 
        short offsetOfT; 
        short lengthOfT;  // 1 or 2 
        
    	try
    	{
	        // Ref: EMV 2000; Book 3; Annex B: "... due to erased or modified TLV..."
	        //We do not want these for PPSE.
	//        while ((arr[off] == (byte)0x00) || (arr[off] == (byte)0xFF)) 
	//            off++; 
    		
	        offsetOfT = off; 
	        if ((arr[off] & (byte) 0x1F) == (byte) 0x1F) 
	        {
	            do 
	            { 
	                off++; 
	            } 
	            while ((arr[off] & 0x80) == 0x80);
	        }
	        
	        off++; 
	        lengthOfT = (short)(off - offsetOfT); 
	        
	        if (lengthOfT == 1) 
	            result[TAG] = (short)(arr[offsetOfT] & 0xff); 
	        else if (lengthOfT == 2) 
	            result[TAG] = (short)(((short)(arr[offsetOfT] & 0xff) << 8) + (short)(arr[(short)(offsetOfT + 1)] & 0xff)); 
	//        else 
	//        { 
	//            // We don't have a need to handle Tags that are longer than 2 bytes in length.  (I hope!!!) 
	//        } 
	        
	        // Parse the Length field 
	        result[LEN] = 0; 
	        if ((arr[off] & (byte) 0x80) == (byte) 0x00) 
	        { 
	            result[LEN] = (short)arr[off]; 
	        } 
	        else 
	        { 
	            short numBytes = (short)(arr[off] & (byte) 0x7F); 
	            short j = 0; 
	            while (numBytes > 0) 
	            { 
	                off++; 
	                j = arr[off]; 
	                result[LEN] += ( j < 0 ? j += 256 : j); 
	                if (numBytes > 1) 
	                    result[LEN] *= 256; 
	                numBytes--; 
	            } 
	        } 
	        off++; 
    	}
    	catch(Exception e)
    	{
			//Log.d("TLVParser", e.toString());
    	}
    	
        return (short)(off - offset); 
    }


	public static byte[] hexToByteArray(String hexString) {
		try {
			int len = hexString.length();
			if (len % 2 != 0) {
				//incorrect num of digits for bytes
				hexString = "0"+hexString;
				len = hexString.length();
			}
			byte[] data = new byte[len / 2];
			for (int i = 0; i < len; i += 2) {
				data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) | Character.digit(
						hexString.charAt(i + 1), 16));
			}
			return data;
		} catch (Exception e) {
			return null;
		}
	}
}
