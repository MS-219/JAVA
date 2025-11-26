package com.yinlian.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class DesEcbUtil {
    public static String Charset = "GBK";
    
    public static String getDesEncrypt(String tmpVal, String posKey) throws Exception{
        byte[] s;
        try {
            byte[] byteTmpVal = null;  
            byte[] bufTmp = tmpVal.getBytes(Charset);  
            int addLen = 0;
            if(bufTmp.length % 8 != 0){
                addLen = 8 - bufTmp.length % 8;
            }
            byteTmpVal = new byte[bufTmp.length + addLen];  
            System.arraycopy(bufTmp, 0, byteTmpVal, 0, bufTmp.length);  
            s = encryptDES(byteTmpVal,posKey.getBytes(Charset));
        } catch (Exception e) {
            throw e;
        }
        return bytesToHexString(s);
    }

    public static String getDesMac(String tmpVal, String posKey) throws Exception{
        String s;
        s = clacMac2(tmpVal.getBytes(Charset),posKey.getBytes(Charset));
        return s;
    }
    
    public static String decryptDES(String tmpVal, String posKey) throws Exception{
        String s;
        try {
            s=hexToString(bytesToHexString(decryptDES(tmpVal, posKey.getBytes(Charset))));
        } catch (Exception e) {
            throw e;
        }
        return s.trim();
    }
    
    public static byte[] encryptDES(byte[] encryptbyte, byte[] encryptKey) throws Exception {  
        SecretKeySpec key = new SecretKeySpec(getKey(encryptKey), "DES");  
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);  
        byte[] encryptedData = cipher.doFinal(encryptbyte);  
        return encryptedData;
    }
    
    public static byte[] getKey(byte[] keyRule) {  
        Key key = null;  
        byte[] keyByte = keyRule;  
        byte[] byteTemp = new byte[8];  
        for (int i = 0; i < byteTemp.length && i < keyByte.length; i++) {  
            byteTemp[i] = keyByte[i];  
        }  
        key = new SecretKeySpec(byteTemp, "DES");  
        return key.getEncoded();  
    }  
      
    public static byte[] decryptDES(String decryptString, byte[] decryptKey) throws Exception {  
        SecretKeySpec key = new SecretKeySpec(getKey(decryptKey), "DES");  
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");  
        cipher.init(Cipher.DECRYPT_MODE, key);  
        byte decryptedData[] = cipher.doFinal(hexStringToByte(decryptString));
        return decryptedData;  
    }  
    
    public static String bytesToHexString(byte[] bArray) {  
        if(bArray == null ) return "";  
        StringBuffer sb = new StringBuffer(bArray.length);  
        String sTemp;  
        for (int i = 0; i < bArray.length; i++) {  
            sTemp = Integer.toHexString(0xFF & bArray[i]);  
            if (sTemp.length() < 2) sb.append(0);
            sb.append(sTemp.toUpperCase());  
        }  
        return sb.toString();  
    }  
    
    public static byte[] hexStringToByte(String hex) {  
        int len = (hex.length() / 2);
        byte[] result = new byte[len];  
        char[] achar = hex.toCharArray();  
        for (int i = 0; i < len; i++) {  
            int pos = i * 2;  
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));  
        }
        return result;  
    }  
  
    private static byte toByte(char c) {  
        byte b = (byte) "0123456789ABCDEF".indexOf(c);  
        return b;  
    }  
      
    public static byte[] bytesXOR(byte[] src, byte[] src1) {  
        int length = src.length;  
        if (length != src1.length) return null;  
        byte[] result = new byte[length];  
        for (int i = 0; i < length; i++) {  
            result[i] = (byte) ((src[i] & 0xFF) ^ (src1[i] & 0xFF));  
        }  
        return result;  
    }
    
    public static String hexToString(String s) {
        String rtnString = "";
        try {
            byte[] baKeyword = new byte[s.length() / 2];
            for (int i = 0; i < baKeyword.length; i++) {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            }
            rtnString = new String(baKeyword, Charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rtnString;
    }
    
    public static String clacMac2(byte[] Input,byte[] key) {  
        String retBuf="00000000";
        try{
            int length = Input.length;  
            int x = length % 8;  
            int addLen = 0;  
            if (x != 0) {  
                addLen = 8 - length % 8;  
            }  
            int pos = 0;  
            byte[] data = new byte[length + addLen];  
            System.arraycopy(Input, 0, data, 0, length);  
            byte[] oper1 = new byte[8];  
            System.arraycopy(data, pos, oper1, 0, 8);  
            pos += 8;  
            for (int i = 1; i < data.length / 8; i++) {  
                byte[] oper2 = new byte[8];  
                System.arraycopy(data, pos, oper2, 0, 8);  
                byte[] t = bytesXOR(oper1, oper2);  
                oper1 = t;  
                pos += 8;  
            }  
            String buff2 = bytesToHexString(oper1);
            byte[] buff3 = encryptDES(buff2.substring(0, 8).getBytes(Charset), key);
            byte[] buff4 = bytesXOR(buff3, buff2.substring(8,buff2.length()).getBytes(Charset));
            byte[] buff5 = encryptDES(buff4, key); 	 
            String buff6 = bytesToHexString(buff5);
            retBuf=buff6.substring(0, 8);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return retBuf;  
    }  
}
