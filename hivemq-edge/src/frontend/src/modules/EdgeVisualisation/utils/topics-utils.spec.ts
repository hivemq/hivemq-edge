import { expect } from 'vitest'
import { getAdapterTopics, getBridgeTopics } from '@/modules/EdgeVisualisation/utils/topics-utils.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { TopicFilter } from '@/modules/EdgeVisualisation/types.ts'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('getBridgeTopics', () => {
  it('should extract topics from a Bridge', async () => {
    const actual = getBridgeTopics(mockBridge)
    const expected: { local: TopicFilter[]; remote: TopicFilter[] } = {
      local: [{ topic: '#' }],
      remote: [{ topic: 'root/topic/act/1' }],
    }

    expect(actual).toStrictEqual(expected)
  })
})

describe('getAdapterTopics', () => {
  it('should extract topics from adapters', async () => {
    const actual = getAdapterTopics(mockAdapter)
    const expected: TopicFilter[] = [{ topic: 'root/topic/ref/1' }, { topic: 'root/topic/ref/2' }]

    expect(actual).toStrictEqual(expected)
  })
})
