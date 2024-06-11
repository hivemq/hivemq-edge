import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { usePostAuthentication } from '@/api/hooks/usePostAuthentication/index.ts'

describe('usePostAuthentication', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(usePostAuthentication, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()

    act(() => {
      result.current.mutateAsync({ password: 'password', userName: 'username' })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        token:
          'eyJraWQiOiIwMDAwMSIsImFsZyI6IlJTMjU2In0.eyJqdGkiOiI2TVExbWtPeFRqUmdFMS1BRE9GNGRRIiwiaWF0IjoxNjg1MDE3OTY3LCJhdWQiOiJIaXZlTVEtRWRnZS1BcGkiLCJpc3MiOiJIaXZlTVEtRWRnZSIsImV4cCI6MTY4NTAxOTc2NywibmJmIjoxNjg1MDE3ODQ3LCJzdWIiOiJhZG1pbiIsInJvbGVzIjpbImFkbWluIl19.o4jlGf7dDBOmfkr46u_MTQA-C3I2ZkHyrljbfNPeqax_EI54vigHH4viVk0yehH47BivbGLp9CmsTujre-G7hrFHliXsbZ81erJ3IgWpSwq1GZdvEFynVabm26QO8fKm3CnpwJDIfPYKOuTEXYpWlDe2pc9_r6oJSllb1UP30SP4UVxC9rIUm4zGbAVNFizaqLo_V92cLW2pv0T2SChJK6pDYSj19RWhoftEES9G-hd8b--_eB2AkflUseUlDDIsGMxQxTdMfR4l6q2C1RNiGdub3KVpnHN86sF_u8zQb4QdmtcXqS4imm1BiZZm67hZQYtX12cLy6sf11Dh8b09Gg',
      })
    })
  })
})
