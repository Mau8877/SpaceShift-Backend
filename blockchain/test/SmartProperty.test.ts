import { expect } from "chai";
import { ethers } from "hardhat";

describe("SmartProperty", function () {
  let smartProperty: any;
  let owner: any;
  let addr1: any;

  beforeEach(async function () {
    [owner, addr1] = await ethers.getSigners();
    const SmartPropertyFactory = await ethers.getContractFactory("SmartProperty");
    smartProperty = await SmartPropertyFactory.deploy();
  });

  describe("Deployment", function () {
    it("Deberia asignar el owner correcto", async function () {
      expect(await smartProperty.owner()).to.equal(owner.address);
    });
  });

  describe("HU 1: Gestion de Contratos Base", function () {
    it("Deberia crear un contrato de inmueble correctamente", async function () {
      const propertyId = "PROP-101";
      await expect(smartProperty.createPropertyContract(propertyId, addr1.address))
        .to.emit(smartProperty, "ContractCreated")
        .withArgs(propertyId, addr1.address);

      const property = await smartProperty.properties(propertyId);
      expect(property.propertyId).to.equal(propertyId);
      expect(property.currentTenant).to.equal(addr1.address);
      expect(property.isActive).to.be.true;
    });

    it("Deberia fallar si alguien mas que el dueno intenta crear un contrato", async function () {
      const propertyId = "PROP-102";
      await expect(
        smartProperty.connect(addr1).createPropertyContract(propertyId, addr1.address)
      ).to.be.revertedWith("No eres el dueno");
    });
  });

  describe("HU 2: Reglas Automatizadas del Contrato", function () {
    const propertyId = "PROP-RULES-1";

    beforeEach(async function () {
      // Creamos un contrato antes de cada prueba de reglas para tener un estado limpio
      await smartProperty.createPropertyContract(propertyId, addr1.address);
    });

    it("Deberia permitir al dueno agregar una regla automatizada", async function () {
      const conditionTime = Math.floor(Date.now() / 1000) + 3600; // Dentro de 1 hora (timestamp)
      
      await expect(smartProperty.addAutomatedRule(propertyId, "UNLOCK_DOOR", conditionTime, false))
        .to.emit(smartProperty, "RuleAdded")
        .withArgs(propertyId, "UNLOCK_DOOR", conditionTime);

      const rulesCount = await smartProperty.getRulesCount(propertyId);
      expect(rulesCount).to.equal(1);
    });

    it("Deberia permitir al inquilino (tenant) agregar una regla automatizada", async function () {
      const conditionTime = Math.floor(Date.now() / 1000) + 7200; // Dentro de 2 horas
      
      await expect(smartProperty.connect(addr1).addAutomatedRule(propertyId, "TURN_ON_LIGHTS", conditionTime, true))
        .to.emit(smartProperty, "RuleAdded")
        .withArgs(propertyId, "TURN_ON_LIGHTS", conditionTime);
        
      const rulesCount = await smartProperty.getRulesCount(propertyId);
      expect(rulesCount).to.equal(1);
    });

    it("Deberia fallar si un tercero intenta agregar una regla", async function () {
      const [owner, addr1, addr2] = await ethers.getSigners();
      const conditionTime = Math.floor(Date.now() / 1000) + 3600;

      await expect(
        smartProperty.connect(addr2).addAutomatedRule(propertyId, "UNLOCK_DOOR", conditionTime, false)
      ).to.be.revertedWith("No tienes permisos sobre esta propiedad");
    });
  });
});
