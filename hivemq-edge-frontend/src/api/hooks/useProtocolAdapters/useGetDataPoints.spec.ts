import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetDataPoints } from '@/api/hooks/useProtocolAdapters/useGetDataPoints.ts'

describe('useGetDataPoints', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetDataPoints(true, 'adapterId'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.items).toStrictEqual([
      expect.objectContaining({
        children: [
          {
            children: [
              {
                description:
                  'New range of formal shirts are designed keeping you in mind. With fits and styling that will make you stand apart',
                id: 'ns=3;i=1001',
                name: 'Constant',
                value: 'ns=3;i=1001',
                nodeType: 'VALUE',
                selectable: true,
              },
              {
                description: 'Carbonite web goalkeeper gloves are ergonomically designed to give easy fit',
                id: 'ns=3;i=1002',
                name: 'Counter',
                value: 'ns=3;i=1002',
                nodeType: 'VALUE',
                selectable: true,
              },
              {
                description:
                  'The automobile layout consists of a front-engine design, with transaxle-type transmissions mounted at the rear of the engine and four wheel drive',
                id: 'ns=3;i=1003',
                name: 'Random',
                value: 'ns=3;i=1003',
                nodeType: 'VALUE',
                selectable: true,
              },
              {
                description:
                  'Ergonomic executive chair upholstered in bonded black leather and PVC padded seat and back for all-day comfort and support',
                id: 'ns=3;i=1004',
                name: 'SawTooth',
                value: 'ns=3;i=1004',
                nodeType: 'VALUE',
                selectable: true,
              },
              {
                children: [
                  {
                    description:
                      'The beautiful range of Apple Naturalé that has an exciting mix of natural ingredients. With the Goodness of 100% Natural Ingredients',
                    id: 'ns=3;i=1010',
                    name: 'Triangle',
                    value: 'ns=3;i=1010',
                    nodeType: 'VALUE',
                    selectable: true,
                  },
                  {
                    description:
                      'New range of formal shirts are designed keeping you in mind. With fits and styling that will make you stand apart',
                    id: 'ns=3;i=1011',
                    name: 'Circle',
                    value: 'ns=3;i=1011',
                    nodeType: 'VALUE',
                    selectable: true,
                  },
                ],
                description:
                  'Andy shoes are designed to keeping in mind durability as well as trends, the most stylish range of shoes & sandals',
                id: 'ns=3;i=1007',
                name: 'NewValues',
                value: 'ns=3;i=1007',
                nodeType: 'FOLDER',
                selectable: false,
              },
              {
                description:
                  'The beautiful range of Apple Naturalé that has an exciting mix of natural ingredients. With the Goodness of 100% Natural Ingredients',
                id: 'ns=3;i=1005',
                name: 'Sinusoid',
                value: 'ns=3;i=1005',
                nodeType: 'VALUE',
                selectable: true,
              },
              {
                description:
                  'New range of formal shirts are designed keeping you in mind. With fits and styling that will make you stand apart',
                id: 'ns=3;i=1006',
                value: 'ns=3;i=1006',
                name: 'Square',
                nodeType: 'VALUE',
                selectable: true,
              },
            ],
            description:
              'Andy shoes are designed to keeping in mind durability as well as trends, the most stylish range of shoes & sandals',
            id: 'ns=3;s=85/0:Simulation',
            value: 'ns=3;s=85/0:Simulation',
            name: 'Simulation',
            nodeType: 'FOLDER',
            selectable: false,
          },
        ],
        description:
          'The Apollotech B340 is an affordable wireless mouse with reliable connectivity, 12 months battery life and modern design',
        id: 'i=85',
        value: 'i=85',
        name: 'Object',
        nodeType: 'FOLDER',
        selectable: false,
      }),
    ])
  })
})
