

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

@Slf4j
public class SignUtil {

    static public String buildSignString(String jsonStr) {
        // 加上特征值Feature.OrderedField，维持原参数的顺序，否则在goodsDetail的值为数组时，数组对象的参数顺序会被打乱，
        // 造成验签和签名字符串不一致，验签失败，值为字符串时不会有这个问题
        JSONObject json = JSONObject.parseObject(jsonStr, Feature.OrderedField);
        return buildSignString(json);
    }

    /**
     * 将buildSignString结果反解为json
     * @param buildRes
     * @param charset 默认utf8
     * @return
     */
    static public JSONObject parseSignString(String buildRes, String charset) {
        if(StringUtils.isBlank(buildRes)){
            return null;
        }
        try{
            buildRes = URLDecoder.decode(buildRes,  StringUtils.isBlank(charset) ? "UTF-8" : charset);
        }catch(UnsupportedEncodingException e){
            log.error("", e);
            throw new RuntimeException("数据解码出错");
        }
        JSONObject json = new JSONObject();
        String[] parts = buildRes.split("&");
        for(String part : parts){
            String[] keyValue = part.split("=");
            //只接受简单key=value的形式，value中不含=
            if(keyValue == null || keyValue.length != 2){
                log.info("错误的key=value形式：{}", part);
                throw new RuntimeException("错误的key=value形式");
            }
            json.put(keyValue[0], keyValue[1]);
        }
        return json;
    }

    static public String buildSignString(JSONObject json) {
        Map<String, String> map = new HashMap<String, String>();
        for (Object key : json.keySet()) {
            String value = json.getString((String) key);
            map.put((String) key, value);
        }
        return buildSignString(map);
    }

    static public String buildSignString(Map<String, String> params) {
        List<String> keys = new ArrayList<String>(params.size());

        for (String key : params.keySet()) {
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            if (StringUtils.isEmpty(params.get(key))) {
                continue;
            }
            keys.add(key);
        }

        Collections.sort(keys);

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);

            if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
                buf.append(key + "=" + value);
            } else {
                buf.append(key + "=" + value + "&");
            }
        }

        return buf.toString();
    }

    /**
     * 签名方法1，入参Json字符串
     * @param jsonStr
     * @param shaKey
     * @param charset
     * @return
     */
    static public String signWithSha(String jsonStr, String shaKey, String charset) {
        String prestr = buildSignString(jsonStr); // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        return sign(prestr, shaKey, charset);
    }

    /**
     * 签名方法2，入参Json对象
     * @param json
     * @param shaKey
     * @param charset
     * @return
     */
    static public String signWithSha(JSONObject json, String shaKey, String charset) {
        String prestr = buildSignString(json); // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        return sign(prestr, shaKey, charset);
    }

    /**
     * 签名方法3，入参Map
     * @param params
     * @param shaKey
     * @param charset
     * @return
     */
    static public String signWithSha(Map<String, String> params, String shaKey, String charset) {
        String prestr = buildSignString(params); // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        return sign(prestr, shaKey, charset);
    }

    static public String signWithRsa(String jsonStr, String shaKey, String charset) throws Exception {
        String prestr = buildSignString(jsonStr); // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        return signRsa(prestr, shaKey, charset);
    }

    static public String signWithSM3(String jsonStr, String shaKey) {
        String prestr = buildSignString(jsonStr); // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        return signSm3(prestr, shaKey);
    }

    static public String signWithHMAC(Map<String, String> params, String shaKey) throws Exception {
        String prestr = buildSignString(params); // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        return signHMAC(prestr, shaKey);
    }

    static public String sign(String originStr, String shaKey, String charset) {
        String text = originStr + shaKey;
        return DigestUtils.sha256Hex(getContentBytes(text, charset)).toUpperCase();
    }

    static public String signMd5(String originStr, String shaKey, String charset) {
        String text = originStr + shaKey;
        return DigestUtils.md5Hex(getContentBytes(text, charset)).toUpperCase();
    }

    /*static public String signMd5(Map params, String shaKey, String charset) {
        String text = buildJsdfSignString(params) + shaKey;
        return DigestUtils.md5Hex(getContentBytes(text, charset)).toUpperCase();
    }*/

    static private String signRsa(String originStr, String shaKey, String charset) throws Exception {
        return RSAUtils.signHex(getContentBytes(originStr, charset), shaKey);
    }

    static public String signSm3(String originStr, String shaKey) {
        String text = originStr + shaKey;
        return SM3Util.sm3Digest(text);
    }

    static public String signHMAC(String originStr, String shaKey) throws Exception {
        return HMAC.hmacSha256Hex(originStr, shaKey);
    }

    private static byte[] getContentBytes(String content, String charset) {
        if (charset == null || "".equals(charset)) {
            return content.getBytes();
        }
        try {
            return content.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("签名过程中出现错误,指定的编码集不对,您目前指定的编码集是:"
                    + charset);
        }
    }

    public static String sha256Hex(String str){
        if(StringUtils.isBlank(str)){
            return str;
        }
        return DigestUtils.sha256Hex(str);
    }

    public static String signMd5(Map Params, String key, String charset) {
        String signStr = MD5.md5(getContent(Params) + key, charset);
        return signStr;
    }

    public static String getContent(Map params) {
        List keys = new ArrayList(params.keySet());
        Collections.sort(keys);
        String prestr = "";
        boolean first = true;

        for(int i = 0; i < keys.size(); ++i) {
            String key = (String)keys.get(i);
            if (!"sign".equals(key) && !"_r".equals(key) && !"_result_type".equals(key) && !"_".equals(key)) {
                String value = String.valueOf(params.get(key));
                if (value != null && value.trim().length() != 0) {
                    if (first) {
                        prestr = prestr + key + "=" + value;
                        first = false;
                    } else {
                        prestr = prestr + "&" + key + "=" + value;
                    }
                }
            }
        }

        return prestr;
    }

    public static void main(String[] args) {
        String jsonStr = "{\"msgType\":\"wx.orderQuery\",\"payTime\":\"2021-07-02 17:09:08\",\"connectSys\":\"OPENCHANNEL\",\"sign\":\"070B17917734FFC3EFCF2FA751C4DAD5C546158D0EA80BFE0869DCD2CF33ACCF\",\"merName\":\"测试退货5(1111)\",\"mid\":\"898340149000005\",\"invoiceAmount\":\"1\",\"settleDate\":\"2021-07-02\",\"billFunds\":\"现金支付0.01元。\",\"buyerId\":\"o8wNP0SM_KLCvF9zZQ4ANgVlhbGk\",\"mchntUuid\":\"4aa8728a06a04f7385869df8b659cd01\",\"tid\":\"88880001\",\"instMid\":\"YUEDANDEFAULT\",\"receiptAmount\":\"1\",\"targetOrderId\":\"4200001175202107022316197947\",\"signType\":\"SHA256\",\"orderDesc\":\"测试退货5(1111)\",\"seqId\":\"00956901684N\",\"merOrderId\":\"31940106210702000800\",\"targetSys\":\"WXPay\",\"totalAmount\":\"1\",\"createTime\":\"2021-07-02 17:08:49\",\"XM\":\"sdJv\",\"buyerPayAmount\":\"1\",\"notifyId\":\"796d8668-86c3-4d0e-9f96-2b61d1b1e6da\",\"subInst\":\"000100\",\"status\":\"TRADE_SUCCESS\"}";
        String key = "fcAmtnx7MwismjWNhNKdHC44mNXtnEQeJkRrhKJwyrW2ysRR";
        String charset = "UTF-8";
        String sign = signWithSha(jsonStr, key, charset);
        System.out.println("sign: " + sign);
//        System.out.println("sha256Hex: "+sha256Hex(jsonStr));

//        String str = "msgType=wx.orderQuery&payTime=2021-07-02+10%3A36%3A11&connectSys=OPENCHANNEL&sign=F28011F813FFAC4D6FFB0EC1FB0A6F8A2047673434CEC111098415B9669B2E1F&merName=%E6%B5%8B%E8%AF%95%E9%80%80%E8%B4%A75%281111%29&mid=898340149000005&invoiceAmount=1&settleDate=2021-07-02&billFunds=%E7%8E%B0%E9%87%91%E6%94%AF%E4%BB%980.01%E5%85%83%E3%80%82&buyerId=o8wNP0SM_KLCvF9zZQ4ANgVlhbGk&mchntUuid=4aa8728a06a04f7385869df8b659cd01&tid=88880001&instMid=YUEDANDEFAULT&Aq=tndP&receiptAmount=1&targetOrderId=4200001150202107023246567026&signType=SHA256&orderDesc=%E6%B5%8B%E8%AF%95%E9%80%80%E8%B4%A75%281111%29&seqId=00956901281N&merOrderId=31940106210702000701&targetSys=WXPay&totalAmount=1&createTime=2021-07-02+10%3A35%3A50&buyerPayAmount=1&notifyId=d21fe744-58a0-4e4d-b660-d3dc6106a162&subInst=000100&status=TRADE_SUCCESS";
//        System.out.println("json: \n" + parseSignString(str, null));

//        String json = "{\"buyerPayAmount\":1,\"couponAmount\":0,\"merOrderId\":\"31940106210701000004\",\"payTime\":\"2021-07-01 11:31:11\",\"sign\":\"1\",\"targetOrderId\":\"NP12345\"}";
//        String localSign = SignUtil.signWithSha(json, null, Constant.Charset.UTF8);
//        log.info("localSign: {}", localSign);

    }

}
