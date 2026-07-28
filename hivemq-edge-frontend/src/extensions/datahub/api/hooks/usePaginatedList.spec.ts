import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { Script, ScriptList } from '@/api/__generated__'

import { extractCursor, usePaginatedList } from '@datahub/api/hooks/usePaginatedList.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

describe('extractCursor', () => {
  it('should return the cursor query param from an absolute next URL', () => {
    expect(extractCursor('https://broker/api/v1/data-hub/scripts?limit=50&cursor=abc123')).toStrictEqual('abc123')
  })

  it('should return the cursor query param from a relative next URL', () => {
    expect(extractCursor('/api/v1/data-hub/scripts?cursor=page2')).toStrictEqual('page2')
  })

  it('should return undefined when there is no next URL or no cursor', () => {
    expect(extractCursor(undefined)).toBeUndefined()
    expect(extractCursor('/api/v1/data-hub/scripts?limit=50')).toBeUndefined()
  })
})

describe('usePaginatedList', () => {
  const script = (id: string): Script => ({ id, version: 1, source: '', functionType: undefined as never })

  it('should follow the cursor and return items from every page (EDG-844)', async () => {
    // Page 1 carries a `_links.next` cursor; page 2 does not, so paging stops there.
    server.use(
      http.get('*/data-hub/scripts', ({ request }) => {
        const cursor = new URL(request.url).searchParams.get('cursor')
        if (!cursor) {
          return HttpResponse.json<ScriptList>({
            items: [script('page1-a'), script('page1-b')],
            _links: { next: 'http://broker/api/v1/data-hub/scripts?cursor=CURSOR_2' },
          })
        }
        return HttpResponse.json<ScriptList>({ items: [script('page2-a')] })
      })
    )

    const { result } = renderHook(
      () => {
        const appClient = useHttpClient()
        return usePaginatedList<Script>([DATAHUB_QUERY_KEYS.SCRIPTS, 'test'], (cursor) =>
          appClient.dataHubScripts.getAllScripts(undefined, undefined, undefined, undefined, cursor)
        )
      },
      { wrapper }
    )

    // All pages fetched: hook stops paging once `_links.next` is absent.
    await waitFor(() =>
      expect(result.current.data?.items.map((s) => s.id)).toStrictEqual(['page1-a', 'page1-b', 'page2-a'])
    )
    await waitFor(() => expect(result.current.isFetching).toBeFalsy())

    expect(result.current.data?.items.map((s) => s.id)).toStrictEqual(['page1-a', 'page1-b', 'page2-a'])
  })

  it('should return a single page unchanged when there is no next cursor', async () => {
    server.use(http.get('*/data-hub/scripts', () => HttpResponse.json<ScriptList>({ items: [script('only')] })))

    const { result } = renderHook(
      () => {
        const appClient = useHttpClient()
        return usePaginatedList<Script>([DATAHUB_QUERY_KEYS.SCRIPTS, 'single'], (cursor) =>
          appClient.dataHubScripts.getAllScripts(undefined, undefined, undefined, undefined, cursor)
        )
      },
      { wrapper }
    )

    await waitFor(() => expect(result.current.isSuccess).toBeTruthy())
    expect(result.current.data?.items.map((s) => s.id)).toStrictEqual(['only'])
  })
})
