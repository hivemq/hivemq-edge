import { useState } from 'react'
import type { AxiosError } from 'axios'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'

import config from '@/config'

import type { CancelablePromise, OpenAPIConfig } from '@/api/__generated__'
import { BaseHttpRequest, HiveMqClient } from '@/api/__generated__'
import type { ApiRequestOptions } from '@/api/__generated__/core/ApiRequestOptions.ts'
import { request as __request } from '@/api/__generated__/core/request.ts'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'

const axiosInstance = axios.create()

export class AxiosHttpRequestWithInterceptors extends BaseHttpRequest {
  constructor(config: OpenAPIConfig) {
    super(config)
  }

  public override request<T>(options: ApiRequestOptions): CancelablePromise<T> {
    return __request(this.config, options, axiosInstance)
  }
}

export const useHttpClient = () => {
  const { credentials, logout } = useAuth()
  const navigate = useNavigate()
  const [client] = useState<HiveMqClient>(createInstance)

  function createInstance() {
    // Make sure to clear the interceptors, since axiosInstance is global
    axiosInstance.interceptors.response.clear()
    axiosInstance.interceptors.request.clear()
    axiosInstance.interceptors.request.use((internalConfig) => {
      internalConfig.timeout = config.httpClient.axiosTimeout
      return internalConfig
    })

    axiosInstance.interceptors.response.use(
      function (response) {
        // Any status code that lie within the range of 2xx cause this function to trigger
        // Do something with response data
        const { 'x-bearer-token-reissue': reissuedToken } = response.headers
        /* istanbul ignore if -- @preserve */
        if (reissuedToken) {
          // TODO[NVL] Deactivating the reissuing, see https://hivemq.kanbanize.com/ctrl_board/57/cards/15303/details/
          // login({ token: reissuedToken }, () => undefined)
        }
        return response
      },
      function (error: AxiosError) {
        // Any status codes that falls outside the range of 2xx cause this function to trigger
        // Do something with response error
        if (error.response?.status === 401) {
          logout(() => navigate('./login'))
        }
        return Promise.reject(error)
      }
    )

    return new HiveMqClient(
      {
        BASE: config.apiBaseUrl,
        TOKEN: credentials?.token,
      },
      AxiosHttpRequestWithInterceptors
    )
  }

  return client
}

export const useSimpleHttpClient = () => {
  const [client] = useState<HiveMqClient>(createInstance)

  function createInstance() {
    return new HiveMqClient({
      BASE: config.apiBaseUrl,
    })
  }

  return client
}
