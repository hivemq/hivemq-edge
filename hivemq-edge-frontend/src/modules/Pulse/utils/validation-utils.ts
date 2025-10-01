import type { FormValidation } from '@rjsf/utils'

import type { Combiner, ManagedAsset } from '@/api/__generated__'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

import i18n from '@/config/i18n.config.ts'

export const customSchemaValidator =
  (combiner: Combiner | undefined) => (formData: ManagedAsset, errors: FormValidation<ManagedAsset>) => {
    const schemaHandler = validateSchemaFromDataURI(formData.schema, SelectEntityType.TOPIC)

    if (schemaHandler.error) {
      errors.schema?.addError(schemaHandler.error)
    }

    if (formData.mapping.mappingId && !combiner) {
      errors.mapping?.mappingId?.addError(i18n.t('pulse.error.mapping.notFound'))
    }

    return errors
  }
