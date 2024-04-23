import { expect } from 'vitest'
import { Node } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataHubNodeType, FunctionData } from '@datahub/types.ts'
import {
  checkValidityJSScript,
  formatScriptName,
  parseScriptName,
} from '@datahub/designer/script/FunctionNode.utils.ts'

describe('checkValidityJSScript', () => {
  it('should return error when not configured', async () => {
    const MOCK_NODE_SCRIPT: Node<FunctionData> = {
      id: 'node-id',
      type: DataHubNodeType.FUNCTION,
      data: {
        type: 'Javascript',
        name: 'my-name',
        version: 1,
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { node, error, data } = checkValidityJSScript(MOCK_NODE_SCRIPT)
    expect(node).toStrictEqual(MOCK_NODE_SCRIPT)
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail:
          'The JS Function is not properly defined. The following properties are missing: name, version, sourceCode',
        id: 'node-id',
        status: 404,
        title: 'FUNCTION',
        type: 'datahub.notConfigured',
      })
    )
  })

  it('should return the complete payload', async () => {
    const MOCK_NODE_SCRIPT: Node<FunctionData> = {
      id: 'node-id',
      type: DataHubNodeType.FUNCTION,
      data: {
        type: 'Javascript',
        name: 'my-name',
        version: 1,
        sourceCode: 'const t=1',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const { node, error, data, resources } = checkValidityJSScript(MOCK_NODE_SCRIPT)
    expect(node).toStrictEqual(MOCK_NODE_SCRIPT)
    expect(data).toEqual(
      expect.objectContaining({
        functionType: 'TRANSFORMATION',
        id: 'my-name',
        source: btoa('const t=1'),
      })
    )
    expect(error).toBeUndefined()
    expect(resources).toBeUndefined()
  })
})

describe('parseScriptName', () => {
  it('should extract the function name from the operation', async () => {
    expect(parseScriptName({ functionId: 'fn:my-function:1', id: 'fakeId', arguments: {} })).toEqual('my-function')
    expect(parseScriptName({ functionId: 'my-raw-id', id: 'fakeId', arguments: {} })).toEqual('my-raw-id')
  })
})

describe('formatScriptName', () => {
  it('should format the id of the script', async () => {
    const MOCK_NODE_SCRIPT: Node<FunctionData> = {
      id: 'node-id',
      type: DataHubNodeType.FUNCTION,
      data: {
        type: 'Javascript',
        name: 'my-name',
        version: 1,
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    expect(formatScriptName(MOCK_NODE_SCRIPT)).toEqual('fn:my-name:latest')
  })
})
