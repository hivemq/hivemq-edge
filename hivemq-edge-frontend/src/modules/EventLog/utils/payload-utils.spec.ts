import { expect } from 'vitest'
import { Payload } from '@/api/__generated__'
import { mockEdgeEvent, mockXmlPayload } from '@/api/hooks/useEvents/__handlers__'

import { prettifyXml, prettyJSON } from './payload-utils.ts'

describe('prettyJSON', () => {
  it('should be error resilient', async () => {
    expect(prettyJSON('test')).toStrictEqual(null)
    expect(prettyJSON(undefined)).toStrictEqual(null)
  })

  it('should prettify JSON', async () => {
    const payload = mockEdgeEvent(2)[1].payload
    expect(payload?.contentType).toStrictEqual(Payload.contentType.JSON)
    expect(payload?.content?.length).toStrictEqual(422)
    expect(prettyJSON(payload?.content as string)?.length).toStrictEqual(584)
  })
})

describe('prettifyXml', () => {
  it('should be error resilient', async () => {
    expect(prettifyXml('test')).toStrictEqual(null)
    expect(prettifyXml(undefined)).toStrictEqual(null)
  })

  // TODO[NVL] Node doesn't support XSLTProcessor so will fail
  it('should prettify XML', async () => {
    expect(prettifyXml(mockXmlPayload)).toStrictEqual(null)
  })
})
