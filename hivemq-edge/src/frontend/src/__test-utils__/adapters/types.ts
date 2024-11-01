// The id of protocol adapters are now all lowercase and unique
export enum MockAdapterType {
  BACNET = 'bacnetip',
  S7 = 's7',
  MODBUS = 'modbus',
  FILE = 'file',
  HTTP = 'http',
  SIMULATION = 'simulation',
  EIP = 'eip',
  OPC_UA = 'opcua',
  ADS = 'ads',
}
