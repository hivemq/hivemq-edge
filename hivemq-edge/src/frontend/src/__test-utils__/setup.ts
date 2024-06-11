import '@testing-library/jest-dom'
import { beforeAll, afterEach, afterAll } from 'vitest'
import { randomUUID } from 'node:crypto'
import { server } from './msw/mockServer.ts'

// Crypto missing from jsdom
window.crypto.randomUUID = randomUUID

// Establish API mocking before all tests.
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'warn' })
})

// Reset any request handlers that we may add during the tests,
// so they don't affect other tests.
afterEach(() => server.resetHandlers())

// Clean up after the tests are finished.
afterAll(() => server.close())
