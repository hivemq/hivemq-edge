import type { UseToastOptions } from '@chakra-ui/react'
import { useToast } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { isRouteErrorResponse } from 'react-router-dom'

import type { Bridge } from '@/api/__generated__'
import { useCreateBridge } from '@/api/hooks/useGetBridges/useCreateBridge.ts'
import { useDeleteBridge } from '@/api/hooks/useGetBridges/useDeleteBridge.ts'
import { useUpdateBridge } from '@/api/hooks/useGetBridges/useUpdateBridge.ts'
import { BASE_TOAST_OPTION, DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'

export const useBridgeManager = () => {
  const { t } = useTranslation()
  const createToast = useToast(BASE_TOAST_OPTION)

  const createMutator = useCreateBridge()
  const deleteMutator = useDeleteBridge()
  const updateMutator = useUpdateBridge()

  const formatToast = (operation: 'delete' | 'create' | 'update') => ({
    success: {
      title: t(`bridge.toast.${operation}.title`),
      description: t(`bridge.toast.${operation}.description`, { context: 'success' }),
    },
    error: {
      title: t(`bridge.toast.${operation}.title`),
      description: t(`bridge.toast.${operation}.description`, { context: 'error' }),
    },
    loading: {
      title: t(`bridge.toast.${operation}.title`),
      description: t('bridge.toast.description', { context: 'loading' }),
    },
  })

  const onDelete = (bridgeId: string) => {
    createToast.promise(deleteMutator.mutateAsync(bridgeId), formatToast('delete'))
  }

  const onCreate = (requestBody: Bridge) => {
    const promise = createMutator.mutateAsync(requestBody)
    createToast.promise(promise, formatToast('create'))
    return promise
  }

  const onUpdate = (bridgeId: string, requestBody: Bridge) => {
    const promise = updateMutator.mutateAsync({ name: bridgeId, requestBody: requestBody })
    createToast.promise(promise, formatToast('update'))
    return promise
  }

  // TODO[NVL] Very clunky. Redesign
  const onError = (error?: Error, options?: UseToastOptions) => {
    if ((options?.id && !createToast.isActive(options.id)) || !options?.id) {
      let message
      if (isRouteErrorResponse(error)) {
        message = (error as { statusText: string }).statusText
      } else if (error instanceof Error) {
        message = error.message
      } else {
        message = String(error)
      }

      createToast({
        ...DEFAULT_TOAST_OPTION,
        ...options,
        status: 'error',
        description: options?.description || message,
      })
    }
  }

  return { onDelete, onCreate, onUpdate, onError }
}
