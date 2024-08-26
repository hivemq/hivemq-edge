import { type FC } from 'react'
import Form from '@rjsf/chakra-ui'

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
  console.log('XXXXXX subscriptionSchema', type, inwardManager)

  const subscriptionManager = type === 'inward' ? inwardManager : outwardManager

  if (!subscriptionManager)
    return <ErrorMessage type="Subscriptions" message={`Cannot extract the ${type} subscriptions`} />

  return (
    <Form
      id="adapter-instance-form"
      schema={subscriptionManager.schema}
      uiSchema={subscriptionManager.uiSchema}
      liveValidate
      // onSubmit={onValidate}
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