import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { GatewayConfiguration } from '@/api/__generated__'
import { useGetConfiguration } from '@/api/hooks/useFrontendServices/useGetConfiguration.ts'
import { handlers, MOCK_PRE_LOGIN_NOTICE } from './__handlers__'

describe('useGetConfiguration', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetConfiguration, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<GatewayConfiguration>(
      expect.objectContaining({
        cloudLink: expect.objectContaining({ displayText: 'HiveMQ Cloud' }),
        ctas: expect.objectContaining({}),
        documentationLink: expect.objectContaining({}),
        environment: expect.objectContaining({}),
        extensions: expect.objectContaining({}),
        firstUseInformation: expect.objectContaining({}),
        gitHubLink: expect.objectContaining({}),
        hivemqId: 'my-current-installation-id',
        modules: expect.objectContaining({}),
        preLoginNotice: expect.objectContaining(MOCK_PRE_LOGIN_NOTICE),
      })
    )
  })
})
