import { expect } from 'vitest'

import { routes } from '@/modules/App/routes.tsx'

describe('createBrowserRouter', () => {
  it('should create the app routes', async () => {
    expect(routes.routes).toStrictEqual(
      expect.arrayContaining([
        expect.objectContaining({
          path: '/',
          children: expect.arrayContaining([
            expect.objectContaining({ path: '' }),
            expect.objectContaining({ path: 'mqtt-bridges/' }),
            expect.objectContaining({ path: 'protocol-adapters/' }),
            expect.objectContaining({ path: 'edge-flow/' }),
            expect.objectContaining({ path: 'event-logs/' }),
            expect.objectContaining({ path: 'namespace/' }),
          ]),
        }),
        expect.objectContaining({ path: '/login' }),
      ])
    )
  })
})
