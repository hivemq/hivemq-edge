import { expect } from 'vitest'
import type { Node } from '@xyflow/react'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import type { DataPolicyData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.DATA_POLICY,
  data: { id: 'my-policy-id' },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

describe('PolicyCheckErrors', () => {
  it('should render not validated error', async () => {
    expect(PolicyCheckErrors.notValidated(MOCK_NODE_DATA_POLICY, 'because of that')).toStrictEqual(
      expect.objectContaining({
        detail: 'The Data Policy is not properly configured: because of that',
        id: 'node-id',
        status: 404,
        title: 'DATA_POLICY',
        type: 'datahub.notConfigured',
      })
    )
  })

  it('should render internal error', async () => {
    expect(PolicyCheckErrors.internal(MOCK_NODE_DATA_POLICY, 'because of that')).toStrictEqual(
      expect.objectContaining({
        detail: 'Encountered an error while processing Data Policy: because of that',
        id: 'node-id',
        status: 404,
        title: 'DATA_POLICY',
        type: 'datahub.notConfigured',
      })
    )
    expect(PolicyCheckErrors.internal(MOCK_NODE_DATA_POLICY, new Error('because of that'))).toStrictEqual(
      expect.objectContaining({
        detail: 'Encountered an error while processing Data Policy: because of that',
        id: 'node-id',
        status: 404,
        title: 'DATA_POLICY',
        type: 'datahub.notConfigured',
      })
    )
  })
})
