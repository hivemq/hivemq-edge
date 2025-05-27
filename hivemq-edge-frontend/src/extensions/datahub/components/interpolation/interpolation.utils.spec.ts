import { describe, expect } from 'vitest'
import { getItems, parseInterpolations } from '@datahub/components/interpolation/interpolation.utils.ts'
import type { MentionNodeAttrs } from '@datahub/components/interpolation/SuggestionList.tsx'

describe('parseInterpolations', () => {
  it('should convert acceptable placeholders to HTML markup', async () => {
    expect(parseInterpolations(undefined)).toEqual('')
    expect(parseInterpolations('test')).toEqual('test')
    expect(parseInterpolations('test ${122}')).toEqual('test ${122}')
    expect(parseInterpolations('test ${content} after')).toEqual(
      'test <span data-type="mention" data-id="content"></span> after'
    )
    expect(parseInterpolations('test ${content} then ${newItem}')).toEqual(
      'test <span data-type="mention" data-id="content"></span> then <span data-type="mention" data-id="newItem"></span>'
    )
  })
})

const mockItems: MentionNodeAttrs[] = [
  {
    id: '0',
    label: 'clientId',
  },
  {
    id: '1',
    label: 'policyId',
  },
  {
    id: '2',
    label: 'fromState',
  },
  {
    id: '3',
    label: 'toState',
  },
  {
    id: '4',
    label: 'validationResult',
  },
  {
    id: '5',
    label: 'triggerEvent',
  },
  {
    id: '6',
    label: 'timestamp',
  },
  {
    id: '7',
    label: 'topic',
  },
]

describe('getItems', () => {
  it('should return the filtered list of placeholders', async () => {
    expect(getItems('')).toEqual<MentionNodeAttrs[]>(mockItems)
    expect(getItems('tri')).toEqual<MentionNodeAttrs[]>([
      {
        id: '5',
        label: 'triggerEvent',
      },
    ])
    expect(getItems('gger')).toEqual<MentionNodeAttrs[]>([])
    expect(getItems('t')).toEqual<MentionNodeAttrs[]>([
      {
        id: '3',
        label: 'toState',
      },
      {
        id: '5',
        label: 'triggerEvent',
      },
      {
        id: '6',
        label: 'timestamp',
      },
      {
        id: '7',
        label: 'topic',
      },
    ])
  })
})
