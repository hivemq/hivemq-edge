import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockScript } from './__handlers__'
import { useGetScript } from '@datahub/api/hooks/DataHubScriptsService/useGetScript.ts'

describe('useGetScript', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetScript(mockScript.id), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        createdAt: '2023-10-13T11:51:24.234Z',
        description: 'this is a description',
        functionType: 'TRANSFORMATION',
        id: 'my-script-id',
        source:
          'IlxuZnVuY3Rpb24gY29udmVydChmYWhyZW5oZWl0KSB7XG4gICAgcmV0dXJuIE1haC5mbG9vcigoZmFocmVuaGVpdCAtIDMyKSAqIDUvOSk7XG59XG5cbmZ1bmN0aW9uIHRyYW5zZm9ybShwdWJsaXNoLCBjb250ZXh0KSB7XG4gICAgIHB1Ymxpc2gucGF5bG9hZCA9IHtcbiAgICAgICAgXCJjZWxzaXVzXCI6IGNvbnZlcnQocHVibGlzaC5wYXlsb2FkLmZhaHJlbmhlaXQpLFxuICAgICAgICBcInRpbWVzdGFtcFwiOiBwdWJsaXNoLnBheWxvYWQudGltZXN0YW1wXG4gICAgfVxuICAgIHJldHVybiBwdWJsaXNoO1xufVxuIg==',
        version: 1,
      })
    )
  })
})
