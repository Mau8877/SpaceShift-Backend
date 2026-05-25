# Documentación del Módulo Blockchain (SpaceShift-Backend)

Este directorio contiene la subestructura encargada de la lógica descentralizada (Web3/Blockchain) del proyecto **SpaceShift**. Se utiliza para crear, compilar, probar y eventualmente desplegar contratos inteligentes en una red blockchain (como Ethereum, Polygon, o redes de prueba).

## Arquitectura y Herramientas
La arquitectura de este módulo se basa en el entorno de desarrollo **Hardhat**, usando **TypeScript** como lenguaje principal de configuración y pruebas, y **Solidity** para los contratos inteligentes.

*   **Hardhat**: Framework de desarrollo para Ethereum. Permite compilar, probar y desplegar contratos.
*   **Solidity (`0.8.20`)**: Lenguaje de programación usado para escribir el contrato inteligente (`SmartProperty.sol`).
*   **Ethers.js / Hardhat Toolbox**: Librerías que proporcionan utilidades para interactuar con los contratos desde el código (especialmente útil en los tests).
*   **TypeScript**: Agrega tipado estático a las pruebas y a la configuración, haciéndolas más robustas.

## Estructura de Directorios y Archivos

### Carpetas Principales

*   `contracts/`: 
    *   **Función**: Contiene el código fuente de los contratos inteligentes escritos en Solidity.
    *   **Archivos clave**: 
        *   `SmartProperty.sol`: Es el contrato principal. Su función es registrar en la blockchain información inmutable sobre una propiedad (como el `propertyId`) y asociarla de forma segura y transparente con un inquilino (`tenant`). Sólo el propietario (`owner`) del contrato puede registrar estas propiedades, evitando falsificaciones.
*   `test/`: 
    *   **Función**: Contiene los scripts de pruebas unitarias o de integración para asegurar que los contratos funcionan como se espera antes de enviarlos a producción.
    *   **Archivos clave**: 
        *   `SmartProperty.test.ts`: Son las pruebas escritas en TypeScript usando Mocha/Chai (las librerías por defecto de Hardhat). Comprueban cosas como: *¿Se asigna el propietario correctamente? ¿Puede alguien que no es propietario crear un contrato de propiedad?*, etc.
*   `node_modules/`:
    *   **Función**: Es el entorno virtual local de Node.js. Aquí se instalan todas las dependencias del proyecto (Hardhat, TypeScript, Ethers, etc.) aislando este módulo de otros proyectos en tu computadora o del backend principal. *(Esta carpeta se genera al ejecutar `npm install`)*.
*   `artifacts/` y `cache/`: 
    *   **Función**: Carpetas autogeneradas por Hardhat.
    *   `artifacts/`: Contiene los archivos JSON compilados (los ABI y Bytecode) de tus contratos, que luego usarás para conectarte al contrato desde el frontend o el backend.
    *   `cache/`: Guarda información interna de Hardhat para acelerar compilaciones futuras.
*   `typechain-types/`:
    *   **Función**: Carpeta autogenerada por la herramienta TypeChain. Contiene tipos de TypeScript generados a partir de tus contratos inteligentes. Esto te permite tener autocompletado y seguridad de tipos cuando interactúas con los contratos en tus tests o en tu backend.

### Archivos de Configuración

*   `hardhat.config.ts`: 
    *   **Función**: Es el archivo de configuración central de Hardhat. Aquí se define la versión del compilador de Solidity (en este caso `0.8.20`), y es donde en el futuro podrías agregar las configuraciones de redes (como una testnet Sepolia o Mainnet) y las claves privadas para los despliegues.
*   `package.json` y `package-lock.json`: 
    *   **Función**: Archivos propios de Node.js y npm. Llevan el registro de qué dependencias/paquetes necesita este proyecto para funcionar (ej. `@nomicfoundation/hardhat-toolbox`, `typescript`, etc.) y mantienen un registro exacto de las versiones instaladas (`package-lock.json`).
*   `tsconfig.json`: 
    *   **Función**: Archivo de configuración del compilador de TypeScript. Define cómo se deben compilar los archivos `.ts` (como tus tests o el config de hardhat) hacia JavaScript para que Node.js los pueda ejecutar internamente.

## Comandos de Ejecución

Para interactuar con el entorno de blockchain, debes abrir una terminal y asegurarte de estar posicionado en la carpeta `SpaceShift-Backend/blockchain`. Luego, puedes utilizar los siguientes comandos:

*   **Compilar los Contratos:**
    ```bash
    npx hardhat compile
    ```
    *Uso:* Ejecútalo cada vez que realices cambios en los archivos `.sol` dentro de la carpeta `contracts/`. Esto generará los artefactos y tipos necesarios para el backend.

*   **Ejecutar Todas las Pruebas:**
    ```bash
    npx hardhat test
    ```
    *Uso:* Ejecuta la batería completa de pruebas unitarias para validar que la lógica del contrato y las reglas automatizadas funcionen según las historias de usuario.

*   **Ejecutar una Prueba Específica:**
    ```bash
    npx hardhat test test/SmartProperty.test.ts
    ```
