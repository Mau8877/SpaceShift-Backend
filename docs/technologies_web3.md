# Tecnologías Web3 - SmartProperty

Este documento detalla las tecnologías elegidas para el subentorno Blockchain del proyecto **SpaceShift**, qué son y el porqué de su elección.

## 1. Solidity (Lenguaje de Programación)
**¿Qué es?** 
Solidity es un lenguaje de programación de alto nivel, estáticamente tipado y orientado a objetos, diseñado específicamente para escribir contratos inteligentes (Smart Contracts) que se ejecutan en la Máquina Virtual de Ethereum (EVM). Es el lenguaje puente entre la lógica de negocio y la inmutabilidad de la blockchain.

**¿Por qué lo usamos (y específicamente la versión 0.8.20)?**
* **Estándar de la Industria:** Es el lenguaje más utilizado y soportado en el ecosistema Web3, garantizando abundancia de documentación y herramientas.
* **Seguridad Nativa (0.8.x):** A partir de la versión 0.8.0, Solidity incluye protección nativa contra errores matemáticos críticos (overflow y underflow), haciendo los contratos inherentemente más seguros frente a hackeos comunes sin necesidad de importar librerías pesadas como `SafeMath`.
* **Eficiencia de Gas (0.8.20):** Esta versión incluye soporte para el opcode `PUSH0`, lo que reduce significativamente el costo de despliegue y ejecución (gas) del contrato, haciéndolo ideal para redes de Capa 2 (Layer 2) como Arbitrum, Optimism o Polygon, donde las transacciones rápidas y baratas son fundamentales para el ecosistema IoT del proyecto.

## 2. Hardhat (Framework y Motor Local)
**¿Qué es?** 
Hardhat es el entorno de desarrollo y framework de pruebas más popular y robusto para Ethereum. No es un lenguaje, es la caja de herramientas que orquesta la compilación, despliegue, pruebas y depuración del código Solidity.

**¿Por qué lo usamos?**
* **Hardhat Network (Motor Local):** Su característica más poderosa. Hardhat levanta instantáneamente un nodo local "falso" de blockchain en la memoria de la computadora de desarrollo. Esto permite al equipo compilar, desplegar y simular transacciones de contratos de manera instantánea y gratuita, sin depender de redes de prueba públicas ni gastar criptomonedas reales.
* **Integración con TypeScript:** Permite escribir scripts de despliegue y pruebas unitarias utilizando TypeScript, ofreciendo tipado estricto y autocompletado, lo que previene errores al interactuar con el contrato desde un entorno JavaScript/TypeScript (como el que se usará más adelante en el frontend u oráculos IoT).
* **Console.log en Solidity:** Ofrece la capacidad única de usar `console.log` directamente dentro del código del contrato inteligente, lo que facilita enormemente la depuración del estado del contrato durante el desarrollo.

## 3. Ethers.js
**¿Qué es?** 
Es una librería de JavaScript/TypeScript diseñada para interactuar de forma completa y compacta con la blockchain de Ethereum y su ecosistema.

**¿Por qué lo usamos?**
Actúa como el cliente de la blockchain en el lado del código de pruebas (dentro de Hardhat). Permite crear "billeteras virtuales" (Signers) para firmar transacciones de prueba y enviar comandos (como la creación de un nuevo contrato de inmueble) desde TypeScript hacia la red local generada por Hardhat.

## 4. Chai
**¿Qué es?** 
Es una librería de aserciones para Node.js y el navegador que se puede emparejar placenteramente con cualquier framework de pruebas.

**¿Por qué lo usamos?**
Dentro del entorno Hardhat, Chai (con su plugin específico para Hardhat) nos permite escribir tests altamente legibles. Permite declarar condiciones matemáticas estrictas (ej. *esperar que la llamada a la función X emita el evento Y*, o *esperar que la variable de estado Z sea igual a W*), validando así el comportamiento exacto de la HU 1 y HU 2 antes de cualquier despliegue en producción.
