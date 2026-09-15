export { MOCK_PROTOCOL_ADS, MOCK_ADAPTER_ADS, MOCK_SCHEMA_ADS } from './ads.ts'
export { MOCK_PROTOCOL_BACNET_IP } from './bacnetip.ts'
export { MOCK_PROTOCOL_DATABASES } from './databases.ts'
export { MOCK_PROTOCOL_EIP, MOCK_ADAPTER_EIP, MOCK_SCHEMA_EIP } from './eip.ts'
export { MOCK_PROTOCOL_FILE, MOCK_ADAPTER_FILE, MOCK_SCHEMA_FILE } from './file.ts'
export { MOCK_PROTOCOL_HTTP, MOCK_ADAPTER_HTTP, MOCK_SCHEMA_HTTP } from './http.ts'
export { MOCK_PROTOCOL_MODBUS, MOCK_ADAPTER_MODBUS, MOCK_SCHEMA_MODBUS } from './modbus.ts'
export { MOCK_PROTOCOL_MTCONNECT } from './mtconnect.ts'
export { MOCK_PROTOCOL_OPC_UA, MOCK_ADAPTER_OPC_UA, MOCK_SCHEMA_OPC_UA } from './opc-ua.ts'
// `opc-ua-tls.ts` is deliberately NOT re-exported here. Everything that imports this barrel — including
// the workspace E2E specs, which only want `MOCK_ADAPTER_OPC_UA` — would otherwise also load the TLS
// fixture and the generated schema behind it. That extra module measurably shifts start-up timing in
// `wizard-create-group.spec.cy.ts`: the canvas is fitted before layout settles, an adapter node ends up
// under the selection panel, and the click that should deselect it hits the panel instead. Import the
// fixture directly from '@/__test-utils__/adapters/opc-ua-tls'.
export { MOCK_PROTOCOL_S7, MOCK_ADAPTER_S7, MOCK_SCHEMA_S7 } from './s7.ts'
export { MOCK_PROTOCOL_SIMULATION, MOCK_ADAPTER_SIMULATION, MOCK_SCHEMA_SIMULATION } from './simulation.ts'
