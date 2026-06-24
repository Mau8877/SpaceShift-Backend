package com.sw.api.modules.iot.tuya;

import com.sw.api.modules.iot.config.TuyaProperties;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Service;

/**
 * Firma HMAC-SHA256 que exige Tuya Cloud API en cada request.
 * El orden de los campos en el string a firmar es estricto:
 * clientId + accessToken + t + nonce + method + "\n" + sha256(body) + "\n" + "" + "\n" + path
 */
@Service
public class TuyaSignatureService {

    private final TuyaProperties properties;

    public TuyaSignatureService(TuyaProperties properties) {
        this.properties = properties;
    }

    public String sign(String accessToken, long timestamp, String method, String path, String body) {
        String bodyHash = Hex.encodeHexString(DigestUtils.sha256(body == null ? "" : body));
        String accessTokenPart = accessToken == null ? "" : accessToken;

        String stringToSign = properties.getClientId() + accessTokenPart + timestamp + ""
                + method + "\n" + bodyHash + "\n" + "" + "\n" + path;

        byte[] signed = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_256,
                properties.getClientSecret().getBytes()).doFinal(stringToSign.getBytes());

        return Hex.encodeHexString(signed).toUpperCase();
    }
}
