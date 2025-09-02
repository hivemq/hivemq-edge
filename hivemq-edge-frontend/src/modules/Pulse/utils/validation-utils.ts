import type { FormValidation } from '@rjsf/utils'

import type { ManagedAsset } from '@/api/__generated__'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

export const customSchemaValidator = (formData: ManagedAsset, errors: FormValidation<ManagedAsset>) => {
  const schemaHandler = validateSchemaFromDataURI(formData.schema, SelectEntityType.TOPIC)

  if (schemaHandler.error) {
    errors.schema?.addError(schemaHandler.error)
  }
  return errors
}
