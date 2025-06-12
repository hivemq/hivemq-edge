import type { Connection, Node, NodeAddChange, XYPosition } from '@xyflow/react'
import { getIncomers } from '@xyflow/react'

import type { DataPolicy, PolicySchema, SchemaReference } from '@/api/__generated__'
import { DataPolicyValidator } from '@/api/__generated__'
import { enumFromStringValue } from '@/utils/types.utils.ts'

import i18n from '@/config/i18n.config.ts'

import type { DryRunResults, SchemaArguments, ValidatorData, WorkspaceState } from '@datahub/types.ts'
import { DataHubNodeType, DataPolicyData, ResourceWorkingVersion } from '@datahub/types.ts'
import { checkValiditySchema, loadSchema } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { getNodeId, isSchemaNodeType, isValidatorNodeType } from '@datahub/utils/node.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'
import { SCRIPT_FUNCTION_LATEST } from '@datahub/utils/datahub.utils.ts'

export function checkValidityPolicyValidator(
  validator: Node<ValidatorData>,
  store: WorkspaceState
): DryRunResults<DataPolicyValidator, PolicySchema> {
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
    // TODO[NVL] Arguments is not typed on the backend!
    arguments: {
      schemas: schemas.map<SchemaReference>((schema) => {
        const version =
          schema.data.version === ResourceWorkingVersion.DRAFT ||
          schema.data.version === ResourceWorkingVersion.MODIFIED
            ? SCRIPT_FUNCTION_LATEST
            : schema.data.version.toString()
        return { schemaId: schema.data.name, version }
      }),
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

export const loadValidators = (policy: DataPolicy, schemas: PolicySchema[], dataPolicyNode: Node<DataPolicyData>) => {
  const position: XYPosition = {
    x: dataPolicyNode.position.x + CANVAS_POSITION.Validator.x,
    y: dataPolicyNode.position.y + CANVAS_POSITION.Validator.y,
  }

  const newNodes: (NodeAddChange | Connection)[] = []
  for (const validator of policy.validation?.validators || []) {
    const validatorArguments = validator.arguments as SchemaArguments

    const type = enumFromStringValue(DataPolicyValidator.type, validator.type.toUpperCase())
    if (!type)
      throw new Error(
        i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.VALIDATOR }) as string
      )

    const validatorNode: Node<ValidatorData> = {
      id: getNodeId(DataHubNodeType.VALIDATOR),
      type: DataHubNodeType.VALIDATOR,
      position,
      data: {
        strategy: validatorArguments.strategy,
        type,
        schemas: validatorArguments.schemas,
      },
    }

    newNodes.push({ item: validatorNode, type: 'add' })
    newNodes.push({
      source: validatorNode.id,
      target: dataPolicyNode.id,
      sourceHandle: null,
      targetHandle: DataPolicyData.Handle.VALIDATION,
    })

    let nb = 0
    for (const schemaRef of validatorArguments.schemas) {
      const schemaNodes = loadSchema(validatorNode, null, nb++, schemaRef, schemas)
      newNodes.push(...schemaNodes)
    }
  }

  return newNodes
}
