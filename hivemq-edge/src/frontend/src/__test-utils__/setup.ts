import '@testing-library/jest-dom'
import { beforeAll, afterEach, afterAll } from 'vitest'
import { server } from './msw/mockServer.ts'

// Establish API mocking before all tests.
beforeAll(() => {
  server.listen()
  console.log('XXXXXXXX', import.meta.env.MODE, import.meta.env.VITE_API_BASE_URL)
})

// Reset any request handlers that we may add during the tests,
// so they don't affect other tests.
afterEach(() => server.resetHandlers())

// Clean up after the tests are finished.
afterAll(() => server.close())
