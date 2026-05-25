// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract SmartProperty {
    address public owner;
    
    struct Rule {
        string action; // Ej: "UNLOCK_DOOR", "TURN_ON_LIGHTS"
        uint256 conditionTime; // Horario específico (timestamp) o condición
        bool isRecurring;
        bool isActive;
    }

    struct Property {
        string propertyId;
        address currentTenant;
        bool isActive;
    }

    mapping(string => Property) public properties;
    // propertyId => lista de reglas automatizadas
    mapping(string => Rule[]) public propertyRules;

    event ContractCreated(string indexed propertyId, address indexed tenant);
    event RuleAdded(string indexed propertyId, string action, uint256 conditionTime);

    modifier onlyOwner() {
        require(msg.sender == owner, "No eres el dueno");
        _;
    }

    modifier onlyTenantOrOwner(string memory _propertyId) {
        require(
            msg.sender == owner || msg.sender == properties[_propertyId].currentTenant,
            "No tienes permisos sobre esta propiedad"
        );
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function createPropertyContract(
        string memory _propertyId, 
        address _tenant
    ) public onlyOwner {
        properties[_propertyId] = Property({
            propertyId: _propertyId,
            currentTenant: _tenant,
            isActive: true
        });
        emit ContractCreated(_propertyId, _tenant);
    }

    // Nueva función para agregar reglas (Historia de usuario: Reglas Automatizadas)
    function addAutomatedRule(
        string memory _propertyId,
        string memory _action,
        uint256 _conditionTime,
        bool _isRecurring
    ) public onlyTenantOrOwner(_propertyId) {
        require(properties[_propertyId].isActive, "Propiedad inactiva");
        
        propertyRules[_propertyId].push(Rule({
            action: _action,
            conditionTime: _conditionTime,
            isRecurring: _isRecurring,
            isActive: true
        }));

        emit RuleAdded(_propertyId, _action, _conditionTime);
    }
    
    function getRulesCount(string memory _propertyId) public view returns (uint256) {
        return propertyRules[_propertyId].length;
    }
}
