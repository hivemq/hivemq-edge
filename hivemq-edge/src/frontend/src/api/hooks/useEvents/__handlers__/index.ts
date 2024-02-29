import { rest } from 'msw'
import { Event, EventList, Payload, TypeIdentifier } from '@/api/__generated__'

import { DateTime } from 'luxon'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

const makeID = (type: TypeIdentifier.type, inc: number): TypeIdentifier => ({
  identifier: `${type}-${inc}`,
  type: type,
})

const lorem =
  'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. ' +
  'At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,'

export const mockXmlPayload = [
  '<root><node/>',
  ' <Sale price="100.00">',
  '   <segment>  0 LO4394L 14SEP PHXORD GK1</segment>',
  '   <segment>  1 LO 999L 14SEP PHXORD GK1</segment>',
  '   <segment>  2 LO 789L 15SEP WAWODS GK1</segment>',
  '   <segment> 12 LO4394T 14SEP PHXORD GK1</segment>',
  '   <name>Vasya Pupkin</name>' + ' </Sale>',
  '</root>',
].join('\n')

const sourceKeys = Object.keys(TypeIdentifier.type)
const severityKeys = Object.keys(Event.severity)
const maxEvents = 200

const contentType = [
  {
    content: mockXmlPayload,
    contentType: Payload.contentType.XML,
  },
  { content: JSON.stringify(mockBridge), contentType: Payload.contentType.JSON },
  {
    content: 'test,id,ff',
    contentType: Payload.contentType.CSV,
  },
]

export const mockEdgeEvent = (n = maxEvents): Event[] =>
  Array.from(Array(n), (_, x) => ({
    identifier: makeID(TypeIdentifier.type.EVENT, x),

    severity: severityKeys[x % severityKeys.length] as Event.severity,
    message: lorem,
    payload: contentType[x % 3],

    source: makeID(sourceKeys[x % sourceKeys.length] as TypeIdentifier.type, x),
    associatedObject: makeID(sourceKeys[x % sourceKeys.length] as TypeIdentifier.type, x),
    created: DateTime.fromISO('2023-10-13T11:51:24.234')
      .plus({ minutes: x % 100 })
      .toISO({ format: 'basic' }) as string,
    timestamp: DateTime.fromISO('2023-10-13T11:51:24.234')
      .plus({ minutes: x % 100 })
      .toUnixInteger(),
  }))

export const handlers = [
  rest.get('**/management/events', (_, res, ctx) => {
    return res(ctx.json<EventList>({ items: [...mockEdgeEvent()] }), ctx.status(200))
  }),
]
