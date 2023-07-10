import { FC, ReactNode, useEffect, useState } from 'react'
import { useDisclosure } from '@chakra-ui/react'
import { useLocation, useNavigate } from 'react-router-dom'
import { SubmitHandler } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

import { Adapter, ApiError } from '@/api/__generated__'
import { useCreateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter.tsx'
import { useUpdateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useUpdateProtocolAdapter.tsx'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import { ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'
import AdapterInstanceDrawer from '@/modules/ProtocolAdapters/components/drawers/AdapterInstanceDrawer.tsx'
import ProtocolSelectorDrawer from '@/modules/ProtocolAdapters/components/drawers/ProtocolSelectorDrawer.tsx'
import { AdapterType } from '@/modules/ProtocolAdapters/types.ts'

interface AdapterEditorProps {
  isNew?: boolean
  children?: ReactNode
}

const AdapterController: FC<AdapterEditorProps> = ({ children, isNew }) => {
  const { t } = useTranslation()
  const { successToast, errorToast } = useEdgeToast()

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
    navigate('/protocol-adapters', { state: { protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters } })
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
          successToast({
            title: t('protocolAdapter.toast.create.title'),
            description: t('protocolAdapter.toast.create.description'),
          })
        })
        .catch((err: ApiError) =>
          errorToast(
            {
              title: t('protocolAdapter.toast.create.title'),
              description: t('protocolAdapter.toast.create.error'),
            },
            err
          )
        )
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
          successToast({
            title: t('protocolAdapter.toast.update.title'),
            description: t('protocolAdapter.toast.update.description'),
          })
        })
        .catch((err: ApiError) =>
          errorToast(
            {
              title: t('protocolAdapter.toast.update.title'),
              description: t('protocolAdapter.toast.update.error'),
            },
            err
          )
        )
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
