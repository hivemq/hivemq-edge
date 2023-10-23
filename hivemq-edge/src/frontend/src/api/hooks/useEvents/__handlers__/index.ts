import { rest } from 'msw'
import { Event, EventList, Payload, TypeIdentifier } from '@/api/__generated__'

import { DateTime } from 'luxon'

const lorem =
  'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren,'

function makeID(length: number) {
  let result = ''
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  const charactersLength = characters.length
  let counter = 0
  while (counter < length) {
    result += characters.charAt(Math.floor(Math.random() * charactersLength))
    counter += 1
  }
  return result
}

const gg = Object.keys(Event.severity)
const l = gg.length

export const mockEdgeEvent: Event[] = Array.from(Array(50000), (_, x) => ({
  identifier: { identifier: makeID(12), type: TypeIdentifier.type.EVENT },
  // severity: EventSeverity[Object.keys(EventSeverity)[Math.floor(Math.random() * Object.keys(EventSeverity).length)]],

  severity: gg[Math.floor(Math.random() * l)] as Event.severity,
  message: lorem,
  payload: { content: JSON.stringify({ a: 1 }), contentType: Payload.contentType.JSON },
  source: { identifier: makeID(12), type: TypeIdentifier.type.EVENT },
  associatedObject: { identifier: makeID(12), type: TypeIdentifier.type.EVENT },
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
    return res(ctx.json<EventList>({ items: [...mockEdgeEvent] }), ctx.status(200))
  }),
]
