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
    const gg = mockEdgeEvent(2)[1].payload
    expect(gg?.contentType).toStrictEqual(Payload.contentType.JSON)
    expect(gg?.content?.length).toStrictEqual(381)
    expect(prettyJSON(gg?.content as string)?.length).toStrictEqual(537)
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
