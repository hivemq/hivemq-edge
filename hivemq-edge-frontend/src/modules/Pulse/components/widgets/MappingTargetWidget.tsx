import type { FC } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import { Button, FormControl, FormLabel, Input } from '@chakra-ui/react'

import type { ManagedAsset } from '@/api/__generated__'
import { useSelectCombinerFromMapping } from '@/api/hooks/useCombiners/useSelectCombinerFromMapping.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'

const MappingTargetWidget: FC<WidgetProps<ManagedAsset['mapping']['mappingId']>> = (props) => {
  const { t } = useTranslation()
  const { id, value, required, disabled, readonly, label, rawErrors } = props
  const mappingId = value as ManagedAsset['mapping']['mappingId']
  const { data, isLoading, error } = useSelectCombinerFromMapping(mappingId)

  const isInvalid = rawErrors && rawErrors.length > 0

  return (
    <FormControl alignItems="center" isRequired={required} isDisabled={disabled || readonly} isInvalid={isInvalid}>
      <FormLabel htmlFor={id}>{label}</FormLabel>

      {isLoading && <LoaderSpinner />}
      {!mappingId && <Input id={id} name={id} defaultValue={t('pulse.assets.listing.sources.unset')} />}
      {error && (
        <Input
          id={id}
          name={id}
          defaultValue={t('pulse.assets.listing.sources.notFound')}
          borderRadius="var(--chakra-radii-md)"
          sx={{
            borderColor: 'var(--chakra-colors-status-error-500)',
            boxShadow: '0 0 0 1px var(--chakra-colors-status-error-500);',
          }}
        />
      )}
      {data && (
        <NodeNameCard
          type={NodeTypes.ASSETS_NODE}
          name={data.name}
          description={data.description}
          rightElement={<Button isDisabled>{t('pulse.assets.actions.mapper')}</Button>}
        />
      )}
    </FormControl>
  )
}

export default MappingTargetWidget
