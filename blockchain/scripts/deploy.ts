import { ethers } from "hardhat";

async function main() {
  const SmartProperty = await ethers.getContractFactory("SmartProperty");
  const contract = await SmartProperty.deploy();
  await contract.waitForDeployment();

  console.log("Contrato SmartProperty desplegado exitosamente en:", await contract.getAddress());
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
