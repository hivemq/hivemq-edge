import { describe, expect } from 'vitest'
import { getFunctions, initialStore } from './store.utils.ts'
import type { WorkspaceState, WorkspaceStatus } from '@/extensions/datahub/types.ts'
import { DesignerStatus } from '@/extensions/datahub/types.ts'
import type { RJSFSchema } from '@rjsf/utils'
import type { GenericObjectType } from '@rjsf/utils/src/types.ts'

describe('initialStore', () => {
  it('should return the initial state of the store', async () => {
    const store = initialStore()
    expect(store).toStrictEqual<WorkspaceState & WorkspaceStatus>({
      nodes: [],
      edges: [],
      functions: expect.arrayContaining([]),
      name: '',
      status: DesignerStatus.DRAFT,
      type: undefined,
    })
    expect(store.functions).toHaveLength(9)
  })
})

describe('getFunctions', () => {
  it('should return an empty list without a proper schema', async () => {
    // @ts-ignore
    expect(getFunctions(undefined)).toHaveLength(0)
    expect(getFunctions({})).toHaveLength(0)

    expect(
      getFunctions({
        definitions: {},
      })
    ).toStrictEqual([])

    const enums: RJSFSchema = {
      definitions: {
        functionId: {
          properties: {
            functionId: {
              title: 'Function',
              description: 'description',
              enum: ['System.log'],
            },
          },
        },
      },
    }
    expect(getFunctions(enums)).toStrictEqual([{}])
  })

  it('should return a function with a correct schema', async () => {
    const schema: GenericObjectType = {
      definitions: {
        'System.log': {
          title: 'System.log',
          metadata: {
            isTerminal: false,
          },
          description: 'Logs a message on the given level',
          required: ['level', 'message'],
          type: 'object',
          properties: {
            level: {
              type: 'string',
              title: 'Log Level',
              description: 'Specifies the log level of the function in the hivemq.log file',
              enum: ['DEBUG', 'ERROR', 'WARN', 'INFO', 'TRACE'],
            },
            message: {
              type: 'string',
              title: 'Message',
              description:
                'Adds a user-defined string that prints to the log file. For more information, see Example log message',
            },
          },
        },

        functionId: {
          properties: {
            functionId: {
              title: 'Function',
              description: 'description',
              enum: ['System.log'],
            },
          },
        },
      },
    }
    const functions = getFunctions(schema)
    expect(functions).toHaveLength(1)
    expect(functions[0]).toStrictEqual(
      expect.objectContaining({
        functionId: 'System.log',
        metadata: {
          isTerminal: false,
        },
        schema: expect.objectContaining({
          description: 'Logs a message on the given level',
          metadata: {
            isTerminal: false,
          },
          required: ['level', 'message'],
          type: 'object',
        }),
      })
    )
  })
})
