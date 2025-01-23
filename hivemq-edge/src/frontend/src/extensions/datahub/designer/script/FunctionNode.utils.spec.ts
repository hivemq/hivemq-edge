import { expect } from 'vitest'
import type { Connection, Node, NodeAddChange } from 'reactflow'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import type { PolicyOperation } from '@/api/__generated__'
import { Script } from '@/api/__generated__'
import type { FunctionData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import {
  checkValidityJSScript,
  formatScriptName,
  loadScripts,
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

describe('loadScripts', () => {
  const scripts: Script[] = [
    {
      id: 'script1',
      version: 1,
      createdAt: '2024-04-22T09:34:51.765Z',
      functionType: Script.functionType.TRANSFORMATION,
      // A Base64 encoding of a dummy script
      source:
        'Ci8qKgogKgogKiBAcGFyYW0ge09iamVjdH0gcHVibGlzaAogKiBAcGFyYW0ge3N0cmluZ30gcHVibGlzaC50b3BpYyAgICBUaGUgTVFUVCB0b3BpYyB0aGF0IGlzIGN1cnJlbnRseSBzcGVjaWZpZWQgZm9yIHRoaXMgUFVCTElTSCBwYWNrZXQuCiAqIEBwYXJhbSB7T2JqZWN0fSBwdWJsaXNoLnBheWxvYWQgIEEgbGlzdCBvZiB0aGUgbmFtZSBhbmQgdmFsdWUgb2YgYWxsIHVzZXIgcHJvcGVydGllcyBvZiB0aGUgTVFUVCA1IFBVQkxJU0ggcGFja2V0LiBUaGlzIHNldHRpbmcgaGFzIG5vIGVmZmVjdCBvbiBNUVRUIDMgY2xpZW50cy4KICogQHBhcmFtIHtSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+W119IHB1Ymxpc2gudXNlclByb3BlcnRpZXMgVGhlIEpTT04gb2JqZWN0IHJlcHJlc2VudGF0aW9uIG9mIHRoZSBkZXNlcmlhbGl6ZWQgTVFUVCBwYXlsb2FkLgogKiBAcGFyYW0ge09iamVjdH0gY29udGV4dAogKiBAcGFyYW0ge1JlY29yZDxzdHJpbmcsIHN0cmluZz5bXX0gY29udGV4dC5hcmd1bWVudHMgIFRoZSBhcmd1bWVudHMgcHJvdmlkZWQgdG8gdGhlIHNjcmlwdC4gQ3VycmVudGx5LCBhcmd1bWVudHMgY2FuIG9ubHkgYmUgcHJvdmlkZWQgdmlhIGEgZGF0YSBwb2xpY3kuCiAqIEBwYXJhbSB7c3RyaW5nfSBjb250ZXh0LnBvbGljeUlkIFRoZSBwb2xpY3kgaWQgb2YgdGhlIHBvbGljeSBmcm9tIHdoaWNoIHRoZSB0cmFuc2Zvcm1hdGlvbiBmdW5jdGlvbiBpcyBjYWxsZWQuCiAqIEBwYXJhbSB7c3RyaW5nfSBjb250ZXh0LmNsaWVudElkIFRoZSBjbGllbnQgSWQgb2YgdGhlIGNsaWVudCBmcm9tIHdoaWNoIHRoZSBNUVRUIHB1Ymxpc2ggd2FzIHNlbnQuCiAqIEByZXR1cm5zIHtPYmplY3R9IFRoZSBwdWJsaXNoLW9iamVjdCBpcyBwYXNzZWQgYXMgYSBwYXJhbWV0ZXIgaW50byB0aGUgdHJhbnNmb3JtIGZ1bmN0aW9uLiBUaGUgc2FtZSBvYmplY3Qgb3IgYSBuZXcgb2JqZWN0IGlzIHJldHVybmVkIGFzIHRoZSB0cmFuc2Zvcm1lZCBvYmplY3QuCiAqLwpmdW5jdGlvbiB0cmFuc2Zvcm0ocHVibGlzaCwgY29udGV4dCkgewogIHJldHVybiBwdWJsaXNoCn0KCg==',
    },
  ]

  const node: Node = {
    id: 'node-id',
    data: {},
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should return nodes', () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node_c4aabfe4-9f76-409a-b98d-04cbd98fd647',
        functionId: 'fn:script1:latest',
        arguments: {},
      },
    ]

    expect(loadScripts(node, 0, policyOperations, scripts)).toStrictEqual<(NodeAddChange | Connection)[]>([
      expect.objectContaining<NodeAddChange>({
        item: {
          data: {
            name: 'script1',
            sourceCode: expect.stringContaining('function transform(publish, context)'),
            type: 'Javascript',
            version: 1,
          },
          id: 'script1',
          position: {
            x: -320,
            y: 0,
          },
          type: DataHubNodeType.FUNCTION,
        },
        type: 'add',
      }),
      expect.objectContaining({
        source: 'script1',
        target: 'node-id',
      }),
    ])
  })

  it('should be used in the right context', () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node_c4aabfe4-9f76-409a-b98d-04cbd98fd647',
        functionId: 'fn:script0001:latest',
        arguments: {},
      },
    ]
    expect(() => loadScripts(node, 0, policyOperations, scripts)).toThrow('Cannot find the JS Function node')
  })
})
