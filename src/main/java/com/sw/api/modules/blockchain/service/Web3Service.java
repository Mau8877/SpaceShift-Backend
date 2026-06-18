package com.sw.api.modules.blockchain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class Web3Service {

    @Value("${spaceshift.blockchain.contract-address:}")
    private String contractAddress;

    @Value("${spaceshift.blockchain.master-secret:SpaceShiftSuperSecretMasterKey}")
    private String masterSecret;

    @Value("${spaceshift.blockchain.chain-id:80002}")
    private long chainId;

    private final Web3j web3j;
    private final Credentials credentials;

    public String registerPropertyContractOnChain(String propertyId, String tenantWalletAddress) {
        if (contractAddress == null || contractAddress.isEmpty()) {
            log.warn("Direccion del Smart Contract no configurada. Saltando registro en Blockchain.");
            return null;
        }

        try {
            log.info("Iniciando registro on-chain de contrato. Propiedad: {}, Inquilino: {}", propertyId,
                    tenantWalletAddress);

            // Obtener nonce para la transaccion
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            // Construir la llamada a la funcion: createPropertyContract(string, address)
            Function function = new Function(
                    "createPropertyContract",
                    Arrays.asList(new Utf8String(propertyId), new Address(tenantWalletAddress)),
                    Collections.emptyList());

            String encodedFunction = FunctionEncoder.encode(function);

            // Obtener el precio del gas de forma dinámica desde el nodo de la red
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            // Multiplicar por 1.25 para asegurar que supere el mínimo y evitar variaciones
            // del mercado
            gasPrice = gasPrice.multiply(BigInteger.valueOf(125)).divide(BigInteger.valueOf(100));
            BigInteger gasLimit = BigInteger.valueOf(300_000L);

            // Crear transaccion sin procesar
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    contractAddress,
                    encodedFunction);

            // Firmar transaccion con proteccion EIP-155 (Chain ID)
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            // Enviar transaccion
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

            if (ethSendTransaction.hasError()) {
                throw new RuntimeException(
                        "Error en transaccion blockchain: " + ethSendTransaction.getError().getMessage());
            }

            String transactionHash = ethSendTransaction.getTransactionHash();
            log.info("Contrato registrado exitosamente. Hash de transaccion: {}", transactionHash);
            return transactionHash;

        } catch (Exception e) {
            log.error("Fallo al registrar contrato en blockchain: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar el contrato en Blockchain: " + e.getMessage(), e);
        }
    }

    public String generateDeterministicWalletAddress(UUID userId) {
        if (userId == null)
            return null;
        try {
            byte[] seed = Hash.sha3((masterSecret + userId.toString()).getBytes(StandardCharsets.UTF_8));
            ECKeyPair ecKeyPair = ECKeyPair.create(seed);
            String address = Numeric.prependHexPrefix(Keys.getAddress(ecKeyPair));
            log.info("Generada wallet determinista para usuario {}: {}", userId, address);
            return address;
        } catch (Exception e) {
            log.error("Error al generar wallet determinista para {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
