import { type FC, useCallback } from 'react'
import Form from '@rjsf/chakra-ui'
import { type IChangeEvent } from '@rjsf/core'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { customFormatsValidator } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { adapterJSFFields, adapterJSFWidgets } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { useSubscriptionManager } from '@/modules/Subscriptions/hooks/useSubscriptionManager.tsx'

interface SubscriptionFormProps {
  id: string
  type: 'inward' | 'outward'
}

// TODO[NVL] Should replicate the config from the adapter form; share component?
const SubscriptionForm: FC<SubscriptionFormProps> = ({ id, type }) => {
  const { inwardManager, outwardManager } = useSubscriptionManager(id)

  const subscriptionManager = type === 'inward' ? inwardManager : outwardManager

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const subscriptions = data.formData?.subscriptions
      subscriptionManager?.onSubmit?.(subscriptions)
      console.log('XXXXXXX', subscriptions)
    },
    [subscriptionManager]
  )

  if (!subscriptionManager)
    return <ErrorMessage type="Subscriptions" message={`Cannot extract the ${type} subscriptions`} />

  return (
    <Form
      id="adapter-instance-form"
      schema={subscriptionManager.schema}
      uiSchema={subscriptionManager.uiSchema}
      liveValidate
      onSubmit={onFormSubmit}
      validator={customFormatsValidator}
      showErrorList="bottom"
      onError={(errors) => console.log('XXXXXXX', errors)}
      formData={subscriptionManager.formData}
      // customValidate={customValidate(schema, allAdapters, t)}
      // transformErrors={filterUnboundErrors}
      // formContext={context}
      widgets={adapterJSFWidgets}
      fields={adapterJSFFields}
      templates={{
        ObjectFieldTemplate,
        FieldTemplate,
        BaseInputTemplate,
        ArrayFieldTemplate,
        ArrayFieldItemTemplate,
      }}
    />
  )
}

export default SubscriptionForm
