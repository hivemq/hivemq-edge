import { FC, ReactNode, useEffect, useState } from 'react'
import { Text, useDisclosure, useToast, UseToastOptions } from '@chakra-ui/react'
import { useLocation, useNavigate } from 'react-router-dom'
import { SubmitHandler } from 'react-hook-form'

import AdapterInstanceDrawer from '@/modules/ProtocolAdapters/components/drawers/AdapterInstanceDrawer.tsx'
import ProtocolSelectorDrawer from '@/modules/ProtocolAdapters/components/drawers/ProtocolSelectorDrawer.tsx'
import { AdapterType } from '@/modules/ProtocolAdapters/types.ts'
import { Adapter, ApiError } from '@/api/__generated__'
import { useCreateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter.tsx'
import { useTranslation } from 'react-i18next'
import { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'
import { useUpdateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useUpdateProtocolAdapter.tsx'

interface AdapterEditorProps {
  isNew?: boolean
  children?: ReactNode
}

const DEFAULT_TOAST_OPTION: UseToastOptions = {
  status: 'success',
  duration: 3000,
  isClosable: true,
  position: 'top-right',
}

const AdapterController: FC<AdapterEditorProps> = ({ children, isNew }) => {
  const { t } = useTranslation()
  const createToast = useToast()

  const [adaptorType, setAdaptorType] = useState<string | undefined>(undefined)
  const { isOpen: isSelectorOpen, onClose: onSelectorClose } = useDisclosure()
  const { isOpen: isInstanceOpen, onOpen: onInstanceOpen, onClose: onInstanceClose } = useDisclosure()
  const createProtocolAdapter = useCreateProtocolAdapter()
  const updateProtocolAdapter = useUpdateProtocolAdapter()
  const navigate = useNavigate()
  const { state } = useLocation()
  const { selectedAdapterId } = state || {}

  useEffect(() => {
    if (selectedAdapterId) {
      setAdaptorType(selectedAdapterId)
    }
  }, [selectedAdapterId])

  useEffect(() => {
    if (adaptorType) {
      onInstanceOpen()
    }
  }, [adaptorType, onInstanceOpen])

  const handleSelectorClose = () => {
    onSelectorClose()
    navigate('/protocol-adapters')
  }

  const handleSelectorSubmit: SubmitHandler<AdapterType> = ({ adapterType }) => {
    setAdaptorType(adapterType)
    onSelectorClose()
  }

  const handleInstanceClose = () => {
    onInstanceClose()
    navigate('/protocol-adapters')
  }

  const handleInstanceSubmit: SubmitHandler<Adapter> = (props) => {
    const { id, ...rest } = props

    if (isNew) {
      createProtocolAdapter
        .mutateAsync({
          adapterType: adaptorType as string,
          requestBody: {
            id: id,
            type: adaptorType,
            // @ts-ignore Need to review the type of the stub
            config: { ...rest, id: id },
          },
        })
        .then(() => {
          createToast({
            ...DEFAULT_TOAST_OPTION,
            title: t('protocolAdapter.toast.create.title'),
            description: t('protocolAdapter.toast.create.description'),
          })
        })
        .catch((e: ApiError) => {
          const { body } = e
          createToast({
            ...DEFAULT_TOAST_OPTION,
            status: 'error',
            title: t('protocolAdapter.toast.create.title'),
            description: (
              <>
                <Text>{t('protocolAdapter.toast.create.error')}</Text>
                {body.errors?.map((e: ProblemDetailsExtended) => (
                  <Text key={e.fieldName as string}>
                    {e.fieldName as string} : {e.detail}
                  </Text>
                ))}
                {(typeof body === 'string' || body instanceof String) && <Text>{body}</Text>}
              </>
            ),
          })
        })
    } else {
      updateProtocolAdapter
        .mutateAsync({
          adapterId: id,
          requestBody: {
            id: id,
            type: adaptorType,
            // @ts-ignore Need to review the type of the stub
            config: { ...rest, id: id },
          },
        })
        .then(() => {
          createToast({
            ...DEFAULT_TOAST_OPTION,
            title: t('protocolAdapter.toast.update.title'),
            description: t('protocolAdapter.toast.update.description'),
          })
        })
        .catch((e: ApiError) => {
          const { body } = e
          createToast({
            ...DEFAULT_TOAST_OPTION,
            status: 'error',
            title: t('protocolAdapter.toast.update.title'),
            description: (
              <>
                <Text>{t('protocolAdapter.toast.update.error')}</Text>
                {body.errors?.map((e: ProblemDetailsExtended) => (
                  <Text key={e.fieldName as string}>
                    {e.fieldName as string} : {e.detail}
                  </Text>
                ))}
                {(typeof body === 'string' || body instanceof String) && <Text>{body}</Text>}
              </>
            ),
          })
        })
    }

    handleInstanceClose()
  }

  return (
    <div>
      <ProtocolSelectorDrawer onClose={handleSelectorClose} isOpen={isSelectorOpen} onSubmit={handleSelectorSubmit} />
      <AdapterInstanceDrawer
        adapterType={adaptorType}
        isNewAdapter={isNew}
        isOpen={isInstanceOpen}
        isSubmitting={false}
        onSubmit={handleInstanceSubmit}
        onClose={handleInstanceClose}
      />
      {children}
    </div>
  )
}

export default AdapterController
