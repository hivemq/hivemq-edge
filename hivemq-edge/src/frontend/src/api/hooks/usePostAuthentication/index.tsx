import { useMutation } from '@tanstack/react-query'
import { ApiBearerToken, ApiError, HiveMqClient, UsernamePasswordCredentials } from '../../__generated__'
import config from '../../../config'

export const usePostAuthentication = () => {
  const postCredential = (credentials: UsernamePasswordCredentials) => {
    const appClient = new HiveMqClient({
      BASE: config.apiBaseUrl,
    })

    return appClient.authentication.authenticate(credentials)
  }

  return useMutation<ApiBearerToken, ApiError, UsernamePasswordCredentials>(['get.token'], postCredential)
}
