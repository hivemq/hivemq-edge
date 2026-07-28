import { useEffect } from 'react'
import { useInfiniteQuery, type QueryKey } from '@tanstack/react-query'
import type { ApiError } from '@/api/__generated__'

/**
 * A page of a cursor-paginated Data Hub listing endpoint: a set of items plus an
 * optional `_links.next` URL pointing at the following page.
 */
interface PaginatedList<T> {
  items?: Array<T>
  _links?: { next?: string }
}

/**
 * Extract the `cursor` query parameter from a `_links.next` URL.
 *
 * The Data Hub list endpoints return the next page as a full URL in `_links.next`;
 * the endpoints themselves expect just the opaque `cursor` token. `undefined` means
 * there is no next page (stop paginating).
 */
export const extractCursor = (next: string | undefined): string | undefined => {
  if (!next) return undefined
  try {
    // `next` may be absolute or relative; a base makes URL parsing tolerant of both.
    return new URL(next, 'http://localhost').searchParams.get('cursor') ?? undefined
  } catch {
    return undefined
  }
}

/**
 * Fetch *every* page of a cursor-paginated Data Hub listing and expose it as a single
 * flattened `{ items }` object — the same shape the callers already consume.
 *
 * The Data Hub list endpoints are cursor-paginated (default page size 50). Fetching only
 * the first page hides any item beyond the 50th (EDG-844). This follows `_links.next`
 * until it is absent, so callers always see the full list without dealing with pages.
 *
 * `useInfiniteQuery` fetches only the first page on its own; the effect below keeps
 * pulling subsequent pages until the cursor is exhausted.
 *
 * @param queryKey  React Query key for this listing (without any cursor component).
 * @param fetchPage Fetches one page for the given cursor (`undefined` = first page).
 */
export const usePaginatedList = <T>(queryKey: QueryKey, fetchPage: (cursor?: string) => Promise<PaginatedList<T>>) => {
  const query = useInfiniteQuery<PaginatedList<T>, ApiError, { items: Array<T> }>({
    queryKey,
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => fetchPage(pageParam as string | undefined),
    getNextPageParam: (lastPage) => extractCursor(lastPage._links?.next),
    // Flatten every fetched page back into the single `{ items }` shape callers expect.
    select: (data) => ({ items: data.pages.flatMap((page) => page.items ?? []) }),
  })

  const { hasNextPage, isFetchingNextPage, fetchNextPage } = query
  useEffect(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage()
  }, [hasNextPage, isFetchingNextPage, fetchNextPage])

  return query
}
