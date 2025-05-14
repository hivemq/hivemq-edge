import { useMemo, type FC } from 'react'
import { useTranslation } from 'react-i18next'
import { type SingleValue, Select, chakraComponents } from 'chakra-react-select'
import { Icon } from '@chakra-ui/react'
import { FaKey } from 'react-icons/fa'

import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'

interface PrimaryOption {
  label: string
  value: string
  type: DataIdentifierReference.type
}
interface PrimarySelectProps {
  id?: string
  formData?: DataCombining
  onChange: (newValue: SingleValue<PrimaryOption>) => void
}

export const PrimarySelect: FC<PrimarySelectProps> = ({ id, formData, onChange }) => {
  const { t } = useTranslation()

  const primaryOptions = useMemo(() => {
    const tags = formData?.sources?.tags || []
    const topicFilters = formData?.sources?.topicFilters || []

    return [
      ...tags.map<PrimaryOption>((entity) => ({
        label: entity,
        value: entity,
        type: DataIdentifierReference.type.TAG,
      })),
      ...topicFilters.map<PrimaryOption>((entity) => ({
        label: entity,
        value: entity,
        type: DataIdentifierReference.type.TOPIC_FILTER,
      })),
    ]
  }, [formData])

  const primaryValue = useMemo<PrimaryOption | null>(() => {
    if (!formData?.sources.primary) return null
    return {
      label: formData.sources.primary.id,
      value: formData.sources.primary.id,
      type: formData.sources.primary.type,
    }
  }, [formData?.sources.primary])

  return (
    <Select<PrimaryOption>
      id={id}
      options={primaryOptions}
      data-testid="combiner-mapping-primaryOptions"
      value={primaryValue}
      onChange={onChange}
      isClearable
      components={{
        Control: ({ children, ...props }) => (
          <chakraComponents.Control {...props}>
            <Icon as={FaKey} ml={3} />
            {children}
          </chakraComponents.Control>
        ),
      }}
      placeholder={t('combiner.schema.mapping.primary.placeholder')}
      aria-label={t('combiner.schema.mapping.primary.label')}
    />
  )
}
