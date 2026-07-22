import { useCallback, useEffect, useRef } from 'react'
import config from '@/config'

const OIDC_LOGIN_PATH = '/api/v1/auth/oidc/login'
const POPUP_FEATURES = 'width=520,height=640,menubar=no,toolbar=no,location=no,status=no'

interface OidcMessage {
  token?: string
}

/**
 * Drives the OIDC "Login with SSO" flow from the browser.
 *
 * Opens the backend's `/api/v1/auth/oidc/login` endpoint in a popup; the backend redirects to the
 * configured Identity Provider, and on success its callback posts the HiveMQ Edge JWT back to this
 * window via `postMessage`. The returned `startLogin` resolves with that token (or rejects if the
 * popup is blocked or closed before completing).
 *
 * Security: messages are only accepted from our own origin, matching the callback page's
 * `postMessage(..., origin)` target.
 */
export const useOidcLogin = () => {
  // Track the in-flight resolve/reject so the message listener can settle the current attempt.
  const pendingRef = useRef<{ resolve: (token: string) => void; reject: (reason: Error) => void } | null>(null)
  const popupRef = useRef<Window | null>(null)
  const pollRef = useRef<number | undefined>(undefined)

  const settleReject = useCallback((reason: Error) => {
    if (pollRef.current !== undefined) {
      window.clearInterval(pollRef.current)
      pollRef.current = undefined
    }
    pendingRef.current?.reject(reason)
    pendingRef.current = null
  }, [])

  useEffect(() => {
    const onMessage = (event: MessageEvent) => {
      // Only trust messages from our own origin (the callback page targets its own origin).
      if (event.origin !== window.location.origin) return
      const data = event.data as OidcMessage
      if (!data || typeof data.token !== 'string') return

      if (pollRef.current !== undefined) {
        window.clearInterval(pollRef.current)
        pollRef.current = undefined
      }
      pendingRef.current?.resolve(data.token)
      pendingRef.current = null
      popupRef.current?.close()
      popupRef.current = null
    }

    window.addEventListener('message', onMessage)
    return () => {
      window.removeEventListener('message', onMessage)
      if (pollRef.current !== undefined) window.clearInterval(pollRef.current)
    }
  }, [])

  const startLogin = useCallback((): Promise<string> => {
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
          settleReject(new Error('popup-closed'))
        }
      }, 500)
    })
  }, [settleReject])

  return { startLogin }
}
