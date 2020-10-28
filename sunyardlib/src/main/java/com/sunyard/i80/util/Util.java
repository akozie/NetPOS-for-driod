package com.sunyard.i80.util;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("DefaultLocale")
public class Util {
	/**
	 * 字节数组转换为字符串
	 *
	 * @param bArray
	 *            0x00, 0xAF
	 * @return "00AF"
	 */
	public static String BytesToString(byte[] bArray) {
		if (bArray == null) {
			return null;
		}
		if (bArray.length == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}

	/**
	 * 一个字节转换为字符串
	 *
	 * @param b
	 *            0xAF
	 * @return "AF"
	 */
	public static String OneByteToString(byte b) {
		byte[] bTemp = new byte[1];
		bTemp[0] = b;
		return BytesToString(bTemp);
	}

	/**
	 * 字符串转换为字节数组
	 *
	 * @param data
	 *            "00AF"
	 * @return 0x00, 0xAF
	 */
	public static byte[] StringToBytes(String data) {
		String hexString = data.toUpperCase().trim();
		if (hexString.length() % 2 != 0) {
			return null;
		}
		byte[] retData = new byte[hexString.length() / 2];
		for (int i = 0; i < hexString.length(); i++) {
			int int_ch; // 两位16进制数转化后的10进制数
			char hex_char1 = hexString.charAt(i); // //两位16进制数中的第一位(高位*16)
			int int_ch1;
			if (hex_char1 >= '0' && hex_char1 <= '9')
				int_ch1 = (hex_char1 - 48) * 16; // // 0 的Ascll - 48
			else if (hex_char1 >= 'A' && hex_char1 <= 'F')
				int_ch1 = (hex_char1 - 55) * 16; // // A 的Ascll - 65
			else
				return null;
			i++;
			char hex_char2 = hexString.charAt(i); // /两位16进制数中的第二位(低位)
			int int_ch2;
			if (hex_char2 >= '0' && hex_char2 <= '9')
				int_ch2 = (hex_char2 - 48); // // 0 的Ascll - 48
			else if (hex_char2 >= 'A' && hex_char2 <= 'F')
				int_ch2 = hex_char2 - 55; // // A 的Ascll - 65
			else
				return null;
			int_ch = int_ch1 + int_ch2;
			retData[i / 2] = (byte) int_ch;// 将转化后的数放入Byte里
		}
		return retData;
	}

	@SuppressLint("SimpleDateFormat")
	public static String DateToString(Date date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		String str = sdf.format(date);
		return str;
	}

	@SuppressLint("SimpleDateFormat")
	public static Date StringToDate(String dateString, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setLenient(false);
		Date date;
		try {
			date = sdf.parse(dateString);
			return date;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] concat(byte[] b1, byte[] b2) {
		if(b1==null) {
			return b2;
		} else if (b2==null) {
			return b1;
		}
		byte[] result = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, result, 0, b1.length);
		System.arraycopy(b2, 0, result, b1.length, b2.length);
		return result;
	}

	public static byte[] subbytes(byte[] src, int start, int length) {
		if (src == null || start < 0 || length <= 0) {
			return null;
		}
		if (src.length < start + length) {
			return null;
		}
		byte[] result = new byte[length];
		System.arraycopy(src, start, result, 0, result.length);
		return result;
	}

	/**
	 * @param num
	 * @return 该数字的15-8位
	 */
	public static byte getNumHigh(int num) {
		return (byte) ((num & 0xFF00) >> 8);
	}

	/**
	 * @param num
	 * @return 该数字的7-0位
	 */
	public static byte getNumLow(int num) {
		return (byte) (num & 0xFF);
	}

	/**
	 * @param high
	 *            高地址字节
	 * @param low
	 *            低地址字节
	 * @return 两个字节代表的int
	 */
	public static int getInteger(byte high, byte low) {
		return (0xFF & high) * 0x100 + (0xFF & low);
	}

	/**
	 * @功能: BCD码转为10进制串(阿拉伯数据)
	 * @参数: BCD码
	 * @结果: 10进制串
	 */
	public static String bcd2Str(byte[] bytes) {
		StringBuffer temp = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
			temp.append((byte) (bytes[i] & 0x0f));
		}
		return temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp
				.toString().substring(1) : temp.toString();
	}

	/**
	 * @功能: 10进制串转为BCD码
	 * @参数: 10进制串
	 * @结果: BCD码
	 */
	public static byte[] str2Bcd(String asc) {
		int len = asc.length();
		int mod = len % 2;
		if (mod != 0) {
			asc = "0" + asc;
			len = asc.length();
		}
		byte abt[] = new byte[len];
		if (len >= 2) {
			len = len / 2;
		}
		byte bbt[] = new byte[len];
		abt = asc.getBytes();
		int j, k;
		for (int p = 0; p < asc.length() / 2; p++) {
			if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
				j = abt[2 * p] - '0';
			} else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
				j = abt[2 * p] - 'a' + 0x0a;
			} else {
				j = abt[2 * p] - 'A' + 0x0a;
			}
			if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
				k = abt[2 * p + 1] - '0';
			} else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
				k = abt[2 * p + 1] - 'a' + 0x0a;
			} else {
				k = abt[2 * p + 1] - 'A' + 0x0a;
			}
			int a = (j << 4) + k;
			byte b = (byte) a;
			bbt[p] = b;
		}
		return bbt;
	}

	/**
	 * 查找byte
	 * @param src		带查找的byte数组
	 * @param offset	byte数组偏移
	 * @param b
	 * @return
	 */
	public static int byteFind(byte[] src, int offset, byte b) {
		if (offset > src.length - 1) {
			return -1;
		}
		for (int i = offset; i < src.length; i++) {
			if (src[i] == b) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 将index偏移的可见字符的两字节的byte数组转换为int
	 * @param data	"FF"--0x45 0x45
	 * @param index	偏移
	 * @return		0xFF
	 */
	public static int hexByteStrigToInt(byte[] data, int index) {
		if (data == null) {
			return 0;
		}
		if (data.length - index < 2) {
			return 0;
		}
		byte[] lLength = new byte[2];
		byte[] tempLength;
		System.arraycopy(data, index, lLength, 0, 2);
		tempLength = StringToBytes(new String(lLength));
		if (tempLength == null) {
			return 0;
		}
		return tempLength[0];
	}
}
