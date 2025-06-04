import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockSchemaTempHumidity } from './__handlers__'
import { useGetSchema } from '@datahub/api/hooks/DataHubSchemasService/useGetSchema.ts'

describe('useGetSchema', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetSchema(mockSchemaTempHumidity.id), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        createdAt: '2023-10-13T11:51:24.234Z',
        id: 'my-schema-id',
        schemaDefinition:
          'eyJ0aXRsZSI6IlZhbGlkIFNlbnNvciBEYXRhIiwiZGVzY3JpcHRpb24iOiJBIHNjaGVtYSB0aGF0IG1hdGNoZXMgdGhlIHRlbXBlcmF0dXJlIGFuZCBodW1pZGl0eSB2YWx1ZXMgb2YgYW55IG9iamVjdCIsInJlcXVpcmVkIjpbInRlbXBlcmF0dXJlIiwiaHVtaWRpdHkiXSwidHlwZSI6Im9iamVjdCIsInByb3BlcnRpZXMiOnsidGVtcGVyYXR1cmUiOnsidHlwZSI6Im51bWJlciIsIm1pbmltdW0iOjIwLCJtYXhpbXVtIjo3MH0sImh1bWlkaXR5Ijp7InR5cGUiOiJudW1iZXIiLCJtaW5pbXVtIjo2NSwibWF4aW11bSI6MTAwfX19',
        type: 'JSON',
        version: 1,
      })
    )
  })
})
