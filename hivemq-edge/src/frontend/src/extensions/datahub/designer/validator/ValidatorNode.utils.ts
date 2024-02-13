import { getIncomers, Node } from 'reactflow'
import {
  DataHubNodeType,
  DataPolicyData,
  DryRunResults,
  SchemaData,
  ValidatorData,
  WorkspaceState,
} from '@datahub/types.ts'
import { DataPolicyValidator, Schema } from '@/api/__generated__'
import { checkValiditySchema } from '@datahub/designer/schema/SchemaNode.utils.ts'

export function checkValidityPolicyValidator(
  validator: Node<ValidatorData>,
  store: WorkspaceState
): DryRunResults<DataPolicyValidator, Schema> {
  const { nodes, edges } = store

  ///////// Check the serializers
  const schemas = getIncomers(validator, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.SCHEMA
  ) as Node<SchemaData>[]

  if (!schemas.length) {
    return {
      node: validator,
      error: {
        title: validator.type as string,
        status: 404,
        detail: 'No schema connected to the operation node',
        type: 'datahub.notConnected',
        id: validator.id,
      },
    }
  }

  const schemaNodes = schemas.map((e) => checkValiditySchema(e))
  // TODO[NVL] if valid, needs to be broken down into three pipeline operation. with reference to the appropriate resource
  const operation: DataPolicyValidator = {
    type: validator.data.type,
    arguments: validator.data.schemas,
  }
  return { data: operation, node: validator, resources: [...schemaNodes] }
}

export function checkValidityPolicyValidators(
  dataPolicyNode: Node<DataPolicyData>,
  store: WorkspaceState
): DryRunResults<DataPolicyValidator>[] {
  const { nodes, edges } = store

  const incomers = getIncomers(dataPolicyNode, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.VALIDATOR
  ) as Node<ValidatorData>[]

  if (!incomers.length) {
    return [
      {
        node: dataPolicyNode,
        error: {
          title: dataPolicyNode.type as string,
          status: 404,
          detail: 'No validator to the data policy',
          type: 'datahub.notConnected',
          id: dataPolicyNode.id,
        },
      },
    ]
  }

  return incomers.map((validator) => checkValidityPolicyValidator(validator, store))
}
