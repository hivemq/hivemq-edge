import { rest } from 'msw'
import { Event, EventList, Payload, TypeIdentifier } from '@/api/__generated__'

import { DateTime } from 'luxon'

const makeID = (type: TypeIdentifier.type, inc: number): TypeIdentifier => ({
  identifier: `${type}-${inc}`,
  type: type,
})

const lorem =
  'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. ' +
  'At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,'

const sourceKeys = Object.keys(TypeIdentifier.type)
const severityKeys = Object.keys(Event.severity)
const maxEvents = 200

export const mockEdgeEvent = (n = maxEvents): Event[] =>
  Array.from(Array(n), (_, x) => ({
    identifier: makeID(TypeIdentifier.type.EVENT, x),

    severity: severityKeys[x % severityKeys.length] as Event.severity,
    message: lorem,
    payload: { content: JSON.stringify({ a: 1 }), contentType: Payload.contentType.JSON },
    source: makeID(sourceKeys[x % sourceKeys.length] as TypeIdentifier.type, x),
    associatedObject: makeID(sourceKeys[x % sourceKeys.length] as TypeIdentifier.type, x),
    created: DateTime.fromISO('2023-10-13T11:51:24.234+01')
      .plus({ minutes: x % 100 })
      .toISO({ format: 'basic' }) as string,
    timestamp: DateTime.fromISO('2023-10-13T11:51:24.234+01')
      .plus({ minutes: x % 100 })
      .toUnixInteger(),
  }))

export const handlers = [
  rest.get('**/management/events', (_, res, ctx) => {
    console.log('XXXX', 200)
    return res(ctx.json<EventList>({ items: [...mockEdgeEvent()] }), ctx.status(200))
  }),
]
