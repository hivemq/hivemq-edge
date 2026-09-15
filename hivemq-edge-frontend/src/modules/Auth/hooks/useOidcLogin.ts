import { useCallback, useEffect, useRef } from 'react'
import { useSimpleHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

const POPUP_FEATURES = 'width=520,height=640,menubar=no,toolbar=no,location=no,status=no'
const POPUP_NAME = 'hivemq-edge-oidc-login'
const OIDC_RESULT_MESSAGE_TYPE = 'oidc-result'

interface OidcResultMessage {
  type?: string
  token?: string
  errorCode?: string
}

/**
 * Drives the OIDC "Login with SSO" flow from the browser.
 *
 * `startLogin` first asks the backend for the Identity Provider authorization URL, then opens that URL
 * in the login popup. The popup is opened blank synchronously on the user's click (so the browser does
 * not block it) and navigated once the URL arrives; if the request fails, the popup is closed and the
 * promise rejects, so a start-time error never leaves a popup stranded on a raw response. The IdP
 * callback posts an `oidc-result` message back to this window, which resolves with the HiveMQ Edge JWT
 * or rejects with a stable error code (`popup-blocked` / `popup-closed` / `unmounted`, or the backend's).
 *
 * Security: a message is only accepted when it comes from our own origin, originates from the popup
 * we opened (`event.source`), and carries the expected message type.
 */
export const useOidcLogin = () => {
  const appClient = useSimpleHttpClient()
  // Track the in-flight resolve/reject so the message listener can settle the current attempt.
  const pendingRef = useRef<{ resolve: (token: string) => void; reject: (reason: Error) => void } | null>(null)
  const popupRef = useRef<Window | null>(null)
  const pollRef = useRef<number | undefined>(undefined)

  // Single cleanup path: clears the poll, closes the popup, and forgets the attempt. Every exit
  // (success, failure, popup closed, unmount) settles the promise exactly once.
  const settle = useCallback((outcome: { token: string } | { error: Error }) => {
    if (pollRef.current !== undefined) {
      window.clearInterval(pollRef.current)
      pollRef.current = undefined
    }
    const pending = pendingRef.current
    pendingRef.current = null
    popupRef.current?.close()
    popupRef.current = null
    if (!pending) return
    if ('token' in outcome) pending.resolve(outcome.token)
    else pending.reject(outcome.error)
  }, [])

  useEffect(() => {
    const onMessage = (event: MessageEvent) => {
      // Only trust messages from our own origin (the callback page targets its own origin) that come
      // from the popup we opened, and that carry our message type.
      if (event.origin !== window.location.origin) return
      if (popupRef.current && event.source !== popupRef.current) return
      const data = event.data as OidcResultMessage
      if (!data || data.type !== OIDC_RESULT_MESSAGE_TYPE) return

      if (typeof data.token === 'string') settle({ token: data.token })
      else settle({ error: new Error(data.errorCode ?? 'login-failed') })
    }

    window.addEventListener('message', onMessage)
    return () => {
      window.removeEventListener('message', onMessage)
      // Unmounting mid-flight must settle the promise, not leave it pending forever.
      settle({ error: new Error('unmounted') })
    }
  }, [settle])

  const startLogin = useCallback((): Promise<string> => {
    // Only one login may be in flight: the popup uses a fixed window name, so a second start would
    // reuse the same window and strand the first promise and its poll timer.
    if (pendingRef.current) return Promise.reject(new Error('login-already-in-progress'))

    // Open the popup synchronously on the click, blank, so the browser does not treat it as a blocked
    // pop-up. It is navigated to the authorization URL once the backend returns it, or closed on error.
    const popup = window.open('', POPUP_NAME, POPUP_FEATURES)
    if (!popup) {
      return Promise.reject(new Error('popup-blocked'))
    }

    return new Promise<string>((resolve, reject) => {
      pendingRef.current = { resolve, reject }
      popupRef.current = popup

      // Detect a user closing the popup before completing the flow.
      pollRef.current = window.setInterval(() => {
        if (popup.closed) {
          settle({ error: new Error('popup-closed') })
        }
      }, 500)

      appClient.authentication
        .oidcLogin()
        .then(({ authorizeUrl }) => {
          // The attempt may already have been settled (popup closed, unmounted) while the request ran.
          if (popupRef.current !== popup) return
          popup.location.href = authorizeUrl
        })
        .catch((error) => {
          // A start-time failure (OIDC not configured, IdP unreachable) settles here, closing the popup,
          // instead of leaving it on a raw error response.
          settle({ error: error instanceof Error ? error : new Error('login-failed') })
        })
    })
  }, [appClient, settle])

  return { startLogin }
}
