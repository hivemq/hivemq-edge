import { getIncomers, Node, NodeAddChange, XYPosition } from 'reactflow'

import { DataPolicy, DataPolicyValidator, Schema, SchemaReference } from '@/api/__generated__'
import { enumFromStringValue } from '@/utils/types.utils.ts'

import i18n from '@/config/i18n.config.ts'

import {
  DataHubNodeType,
  DataPolicyData,
  DryRunResults,
  SchemaArguments,
  ValidatorData,
  ValidatorType,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import { checkValiditySchema, loadSchema } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { getNodeId, isSchemaNodeType, isValidatorNodeType } from '@datahub/utils/node.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'

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
    // TODO[19466] Id should be user-facing; Need to fix before merging!
    // TODO[NVL] Arguments is not typed on the backend!
    // TODO[NVL] Forcing the version to string is too awkward
    arguments: {
      schemas: schemas.map<SchemaReference>((e) => ({ schemaId: e.id, version: e.data.version.toString() })),
      strategy: validator.data.strategy,
    } as SchemaArguments,
  }
  return { data: operation, node: validator, resources: [...schemaNodes] }
}

export function checkValidityPolicyValidators(
  dataPolicyNode: Node<DataPolicyData>,
  store: WorkspaceState
): DryRunResults<DataPolicyValidator>[] {
  const { nodes, edges } = store

  const incomers = getIncomers(dataPolicyNode, nodes, edges).filter(isValidatorNodeType)

  return incomers.map((validator) => checkValidityPolicyValidator(validator, store))
}

export const loadValidators = (policy: DataPolicy, schemas: Schema[], store: WorkspaceState & WorkspaceAction) => {
  const { onNodesChange, onConnect } = store
  const dataNode = store.nodes.find((node) => node.id === policy.id)
  if (!dataNode)
    throw new Error(
      i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.DATA_POLICY }) as string
    )

  const position: XYPosition = {
    x: dataNode.position.x + CANVAS_POSITION.Validator.x,
    y: dataNode.position.y + CANVAS_POSITION.Validator.y,
  }

  for (const validator of policy.validation?.validators || []) {
    const validatorArguments = validator.arguments as SchemaArguments

    const validatorNode: Node<ValidatorData> = {
      id: getNodeId(),
      type: DataHubNodeType.VALIDATOR,
      position,
      data: {
        strategy: validatorArguments.strategy,
        // @ts-ignore force undefined
        type: enumFromStringValue(ValidatorType, validator.type),
        schemas: validatorArguments.schemas,
      },
    }

    onNodesChange([{ item: validatorNode, type: 'add' } as NodeAddChange])
    onConnect({
      source: validatorNode.id,
      target: dataNode.id,
      sourceHandle: null,
      targetHandle: DataPolicyData.Handle.VALIDATION,
    })

    for (const schemaRef of validatorArguments.schemas) {
      loadSchema(validatorNode, null, 0, schemaRef, schemas, store)
    }
  }
}
