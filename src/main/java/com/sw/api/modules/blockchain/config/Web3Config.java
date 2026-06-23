package com.sw.api.modules.blockchain.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Slf4j
@Configuration
public class Web3Config {

    @Value("${spaceshift.blockchain.node-url:http://127.0.0.1:8545}")
    private String nodeUrl;

    @Value("${spaceshift.blockchain.private-key:0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80}")
    private String adminPrivateKey;

    @Bean
    public Web3j web3j() {
        log.info("Inicializando cliente Web3j conectando a nodo: {}", nodeUrl);
        return Web3j.build(new HttpService(nodeUrl));
    }

    @Bean
    public Credentials credentials() {
        if (adminPrivateKey == null || adminPrivateKey.trim().isEmpty()) {
            throw new IllegalArgumentException("La llave privada del administrador blockchain no está configurada");
        }
        Credentials credentials = Credentials.create(adminPrivateKey);
        log.info("Credenciales Web3 cargadas. Dirección de administrador: {}", credentials.getAddress());
        return credentials;
    }
}
