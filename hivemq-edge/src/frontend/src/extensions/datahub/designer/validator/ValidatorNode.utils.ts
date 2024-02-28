import { getIncomers, Node } from 'reactflow'

import { DataPolicyValidator, Schema, SchemaReference } from '@/api/__generated__'
import { DataHubNodeType, DataPolicyData, DryRunResults, ValidatorData, WorkspaceState } from '@datahub/types.ts'
import { checkValiditySchema } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { isSchemaNodeType, isValidatorNodeType } from '@datahub/utils/node.utils.ts'

export function checkValidityPolicyValidator(
  validator: Node<ValidatorData>,
  store: WorkspaceState
): DryRunResults<DataPolicyValidator, Schema> {
  const { nodes, edges } = store

  const schemas = getIncomers(validator, nodes, edges).filter(isSchemaNodeType)

  if (!schemas.length) {
    return {
      node: validator,
      error: PolicyCheckErrors.notConnected(DataHubNodeType.SCHEMA, validator),
    }
  }

  const schemaNodes = schemas.map((e) => checkValiditySchema(e))
  const operation: DataPolicyValidator = {
    type: validator.data.type,
    // extracting the value from the schema data
    // TODO[19466] Id should be user-facing; Need to fix before merging!
    arguments: schemas.map<SchemaReference>((e) => ({ schemaId: e.id, version: e.data.version })),
  }
  return { data: operation, node: validator, resources: [...schemaNodes] }
}

export function checkValidityPolicyValidators(
  dataPolicyNode: Node<DataPolicyData>,
  store: WorkspaceState
): DryRunResults<DataPolicyValidator>[] {
  const { nodes, edges } = store

  const incomers = getIncomers(dataPolicyNode, nodes, edges).filter(isValidatorNodeType)

  if (!incomers.length) {
    return [
      {
        node: dataPolicyNode,
        error: PolicyCheckErrors.notConnected(DataHubNodeType.VALIDATOR, dataPolicyNode),
      },
    ]
  }

  return incomers.map((validator) => checkValidityPolicyValidator(validator, store))
}
