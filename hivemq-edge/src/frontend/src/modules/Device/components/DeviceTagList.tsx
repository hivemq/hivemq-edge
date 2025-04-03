import type { IChangeEvent } from '@rjsf/core'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

import type { Adapter, DomainTagList } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { useTagManager } from '@/modules/Device/hooks/useTagManager.ts'
import type { DeviceTagListContext } from '../types'

interface DeviceTagListProps {
  adapter: Adapter
  onClose: () => void
}

const DeviceTagList: FC<DeviceTagListProps> = ({ adapter, onClose }) => {
  const { t } = useTranslation()
  const { isLoading, isError, context, onupdateCollection } = useTagManager(adapter.id)

  const onHandleSubmit = (data: IChangeEvent) => {
    if (!data.formData) return
    onupdateCollection(data.formData as DomainTagList)
    onClose()
  }

  const formContext: DeviceTagListContext = {
    adapterId: adapter.id,
  }

  return (
    <>
      {isLoading && <LoaderSpinner />}
      {isError && <ErrorMessage message={t('device.errors.noTagLoaded')} />}

      {!context.schema && <ErrorMessage message={t('device.errors.noFormSchema')} status="error" />}
      {context.schema && (
        <ChakraRJSForm
          id="tag-listing-form"
          schema={context.schema}
          uiSchema={context.uiSchema}
          formData={context.formData}
          onSubmit={onHandleSubmit}
          formContext={formContext}
        />
      )}
    </>
  )
}

export default DeviceTagList
