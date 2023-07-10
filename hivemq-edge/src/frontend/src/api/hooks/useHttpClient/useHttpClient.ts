import { useState } from 'react'
import axios, { AxiosError } from 'axios'

import { BaseHttpRequest, CancelablePromise, HiveMqClient, OpenAPIConfig } from '@/api/__generated__'
import { ApiRequestOptions } from '@/api/__generated__/core/ApiRequestOptions.ts'
import { request as __request } from '@/api/__generated__/core/request.ts'

import config from '@/config'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'
import { useNavigate } from 'react-router-dom'

const axiosInstance = axios.create()

class AxiosHttpRequestWithInterceptors extends BaseHttpRequest {
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
    axiosInstance.interceptors.response.use(
      function (response) {
        // Any status code that lie within the range of 2xx cause this function to trigger
        // Do something with response data

        return response
      },
      function (error: AxiosError) {
        // Any status codes that falls outside the range of 2xx cause this function to trigger
        // Do something with response error
        if (error.response?.status === 401) {
          logout(() => navigate('/login'))
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
