package com.sw.api.modules.iot.tuya;

import com.sw.api.modules.iot.config.TuyaProperties;
import com.sw.api.modules.iot.tuya.dto.TuyaApiResponse;
import com.sw.api.modules.iot.tuya.dto.TuyaTokenResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maneja el ciclo de vida del access token de Tuya: lo obtiene, lo cachea en memoria
 * y lo renueva automáticamente antes de que expire (Tuya lo emite con vida de 7200s).
 */
@Service
public class TuyaAuthService {

    private static final long EXPIRY_MARGIN_MILLIS = 60_000;

    private final WebClient webClient;
    private final TuyaProperties properties;
    private final TuyaSignatureService signatureService;

    private volatile String cachedAccessToken;
    private volatile long expiresAtMillis;

    public TuyaAuthService(WebClient tuyaWebClient, TuyaProperties properties, TuyaSignatureService signatureService) {
        this.webClient = tuyaWebClient;
        this.properties = properties;
        this.signatureService = signatureService;
    }

    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < expiresAtMillis - EXPIRY_MARGIN_MILLIS) {
            return cachedAccessToken;
        }
        return requestNewToken();
    }

    public synchronized void invalidate() {
        cachedAccessToken = null;
        expiresAtMillis = 0;
    }

    private String requestNewToken() {
        String path = "/v1.0/token?grant_type=1";
        long timestamp = System.currentTimeMillis();
        String sign = signatureService.sign(null, timestamp, "GET", path, null);

        TuyaApiResponse<TuyaTokenResult> response = webClient.get()
                .uri(path)
                .header("client_id", properties.getClientId())
                .header("sign", sign)
                .header("t", String.valueOf(timestamp))
                .header("sign_method", "HMAC-SHA256")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<TuyaApiResponse<TuyaTokenResult>>() {
                })
                .block();

        if (response == null || !response.success() || response.result() == null) {
            String msg = response != null ? response.msg() : "sin respuesta de Tuya";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo obtener el access token de Tuya: " + msg);
        }

        cachedAccessToken = response.result().accessToken();
        expiresAtMillis = System.currentTimeMillis() + response.result().expireTimeSeconds() * 1000;
        return cachedAccessToken;
    }
}
