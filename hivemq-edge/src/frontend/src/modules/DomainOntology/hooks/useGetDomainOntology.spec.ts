import { beforeEach, expect, vi } from 'vitest'

import { renderHook, waitFor } from '@testing-library/react'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import type { SouthboundMapping } from '@/api/__generated__'
import { type NorthboundMapping, QoS, type TopicFilter, type DomainTag } from '@/api/__generated__'

import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { handlers as protocolHandler } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as tahHandlers } from '@/api/hooks/useDomainModel/__handlers__'
import { handlers as topicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'
import { handlers as bridgeHandlers } from '@/api/hooks/useGetBridges/__handlers__'
import type { BridgeSubscription } from '@/modules/DomainOntology/types.ts'

const successListOf = <T>(payload: T) =>
  expect.objectContaining({
    data: expect.objectContaining({
      items: [
        expect.objectContaining<T>({
          ...payload,
        }),
      ],
    }),
    error: null,
    isSuccess: true,
    isError: false,
  })

describe('useGetDomainOntology', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...protocolHandler, ...tahHandlers, ...topicFilterHandlers, ...bridgeHandlers)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should return a valid topicFilters payload', async () => {
    const { result } = renderHook(useGetDomainOntology, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.topicFilters).toStrictEqual(
      successListOf<TopicFilter>({
        description: 'This is a topic filter',
        topicFilter: 'a/topic/+/filter',
      })
    )
  })

  it('should return a valid tags payload', async () => {
    const { result } = renderHook(useGetDomainOntology, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.tags).toStrictEqual(
      successListOf<DomainTag>({
        definition: {
          endIdx: 1,
          startIdx: 0,
        },
        name: 'test/tag1',
      })
    )
  })

  it('should return a valid northMappings payload', async () => {
    const { result } = renderHook(useGetDomainOntology, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.northMappings).toStrictEqual(
      successListOf<NorthboundMapping>({
        includeTagNames: true,
        includeTimestamp: true,
        maxQoS: QoS.AT_MOST_ONCE,
        messageExpiryInterval: -1000,
        tagName: 'my/tag',
        topic: 'my/topic',
      })
    )
  })

  it('should return a valid southMappings payload', async () => {
    const { result } = renderHook(useGetDomainOntology, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.southMappings).toStrictEqual(
      successListOf<SouthboundMapping>({
        fieldMapping: {
          instructions: [
            {
              destination: 'lastName',
              source: 'dropped-property',
            },
          ],
        },
        tagName: 'my/tag',
        topicFilter: 'my/filter',
      })
    )
  })

  it('should return a valid bridgeSubscriptions payload', async () => {
    const { result } = renderHook(useGetDomainOntology, { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.bridgeSubscriptions).toStrictEqual(
      expect.objectContaining<BridgeSubscription>({
        mappings: [
          ['#', 'prefix/{#}/bridge/${bridge.name}'],
          ['root/topic/ref/1', 'prefix/{#}/bridge/${bridge.name}'],
        ],
        topicFilters: ['#', 'root/topic/ref/1'],
        topics: ['prefix/{#}/bridge/${bridge.name}'],
      })
    )
  })
})
