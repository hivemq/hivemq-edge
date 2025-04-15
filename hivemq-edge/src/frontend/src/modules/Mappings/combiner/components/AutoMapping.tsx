import { type FC, useMemo } from 'react'
import type { JSONSchema7 } from 'json-schema'
import { useTranslation } from 'react-i18next'
import { Icon } from '@chakra-ui/react'
import { FaRightFromBracket } from 'react-icons/fa6'

import type { DataCombining, DataIdentifierReference, Instruction } from '@/api/__generated__'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import IconButton from '@/components/Chakra/IconButton'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
import { toJsonPath } from '@/components/rjsf/MqttTransformation/utils/data-type.utils'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'
import {
  findBestMatch,
  getFilteredDataReferences,
  getSchemasFromReferences,
} from '@/modules/Mappings/utils/combining.utils'
import type { CombinerContext } from '../../types'

interface AutoMappingProps {
  id?: string
  formData?: DataCombining
  formContext?: CombinerContext
  onChange?: (instructions: Instruction[]) => void
}

export const AutoMapping: FC<AutoMappingProps> = ({ formData, formContext, onChange }) => {
  const { t } = useTranslation()

  // TODO[NVL] This is almost a duplicate of the CombinedEntitySelect; reuse
  const references = useMemo(() => {
    return getFilteredDataReferences(formData, formContext)
  }, [formContext, formData])

  const schemaQueries = useGetCombinedDataSchemas(references)

  const displayedSchemas = useMemo(() => {
    const dataRef = getSchemasFromReferences(references, schemaQueries)
    const schemas = dataRef.filter(
      (dataReference) => dataReference.schema?.status === 'success' && Boolean(dataReference.schema.schema)
    )

    return schemas
      .map((e) => {
        const gg = getPropertyListFrom(e.schema?.schema as JSONSchema7)

        const instruction: DataIdentifierReference = {
          id: e.id as string,
          type: e.type as DataIdentifierReference.type,
        }

        gg.forEach((e) => (e.metadata = instruction))
        return gg
      })
      .flat()
  }, [references, schemaQueries])

  const schema = useMemo(() => {
    if (!formData?.destination?.schema) return []

    const hhh = validateSchemaFromDataURI(formData?.destination?.schema)
    return hhh.schema ? getPropertyListFrom(hhh.schema) : []
  }, [formData?.destination?.schema])

  const handleMatching = () => {
    if (!schema.length) return

    const inst = schema.reduce<Instruction[]>((acc, cur) => {
      const bestMatch = findBestMatch(cur, displayedSchemas, null)
      console.log('XXXXXX mat', bestMatch)
      if (bestMatch?.value) {
        const { id, type } = bestMatch.value.metadata || {}
        const ref: DataIdentifierReference = { id: id as string, type: type as DataIdentifierReference.type }
        const instruction = {
          sourceRef: ref,
          destination: toJsonPath([...cur.path, cur.key].join('.')),
          source: toJsonPath([...bestMatch.value.path, bestMatch.value.key].join('.')),
        }
        acc.push(instruction)
      }
      return acc
    }, [])

    console.log('XXXXXX mat', inst)
    onChange?.(inst)
  }

  return (
    <IconButton
      isDisabled={!(schema.length && displayedSchemas.length)}
      icon={<Icon as={FaRightFromBracket} />}
      aria-label={t('combiner.schema.mapping.action.generateMappings')}
      onClick={handleMatching}
    />
  )
}
