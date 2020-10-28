package com.sunyard.i80.util;


public class HexUtil {
	
	
	
	/**
	 * @函数功能: 字节转换为16进制串
     * @输入参数: btye
     * @输出结果: 16进制串
     * @author Xrh @time 20130410
	 */
	public static String byteToHex(byte b) {
		return ("" + "0123456789ABCDEF".charAt(0xf & b >> 4) + "0123456789ABCDEF".charAt(b & 0xf));
	}
	
	public static byte[] hexStringToByte(String hex) {
		hex = hex.toUpperCase();
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
	}
	
    private static byte asc_to_bcd(byte asc) {  
        byte bcd;  
  
        if ((asc >= '0') && (asc <= '9'))  
            bcd = (byte) (asc - '0');  
        else if ((asc >= 'A') && (asc <= 'F'))  
            bcd = (byte) (asc - 'A' + 10);  
        else if ((asc >= 'a') && (asc <= 'f'))  
            bcd = (byte) (asc - 'a' + 10);  
        else  
            bcd = (byte) (asc - 48);  
        return bcd;  
    }  
  
    public static byte[] ASCII_To_BCD(byte[] ascii, int asc_len) {  
        byte[] bcd = new byte[asc_len / 2];  
        int j = 0;  
        for (int i = 0; i < (asc_len + 1) / 2; i++) {  
            bcd[i] = asc_to_bcd(ascii[j++]);  
            bcd[i] = (byte) (((j >= asc_len) ? 0x00 : asc_to_bcd(ascii[j++])) + (bcd[i] << 4));  
        }  
        return bcd;  
    }  

    /** *//**
     * @函数功能: BCD码转为10进制串(阿拉伯数据)
     * @输入参数: BCD码
     * @输出结果: 10进制串
     */
 public static String bcd2Str(byte[] bytes){
     StringBuffer temp=new StringBuffer(bytes.length*2);
     for(int i=0;i<bytes.length;i++){
      temp.append((byte)((bytes[i]& 0xf0)>>>4));
      temp.append((byte)(bytes[i]& 0x0f));
     }
     return temp.toString().substring(0,1).equalsIgnoreCase("0")?temp.toString().substring(1):temp.toString();
 }

	public static String bcd2str(byte[] bcds) {
		char[] ascii = "0123456789abcdef".toCharArray();
		byte[] temp = new byte[bcds.length * 2];
		for (int i = 0; i < bcds.length; i++) {
			temp[i * 2] = (byte) ((bcds[i] >> 4) & 0x0f);
			temp[i * 2 + 1] = (byte) (bcds[i] & 0x0f);
		}
		StringBuffer res = new StringBuffer();

		for (int i = 0; i < temp.length; i++) {
			res.append(ascii[temp[i]]);
		}
		return res.toString().toUpperCase();
	}

	private static byte toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c);
		return b;
	}

	public static byte[] int2bytes(int num) {
		byte[] b = new byte[4];
		int mask = 0xff;
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (num >>> (24 - i * 8));
		}
		return b;
	}

	public static int bytes2int(byte[] b) {
		// byte[] b=new byte[]{1,2,3,4};
		int mask = 0xff;
		int temp = 0;
		int res = 0;
		for (int i = 0; i < 4; i++) {
			res <<= 8;
			temp = b[i] & mask;
			res |= temp;
		}
		return res;
	}
	
    /** 
     * 将长度为2的byte数组转换为16位int 
     *  
     * @param res 
     *            byte[] 
     * @return int 
     * */  
	public static int bytes2short(byte[] b) {
		// byte[] b=new byte[]{1,2,3,4};
		int mask = 0xff;
		int temp = 0;
		int res = 0;
		for (int i = 0; i < 2; i++) {
			res <<= 8;
			temp = b[i] & mask;
			res |= temp;
		}
		return res;
	}

	public static String getBinaryStrFromByteArr(byte[] bArr) {
		String result = "";
		for (byte b : bArr) {
			result += getBinaryStrFromByte(b);
		}
		return result;
	}


	public static String getBinaryStrFromByte(byte b) {
		String result = "";
		byte a = b;
		;
		for (int i = 0; i < 8; i++) {
			byte c = a;
			a = (byte) (a >> 1);
			a = (byte) (a << 1);
			if (a == c) {
				result = "0" + result;
			} else {
				result = "1" + result;
			}
			a = (byte) (a >> 1);
		}
		return result;
	}

	public static String getBinaryStrFromByte2(byte b) {
		String result = "";
		byte a = b;
		;
		for (int i = 0; i < 8; i++) {
			result = (a % 2) + result;
			a = (byte) (a >> 1);
		}
		return result;
	}

	public static String getBinaryStrFromByte3(byte b) {
		String result = "";
		byte a = b;
		;
		for (int i = 0; i < 8; i++) {
			result = (a % 2) + result;
			a = (byte) (a / 2);
		}
		return result;
	}
	

    public static byte[] toByteArray(int iSource, int iArrayLen) { 
        byte[] bLocalArr = new byte[iArrayLen]; 
        for ( int i = 0; (i < 4) && (i < iArrayLen); i++) { 
            bLocalArr[i] = (byte)( iSource>>8*i & 0xFF ); 
           
        } 
        return bLocalArr; 
    }  
    
  
    public static byte[] xor(byte[] op1,byte[] op2){
    	if(op1.length!=op2.length){
    		throw new IllegalArgumentException("参数错误，长度不一致");
    	}
    	byte[] result = new byte[op1.length];
    	for(int i=0;i<op1.length;i++){
    		result[i] = (byte) (op1[i] ^ op2[i]);
    	}
    	return result;
    }
    /**
	 * 
	 * @method toBCD
	 * @description ASCII字节数组转BCD编码
	 * @param asc   ASCII字节
	 * @return
	 */
	public static byte[] toBCD(byte[] asc) throws IllegalArgumentException
	{
		// 空处理
		if (null == asc)
		{
			return null;
		}
		// 获得原始字节长度
		int len = asc.length;
		byte[] tmp = null;
		// 补位
		if (len % 2 != 0)
		{
			tmp = new byte[len + 1];
			//复制字节
			for (int i = 0; i < asc.length; i++)
			{
				tmp[i] = asc[i];
			}
			//补十六进制0
			tmp[len] =0x0;
			
		} 
		else
		{
			tmp = new byte[len];
			//复制字节
			for (int i = 0; i < asc.length; i++)
			{
				tmp[i] = asc[i];
			}
		}
		
		// 输出的bcd字节数组
		byte bcd[] = new byte[tmp.length / 2];

		
		int j, k;
		
		for (int p = 0; p < tmp.length / 2; p++)
		{
			// 双
			//处理0
			if(tmp[2 * p]==0x0)
			{
				j =0x0;
			}
			else if(tmp[2 * p]==0xf)
			{
				j =0xf;
			}
			//处理字符0-9
			else if ((tmp[2 * p] >= '0') && (tmp[2 * p] <= '9'))
			{
				j = tmp[2 * p] - '0';
			}
			//处理字符A
			else if (tmp[2 * p] == 'a' || tmp[2 * p] == 'A')
			{
				j = 0xa;
			}
			//处理字符B
			else if (tmp[2 * p] == 'b' || tmp[2 * p] == 'B')
			{
				j = 0xb;
			}
			//处理字符C
			else if (tmp[2 * p] == 'c' || tmp[2 * p] == 'C')
			{
				j = 0xc;
			}
			//处理字符D以及字符=
			else if (tmp[2 * p] == 'D' || tmp[2 * p] == 'd' || tmp[2 * p] == '=')
			{
				j = 0xd;
			}
			//处理字符E
			else if (tmp[2 * p] == 'e' || tmp[2 * p] == 'E')
			{
				j = 0xe;
			}
			//处理字符F
			else if (tmp[2 * p] == 'f' || tmp[2 * p] == 'F')
			{
				j = 0xf;
			}
			else
			{
				throw new IllegalArgumentException("非BCD字节");
			}

			// 单
			//处理0x0
			if(tmp[2 * p + 1]==0x0)
			{
				k =0x0;
			}
			//处理0xf
			else if(tmp[2 * p + 1]==0xf)
			{
				k =0xf;
			}
			//处理字符0-9
			else if((tmp[2 * p + 1] >= '0') && (tmp[2 * p + 1] <= '9'))
			{
				k = tmp[2 * p + 1] - '0';
			}
			//处理字符A
			else if (tmp[2 * p + 1] == 'a' || tmp[2 * p + 1] == 'A')
			{
				k = 0xa;
			}
			//处理字符B
			else if (tmp[2 * p + 1] == 'b' || tmp[2 * p + 1] == 'B')
			{
				k = 0xb;
			}
			//处理字符C
			else if (tmp[2 * p + 1] == 'c' || tmp[2 * p + 1] == 'C')
			{
				k = 0xc;
			}
			//处理字符D以及字符=
			else if (tmp[2 * p + 1] == 'D' || tmp[2 * p + 1] == 'd' || tmp[2 * p + 1]  == '=' )
			{
				k = 0xd;
			}
			//处理字符E
			else if (tmp[2 * p + 1] == 'e' || tmp[2 * p + 1] == 'E')
			{
				k = 0xe;
			}
			//处理字符F
			else if (tmp[2 * p + 1] == 'f' || tmp[2 * p + 1] == 'F')
			{
				k = 0xf;
			}
			else
			{
				throw new IllegalArgumentException("非BCD字节");
			}

			int a = (j << 4) + k;
			byte b = (byte) a;

			bcd[p] = b;
		}

		return bcd;

	}
	/**
	 * 
	 * @method toBCD
	 * @description 字符串转化为BCD压缩码
	 *              例如字符串"12345678",
	 *              压缩之后的字节数组内容为{0x12,0x34,0x56,0x78}
	 * @param asc   需要进行压缩的字符串
	 * @return
	 */
	public static byte[] toBCD(String asc) throws IllegalArgumentException
	{
		// 空处理
		if (null == asc || asc.trim().equals(""))
		{
			return null;
		}
		//替换=号
		asc = asc.replace('=', 'D').toUpperCase();
		// 非BCD编码处理
		if (!isBCD(asc))
		{
			throw new IllegalArgumentException("非BCD字符串");
		}
		return toBCD(asc.getBytes());
	}
	/**
	 * 
	 * @method isBCD
	 * @description 是否为BCD字符串
	 * @param asc BCD字符串
	 * @return
	 */
	public static boolean isBCD(String bcd)
	{
		boolean ans = false;
		// 化为大写
		bcd = bcd.toUpperCase();
		for (int i = 0; i < bcd.length(); i++)
		{
			ans = false;
			char c = bcd.charAt(i);
			if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9' || 
				c == 'A' || c == 'B' || c == 'C' || c == 'D' || c == 'E' || c == 'F')
			{
				ans = true;
			}
			else 
			{
				break;
			}
		}
		return ans;
	}
	/***
	 * The BCD code is converted into a Arabia number, such as an array of {0x12,0x34,0x56} converted to a Arabia number string is 123456
	 * @method toString
	 * @param bcd
	 * @return
	 */
	public static String toString(byte[] bcd)
	{

		// NULL处理
		if (null == bcd)
		{
			return null;
		}
		// 空处理
		if (bcd.length == 0)
		{
			return "";
		}

		StringBuffer buffer = new StringBuffer(bcd.length * 2);
		for (int i = 0; i < bcd.length; i++)
		{

			switch ((bcd[i] & 0xf0) >>> 4)
			{
			case 10:
				buffer.append("A");
				break;
			case 11:
				buffer.append("B");
				break;
			case 12:
				buffer.append("C");
				break;
			case 13:
				buffer.append("D");
				break;
			case 14:
				buffer.append("E");
				break;
			case 15:
				buffer.append("F");
				break;
			default:
				buffer.append((byte) ((bcd[i] & 0xf0) >>> 4));
				break;
			}
			switch (bcd[i] & 0x0f)
			{
			case 10:
				buffer.append("A");
				break;
			case 11:
				buffer.append("B");
				break;
			case 12:
				buffer.append("C");
				break;
			case 13:
				buffer.append("D");
				break;
			case 14:
				buffer.append("E");
				break;
			case 15:
				buffer.append("F");
				break;
			default:
				buffer.append((byte) (bcd[i] & 0x0f));
				break;
			}

		}
		return buffer.toString();
	}
}
