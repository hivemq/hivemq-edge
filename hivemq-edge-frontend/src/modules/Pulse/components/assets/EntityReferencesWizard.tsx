import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormHelperText, FormLabel, Text } from '@chakra-ui/react'
import type { MultiValueProps, MultiValueRemoveProps, OptionProps } from 'chakra-react-select'
import { Select, createFilter, chakraComponents } from 'chakra-react-select'

import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import MoreInfo from '@/components/MoreInfo.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { EntityRenderer } from '@/modules/Mappings/combiner/EntityRenderer.tsx'
import { DEFAULT_ASSET_MAPPER_SOURCES } from '@/modules/Pulse/utils/assets.utils.ts'

interface EntityReferenceWizardProps {
  values: EntityReference[]
  onChange: (sources: EntityReference[]) => void
}

const Option = (props: OptionProps<EntityReference, true>) => (
  <chakraComponents.Option {...props}>
    <EntityRenderer reference={props.data} />
  </chakraComponents.Option>
)

const MultiValue = (props: MultiValueProps<EntityReference, true>) => (
  <chakraComponents.MultiValue {...props}>
    <Text>{props.data.id}</Text>
  </chakraComponents.MultiValue>
)

const MultiValueRemove = (props: MultiValueRemoveProps<EntityReference, true>) => {
  if (props.data.type === EntityType.EDGE_BROKER || props.data.type === EntityType.PULSE_AGENT)
    return <>{props.children}</>
  return <chakraComponents.MultiValueRemove {...props}>{props.children}</chakraComponents.MultiValueRemove>
}

const EntityReferencesWizard: FC<EntityReferenceWizardProps> = ({ values, onChange }) => {
  const { t } = useTranslation()
  const { data: bridges, isLoading: isBridgeLoading, error: bridgeError } = useListBridges()
  const { data: adapters, isLoading: isAdapterLoading, error: adapterError } = useListProtocolAdapters()

  const safeOptions = useMemo<EntityReference[]>(() => {
    if (!bridges || !adapters) return []
    const bridgeEntities = bridges.map<EntityReference>((e) => ({ type: EntityType.BRIDGE, id: e.id }))
    const adapterEntities = adapters.map<EntityReference>((e) => ({ type: EntityType.ADAPTER, id: e.id }))
    return [...bridgeEntities, ...adapterEntities]
  }, [adapters, bridges])

  if (bridgeError) return <ErrorMessage type={bridgeError.message} />
  if (adapterError) return <ErrorMessage type={adapterError.message} />

  return (
    <FormControl data-testid="wizard-mapper-entities-container">
      <FormLabel htmlFor="mapper-sources">
        {t('pulse.assets.operation.edit.Sources.title')}
        <MoreInfo description={t('pulse.assets.operation.edit.Sources.moreInfo.title')} />
      </FormLabel>
      <Select<EntityReference, true>
        id="wizard-mapper-sources"
        inputId="mapper-sources"
        instanceId="sources"
        isMulti
        isClearable
        isLoading={isBridgeLoading || isAdapterLoading}
        options={safeOptions}
        value={values}
        onChange={(vals) => {
          if (vals.length <= DEFAULT_ASSET_MAPPER_SOURCES.length) onChange(DEFAULT_ASSET_MAPPER_SOURCES)
          else onChange([...vals])
        }}
        getOptionValue={(option) => `${option.type}:${option.id}`}
        noOptionsMessage={({ inputValue }) => {
          return inputValue
            ? t('pulse.assets.operation.edit.Sources.noOptionMatching')
            : t('pulse.assets.operation.edit.Sources.noOptionAvailable')
        }}
        filterOption={createFilter({
          stringify: (option) => {
            const { id } = option.data
            return id
          },
        })}
        placeholder={t('pulse.assets.operation.edit.Sources.placeholder')}
        components={{
          Option,
          MultiValue,
          MultiValueRemove,
        }}
      />
      <FormHelperText>{t('pulse.assets.operation.edit.Sources.helper')}</FormHelperText>
    </FormControl>
  )
}

export default EntityReferencesWizard
