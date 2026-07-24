import { useCallback, useEffect, useRef } from 'react'
import config from '@/config'

const OIDC_LOGIN_PATH = '/api/v1/auth/oidc/login'
const POPUP_FEATURES = 'width=520,height=640,menubar=no,toolbar=no,location=no,status=no'
const OIDC_RESULT_MESSAGE_TYPE = 'oidc-result'

interface OidcResultMessage {
  type?: string
  token?: string
  errorCode?: string
}

/**
 * Drives the OIDC "Login with SSO" flow from the browser.
 *
 * Opens the backend's `/api/v1/auth/oidc/login` endpoint in a popup; the backend redirects to the
 * configured Identity Provider, and its callback posts an `oidc-result` message back to this window.
 * The returned `startLogin` resolves with the HiveMQ Edge JWT, or rejects with a stable error code
 * (the backend's `errorCode`, or `popup-blocked` / `popup-closed` / `unmounted`).
 *
 * Security: a message is only accepted when it comes from our own origin, originates from the popup
 * we opened (`event.source`), and carries the expected message type.
 */
export const useOidcLogin = () => {
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

    return new Promise<string>((resolve, reject) => {
      const popup = window.open(`${config.apiBaseUrl}${OIDC_LOGIN_PATH}`, 'hivemq-edge-oidc-login', POPUP_FEATURES)
      if (!popup) {
        reject(new Error('popup-blocked'))
        return
      }
      pendingRef.current = { resolve, reject }
      popupRef.current = popup

      // Detect a user closing the popup before completing the flow.
      pollRef.current = window.setInterval(() => {
        if (popup.closed) {
          settle({ error: new Error('popup-closed') })
        }
      }, 500)
    })
  }, [settle])

  return { startLogin }
}
