import { useCallback, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useLocation } from 'react-router'

import type { HiveMqClient } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { STORE_WORKSPACE_KEY } from '@/modules/Workspace/types.ts'

/**
 * The six lists the workspace graph is built from, fetched together and all-or-nothing: if one
 * fails, nothing is reconciled.
 *
 * That is the decisive safety property. Each list succeeds or fails independently, and the graph
 * builder only tracks a combined "is loading" — it has no combined error state. So a reconcile that
 * ran on a partial result could not tell "the server has no combiners" from "the combiner call
 * failed", and would delete every combiner from the user's workspace during a momentary blip.
 *
 * Each carries its own fetcher rather than relying on `refetchQueries`, which only refreshes queries
 * the cache already holds — a list nothing has mounted yet would be silently skipped, and the reload
 * would report success having asked the server nothing.
 */
interface WorkspaceQuery {
  queryKey: string[]
  /** The reload only passes the payload through to the cache, so its shape is the caller's concern. */
  queryFn: () => Promise<unknown>
}

const workspaceQueries = (appClient: HiveMqClient): WorkspaceQuery[] =>
  [
    // The shapes must match what each list hook stores, or the reload would corrupt the cache it is
    // meant to refresh: adapters and bridges unwrap to their `items`, the others keep the envelope.
    {
      queryKey: [QUERY_KEYS.ADAPTERS],
      queryFn: async () => (await appClient.protocolAdapters.getAdapters()).items,
    },
    { queryKey: [QUERY_KEYS.PROTOCOLS], queryFn: () => appClient.protocolAdapters.getAdapterTypes() },
    { queryKey: [QUERY_KEYS.BRIDGES], queryFn: async () => (await appClient.bridges.getBridges()).items },
    { queryKey: [QUERY_KEYS.LISTENERS], queryFn: () => appClient.gatewayEndpoint.getListeners() },
    { queryKey: [QUERY_KEYS.COMBINER], queryFn: () => appClient.combiners.getCombiners() },
    { queryKey: [QUERY_KEYS.ASSET_MAPPER], queryFn: () => appClient.pulse.getAssetMappers() },
  ] as const

/**
 * The status queries that poll on a timer and write into node `data`.
 *
 * They must be stopped for the duration of a reload: a blocking overlay stops the *user*, not the
 * *application*, and a status write landing mid-reconcile would interleave with it.
 */
const POLLED_STATUS_QUERY_KEYS = [
  [QUERY_KEYS.ADAPTERS, QUERY_KEYS.CONNECTION_STATUS],
  [QUERY_KEYS.BRIDGES, QUERY_KEYS.CONNECTION_STATUS],
  [QUERY_KEYS.PULSE_STATUS],
] as const

const MAX_ATTEMPTS = 5
const RETRY_DELAY_MS = 400

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export enum ReloadPhase {
  IDLE = 'IDLE',
  FETCHING = 'FETCHING',
  RECONCILING = 'RECONCILING',
}

/**
 * Detects an open configuration panel.
 *
 * Every workspace detail panel is a child route of `/workspace/` — `/workspace/adapter/<type>/<id>`,
 * `/workspace/combiner/<id>`, and so on. So the current path says both *that* a panel is open and
 * *which* entity it is bound to, with no extra bookkeeping.
 *
 * Any open panel counts as potentially unsaved: dirty-form tracking barely exists in this module, so
 * the reload cannot ask a panel whether it holds unsaved edits and gets to assume the worst.
 */
export const useIsPanelOpen = () => {
  const { pathname } = useLocation()
  const normalised = pathname.replace(/\/+$/, '')
  return normalised.startsWith('/workspace/') && normalised !== '/workspace'
}

export const useReloadWorkspace = () => {
  const queryClient = useQueryClient()
  const appClient = useHttpClient()
  const [phase, setPhase] = useState<ReloadPhase>(ReloadPhase.IDLE)

  /**
   * Refetch every query the graph is built from, all-or-nothing, retrying the whole set.
   *
   * Resolves once the query cache holds a complete, fresh set; throws when it could not get one, in
   * which case the caller reconciles nothing.
   */
  const refetchAll = useCallback(async () => {
    let lastError: unknown

    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        // fetchQuery runs the fetcher and rejects when it fails, so Promise.all gives the
        // all-or-nothing outcome directly. Written into the cache only once every one resolved.
        const results = await Promise.all(
          workspaceQueries(appClient).map(({ queryKey, queryFn }) =>
            queryClient
              .fetchQuery({ queryKey: [...queryKey], queryFn, staleTime: 0, retry: false })
              .then((data) => ({ queryKey, data }))
          )
        )

        results.forEach(({ queryKey, data }) => queryClient.setQueryData([...queryKey], data))
        return
      } catch (error) {
        lastError = error
      }

      if (attempt < MAX_ATTEMPTS) await delay(RETRY_DELAY_MS * attempt)
    }

    throw lastError ?? new Error('Could not reload the workspace')
  }, [appClient, queryClient])

  /**
   * The safe reload: refresh from the server and repair the graph, keeping the user's arrangement.
   *
   * `reconcile` is supplied by the caller, which holds the freshly built graph — this hook owns the
   * fetching and the polling suspension, not the graph shape.
   */
  const reload = useCallback(
    async (reconcile: () => void) => {
      // Stop the status pollers, and cancel anything already in flight so a response that departed
      // before now cannot land mid-reconcile.
      POLLED_STATUS_QUERY_KEYS.forEach((queryKey) => {
        queryClient.cancelQueries({ queryKey: [...queryKey] })
      })

      try {
        setPhase(ReloadPhase.FETCHING)
        await refetchAll()

        setPhase(ReloadPhase.RECONCILING)
        reconcile()
      } finally {
        // Always, so a failed reload does not leave status frozen.
        setPhase(ReloadPhase.IDLE)
        POLLED_STATUS_QUERY_KEYS.forEach((queryKey) => {
          queryClient.invalidateQueries({ queryKey: [...queryKey] })
        })
      }
    },
    [queryClient, refetchAll]
  )

  /**
   * The destructive reload: throw away every trace of client state and start the app again.
   *
   * This is the escape hatch for a workspace corrupted beyond what reconciling can repair — today it
   * means opening browser developer tools and clearing storage by hand. It wipes storage wholesale
   * rather than clearing an enumerated list of keys: a curated list has to be kept in step as new
   * stores appear, and forgetting one reintroduces exactly the staleness this button exists to clear.
   *
   * The bearer token lives in the same storage, so this signs the user out. That is accepted and the
   * button says so.
   */
  const resetEverything = useCallback(() => {
    queryClient.clear()

    try {
      window.localStorage.clear()
      window.sessionStorage.clear()
    } catch (error) {
      // Storage can throw when the browser blocks site data; the reload below still gets a clean
      // in-memory start, so this should not stop the operation.
      console.warn(`Could not clear browser storage, reloading anyway (${STORE_WORKSPACE_KEY})`, error)
    }

    window.location.replace('/')
  }, [queryClient])

  return { phase, isReloading: phase !== ReloadPhase.IDLE, reload, resetEverything }
}

export default useReloadWorkspace
