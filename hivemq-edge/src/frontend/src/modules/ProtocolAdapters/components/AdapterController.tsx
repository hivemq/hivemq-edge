import { FC, ReactNode, useEffect, useState } from 'react'
import { useDisclosure } from '@chakra-ui/react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { SubmitHandler } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

import { Adapter, ApiError } from '@/api/__generated__'
import { useCreateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter.tsx'
import { useUpdateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useUpdateProtocolAdapter.tsx'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import { AdapterNavigateState, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import AdapterInstanceDrawer from '@/modules/ProtocolAdapters/components/drawers/AdapterInstanceDrawer.tsx'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'

interface AdapterEditorProps {
  isNew?: boolean
  children?: ReactNode
}

const AdapterController: FC<AdapterEditorProps> = ({ children, isNew }) => {
  const { t } = useTranslation()
  const { successToast, errorToast } = useEdgeToast()

  const [adaptorType, setAdaptorType] = useState<string | undefined>(undefined)
  const { isOpen: isInstanceOpen, onOpen: onInstanceOpen, onClose: onInstanceClose } = useDisclosure()
  const createProtocolAdapter = useCreateProtocolAdapter()
  const updateProtocolAdapter = useUpdateProtocolAdapter()
  const navigate = useNavigate()
  const { state } = useLocation()
  const { data: allAdapters } = useListProtocolAdapters()
  const { adapterId } = useParams()

  useEffect(() => {
    if ((state as AdapterNavigateState)?.protocolAdapterType) {
      setAdaptorType(state.protocolAdapterType)
    }
  }, [state])

  useEffect(() => {
    if (!allAdapters) return
    const instance = allAdapters?.find((e) => e.id === adapterId)
    if (!isNew && !instance) {
      errorToast(
        {
          id: 'adapter-open-noExist',
          title: t('protocolAdapter.toast.view.title'),
          description: t('protocolAdapter.toast.view.error'),
        },
        new Error(t('protocolAdapter.toast.view.noLongerExist', { id: adapterId }) as string)
      )
      navigate('/protocol-adapters', { replace: true })
      return
    }

    if (adaptorType) {
      onInstanceOpen()
    }
  }, [adapterId, adaptorType, allAdapters, errorToast, isNew, navigate, onInstanceOpen, t])

  const handleInstanceClose = () => {
    onInstanceClose()
    navigate('/protocol-adapters', { state: { protocolAdapterTabIndex: ProtocolAdapterTabIndex.protocols } })
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

    onInstanceClose()
    const adapterNavigateState: AdapterNavigateState = {
      protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters,
      selectedActiveAdapter: { isNew: !!isNew, isOpen: false, adapterId: id },
    }
    navigate('/protocol-adapters', {
      state: adapterNavigateState,
    })
  }

  return (
    <div>
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
