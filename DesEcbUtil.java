package com.yinlian.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

//ECB模式  DES加密、解密算法
public class DesEcbUtil {
    // 设定字符集
    public static String Charset = "GBK";

    /**
     * 用des加密String明文输入,String密文输出 不足8位补零
     * 
     * @param tmpVal String明文
     * @return String密文
     * @throws Exception
     */
    public static String getDesEncrypt(String tmpVal, String posKey) throws Exception {
        byte[] s;
        try {
            byte[] byteTmpVal = null;
            byte[] bufTmp = tmpVal.getBytes(Charset);
            int addLen = 0;
            // 不是8的整数倍补0
            if (bufTmp.length % 8 != 0) {
                addLen = 8 - bufTmp.length % 8;
            }
            byteTmpVal = new byte[bufTmp.length + addLen];
            System.arraycopy(bufTmp, 0, byteTmpVal, 0, bufTmp.length);
            // 调用机密函数
            s = encryptDES(byteTmpVal, posKey.getBytes(Charset));

        } catch (Exception e) {
            throw e;
        }
        return bytesToHexString(s);
    }

    /**
     * 计算mac 不足8位补零
     * 
     * @param tmpVal String明文
     * @return String密文
     * @throws Exception
     */
    public static String getDesMac(String tmpVal, String posKey) throws Exception {
        String s;
        // try {
        s = clacMac2(tmpVal.getBytes(Charset), posKey.getBytes(Charset));

        // } catch (Exception e) {
        // throw e;
        // }
        return s;
    }

    /**
     * 解密报文
     * 
     * @param tmpVal String明文
     * @return String密文
     * @throws Exception
     */
    public static String decryptDES(String tmpVal, String posKey) throws Exception {
        String s;
        try {
            s = hexToString(bytesToHexString(decryptDES(tmpVal, posKey.getBytes(Charset))));
        } catch (Exception e) {
            throw e;
        }
        return s.trim();
    }

    /**
     * des加密数据
     * 
     * @param encryptbyte 注意：这里的数据长度只能为8的倍数
     * @param encryptKey
     * @return
     * @throws Exception
     */
    public static byte[] encryptDES(byte[] encryptbyte, byte[] encryptKey) throws Exception {
        SecretKeySpec key = new SecretKeySpec(getKey(encryptKey), "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        // cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        // cipher = Cipher.getInstance("DES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(encryptbyte);
        return encryptedData;
    }

    // 进行ECB模式的DES加密，已验证成功
    public static void main(String[] args) {
        try {
            String text = "text";
            String key = "12345678";
            String cipher = getDesEncrypt(text, key);
            System.out.println(cipher);
            System.out.println(decryptDES(cipher, key));
        } catch (Exception e) {
            System.exit(1);
        }
    }

    /**
     * 自定义一个key
     * 
     * @param keyRule
     */
    public static byte[] getKey(byte[] keyRule) {
        Key key = null;
        byte[] keyByte = keyRule;
        // 创建一个空的八位数组,默认情况下为0
        byte[] byteTemp = new byte[8];
        // 将用户指定的规则转换成八位数组
        for (int i = 0; i < byteTemp.length && i < keyByte.length; i++) {
            byteTemp[i] = keyByte[i];
        }
        key = new SecretKeySpec(byteTemp, "DES");
        return key.getEncoded();
    }

    /***
     * 解密数据
     * 
     * @param decryptString
     * @param decryptKey
     * @return
     * @throws Exception
     */
    public static byte[] decryptDES(String decryptString, byte[] decryptKey) throws Exception {
        SecretKeySpec key = new SecretKeySpec(getKey(decryptKey), "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte decryptedData[] = cipher.doFinal(hexStringToByte(decryptString));
        return decryptedData;
    }

    public static String bytesToHex(byte[] data) {
        if (data == null) {
            return null;
        } else {
            int len = data.length;
            String str = "";
            for (int i = 0; i < len; i++) {
                if ((data[i] & 0xFF) < 16) {
                    str = str + "0"
                            + Integer.toHexString(data[i] & 0xFF);
                } else {
                    str = str
                            + Integer.toHexString(data[i] & 0xFF);
                }
            }
            return str.toUpperCase();
        }
    }

    /**
     * 把字节数组转换成16进制字符串
     * 
     * @param bArray
     * @return
     */
    public static final String bytesToHexString(byte[] bArray) {
        if (bArray == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 把16进制字符串转换成字节数组
     * 
     * @param hex
     * @return
     */
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

    /**
     * mac计算,数据不为8的倍数，需要补0，将数据8个字节进行异或，再将异或的结果与下一个8个字节异或，一直到最后，将异或后的数据进行DES计算
     * 
     * @param key
     * @param Input
     * @return
     * @throws Exception
     */
    public static byte[] clacMac(byte[] key, byte[] Input) {
        byte[] retBuf = new byte[8];
        try {
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
            byte[] buff = encryptDES(oper1, key);
            // 取8个长度字节

            System.arraycopy(buff, 0, retBuf, 0, 8);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retBuf;
    }

    public static byte byteXOR(byte src, byte src1) {
        return (byte) ((src & 0xFF) ^ (src1 & 0xFF));
    }

    /**
     * 异或运算（如果a、b两个值不相同，则异或结果为1。如果a、b两个值相同，异或结果为0）
     * 
     * @description
     *
     * @author <a href="mailto:hwchu@chinaums.com">楚洪武</a>
     *
     * @date 2015年8月8日 下午1:36:23
     */
    public static byte[] bytesXOR(byte[] src, byte[] src1) {
        int length = src.length;
        if (length != src1.length) {
            return null;
        }
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = byteXOR(src[i], src1[i]);
        }
        return result;
    }

    // 转化十六进制编码为字符串
    public static String hexToString(String s) {
        String rtnString = "";
        try {
            byte[] baKeyword = new byte[s.length() / 2];
            for (int i = 0; i < baKeyword.length; i++) {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(
                        s.substring(i * 2, i * 2 + 2), 16));
            }

            rtnString = new String(baKeyword, Charset);// UTF-16le:Not
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rtnString;
    }

    // 银联ECB算法
    /**
     * mac计算,数据不为8的倍数，需要补0，将数据8个字节进行异或，再将异或的结果与下一个8个字节异或，
     * 将异或运算后的最后8个字节（RESULT
     * BLOCK）转换成16个HEXDECIMAL,取前8个字节用mkey，DES加密,将加密后的结果与后8个字节异或,
     * 用异或的结果TEMP BLOCK 再进行一次单倍长密钥算法运算,将运算后的结果（ENC BLOCK2）转换成16
     * 个HEXDECIMAL，取前8个字节作为MAC值
     * 
     * @param key
     * @param Input
     * @return
     * @throws Exception
     */
    public static String clacMac2(byte[] Input, byte[] key) {
        String retBuf = "00000000";
        try {

            int length = Input.length;
            int x = length % 8;
            int addLen = 0;
            if (x != 0) {
                // 由于des ecb加密模式为对称加密，所以需要补位
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

            // 将异或运算后的最后8个字节（RESULT BLOCK）转换成16个HEXDECIMAL
            String buff2 = bytesToHexString(oper1);
            // System.out.println("16:"+buff2);
            // 取前8个字节用mkey，DES加密
            byte[] buff3 = encryptDES(buff2.substring(0, 8).getBytes(Charset), key);

            // 将加密后的结果与后8个字节异或
            byte[] buff4 = bytesXOR(buff3, buff2.substring(8, buff2.length()).getBytes(Charset));
            // System.out.println("后八位亦或："+buff4);
            // 用异或的结果TEMP BLOCK 再进行一次单倍长密钥算法运算
            byte[] buff5 = encryptDES(buff4, key);
            // System.out.println("des加密："+buff5);
            // 将运算后的结果（ENC BLOCK2）转换成16 个HEXDECIMAL
            String buff6 = bytesToHexString(buff5);
            // System.out.println("最后16位结果："+buff6);
            retBuf = buff6.substring(0, 8);
            // 取8个长度字节作为MAC
            // System.arraycopy(buff6, 0, retBuf, 0, 8);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retBuf;
    }

}
