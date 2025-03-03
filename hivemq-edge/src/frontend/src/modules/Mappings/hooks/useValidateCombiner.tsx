import { useCallback } from 'react'
import type { CustomValidator, RJSFSchema } from '@rjsf/utils'
import type { UseQueryResult } from '@tanstack/react-query'

import type { Combiner, DomainTagList, TopicFilterList } from '@/api/__generated__'

import type { CombinerContext } from '@/modules/Mappings/types'

// TODO[NVL] Context is not part of the customValidator props; need to get a better construction of props
export const useValidateCombiner = (queries: UseQueryResult<DomainTagList | TopicFilterList, Error>[]) => {
  const validateCombiner = useCallback<CustomValidator<Combiner, RJSFSchema, CombinerContext>>((formData, errors) => {
    formData?.mappings?.items?.forEach((entity, index) => {
      if (!errors.mappings?.items?.[index]) return

      console.log('XXX', queries, entity)
      // TODO
    })

    return errors
  }, [])

  return validateCombiner
}
