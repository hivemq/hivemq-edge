import '@testing-library/jest-dom'
import { beforeAll, afterEach, afterAll } from 'vitest'
import { randomUUID } from 'node:crypto'
import { server } from './msw/mockServer.ts'

// Crypto missing from jsdom
window.crypto.randomUUID = randomUUID

// Monaco is now bundled rather than fetched from a CDN, so its modules execute under jsdom when a
// spec pulls in the code editor. jsdom does not implement the deprecated execCommand clipboard API
// that Monaco feature-detects, so provide the stub it probes for.
if (typeof document !== 'undefined' && typeof document.queryCommandSupported !== 'function') {
  document.queryCommandSupported = () => false
}

// Establish API mocking before all tests.
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'warn' })
})

// Reset any request handlers that we may add during the tests,
// so they don't affect other tests.
afterEach(() => server.resetHandlers())

// Clean up after the tests are finished.
afterAll(() => server.close())
