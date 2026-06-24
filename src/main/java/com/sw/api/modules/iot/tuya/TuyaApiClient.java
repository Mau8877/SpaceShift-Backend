package com.sw.api.modules.iot.tuya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sw.api.modules.iot.config.TuyaProperties;
import com.sw.api.modules.iot.tuya.dto.TuyaApiResponse;
import com.sw.api.modules.iot.tuya.dto.TuyaCommandRequest;
import com.sw.api.modules.iot.tuya.dto.TuyaDevice;
import com.sw.api.modules.iot.tuya.dto.TuyaStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Llamadas a Tuya Cloud API. Filtra dispositivos de categoría "cz" (enchufes/socket)
 * y reintenta una vez si el fallo es por token expirado.
 */
@Service
public class TuyaApiClient {

    private static final String SOCKET_CATEGORY = "cz";

    private final WebClient webClient;
    private final TuyaProperties properties;
    private final TuyaSignatureService signatureService;
    private final TuyaAuthService authService;
    private final ObjectMapper objectMapper;

    public TuyaApiClient(WebClient tuyaWebClient, TuyaProperties properties, TuyaSignatureService signatureService,
            TuyaAuthService authService, ObjectMapper objectMapper) {
        this.webClient = tuyaWebClient;
        this.properties = properties;
        this.signatureService = signatureService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    public List<TuyaDevice> getDeviceList() {
        String path = "/v1.0/users/" + properties.getUid() + "/devices";
        TuyaApiResponse<List<TuyaDevice>> response = get(path, new ParameterizedTypeReference<>() {
        });
        List<TuyaDevice> devices = response.result() == null ? List.of() : response.result();
        return devices.stream().filter(d -> SOCKET_CATEGORY.equals(d.category())).toList();
    }

    public TuyaDevice getDeviceDetail(String deviceId) {
        String path = "/v1.0/devices/" + deviceId;
        TuyaApiResponse<TuyaDevice> response = get(path, new ParameterizedTypeReference<>() {
        });
        if (!response.success() || response.result() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El dispositivo no existe en la cuenta Tuya de SpaceShift");
        }
        return response.result();
    }

    public List<TuyaStatus> getDeviceStatus(String deviceId) {
        String path = "/v1.0/devices/" + deviceId + "/status";
        TuyaApiResponse<List<TuyaStatus>> response = get(path, new ParameterizedTypeReference<>() {
        });
        if (!response.success()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error de Tuya: " + response.msg());
        }
        return response.result() == null ? List.of() : response.result();
    }

    public void sendCommand(String deviceId, boolean on) {
        String path = "/v1.0/devices/" + deviceId + "/commands";
        TuyaApiResponse<Object> response = post(path, TuyaCommandRequest.switchCommand(on),
                new ParameterizedTypeReference<>() {
                });
        if (!response.success()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error de Tuya al enviar el comando: " + response.msg());
        }
    }

    private <T> T get(String path, ParameterizedTypeReference<T> type) {
        return execute(HttpMethod.GET, path, null, type, true);
    }

    private <T> T post(String path, Object body, ParameterizedTypeReference<T> type) {
        return execute(HttpMethod.POST, path, body, type, true);
    }

    private <T> T execute(HttpMethod method, String path, Object body, ParameterizedTypeReference<T> type,
            boolean allowRetry) {
        String accessToken = authService.getAccessToken();
        String bodyJson = writeJsonOrNull(body);
        long timestamp = System.currentTimeMillis();
        String sign = signatureService.sign(accessToken, timestamp, method.name(), path, bodyJson);

        WebClient.RequestBodySpec request = webClient.method(method)
                .uri(path)
                .header("client_id", properties.getClientId())
                .header("sign", sign)
                .header("t", String.valueOf(timestamp))
                .header("sign_method", "HMAC-SHA256")
                .header("access_token", accessToken);

        WebClient.RequestHeadersSpec<?> spec = body == null ? request : request.bodyValue(body);

        try {
            T result = spec.retrieve().bodyToMono(type).block();
            if (result == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tuya no devolvió respuesta");
            }
            return result;
        } catch (WebClientResponseException ex) {
            if (allowRetry && ex.getStatusCode().value() == 401) {
                authService.invalidate();
                return execute(method, path, body, type, false);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error de comunicación con Tuya Cloud API");
        }
    }

    private String writeJsonOrNull(Object body) {
        if (body == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo serializar el body para Tuya");
        }
    }
}
