import type { FC } from 'react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { BoxProps } from '@chakra-ui/react'
import { Box, HStack, Text, VStack } from '@chakra-ui/react'
import { chakraComponents, createFilter, Select } from 'chakra-react-select'
import type { GroupBase, OptionProps, SingleValue, SingleValueProps } from 'chakra-react-select'
import { LuPlus } from 'react-icons/lu'

import type { DataCombining, ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'

interface ManagedAssetSelectProps extends Omit<BoxProps, 'onChange'> {
  onChange?: (value: SingleValue<ManagedAsset>) => void
  mappings: DataCombining[]
}

const SingleValue = (props: SingleValueProps<ManagedAsset>) => (
  <chakraComponents.SingleValue {...props}>
    <Text data-testid="combiner-asset-selected-value">{props.data.name}</Text>
  </chakraComponents.SingleValue>
)

const Option =
  (selection: ManagedAsset | null) =>
  ({ children, ...props }: OptionProps<ManagedAsset>) => (
    <chakraComponents.Option {...props} isSelected={Boolean(selection && props.data.id === selection?.id)}>
      <VStack gap={0} alignItems="stretch" w="100%">
        <HStack>
          <Box flex={1}>
            <Text flex={1} data-testid="combiner-asset-name">
              {props.data.name}
            </Text>
          </Box>
          <Box flex={1}>
            <Text flex={1} data-testid="combiner-asset-status">
              {props.data.mapping?.status}
            </Text>
          </Box>
        </HStack>
        <Text
          fontSize="sm"
          noOfLines={3}
          ml={4}
          lineHeight="normal"
          textAlign="justify"
          data-testid="combiner-asset-description"
        >
          {props.data.description}
        </Text>
      </VStack>
    </chakraComponents.Option>
  )

const ManagedAssetSelect: FC<ManagedAssetSelectProps> = ({ onChange, mappings, ...boxProps }) => {
  const { t } = useTranslation()
  const { isLoading, data } = useListManagedAssets()

  const allAssetIds = useMemo(() => {
    return mappings.map((e) => e.destination.assetId)
  }, [mappings])

  const filteredData = useMemo(() => {
    if (!data?.items) return []
    return data.items.filter(
      // Only shows unmapped or draft assets
      (asset) =>
        asset.mapping.status === AssetMapping.status.UNMAPPED || asset.mapping.status === AssetMapping.status.DRAFT
    )
  }, [data?.items])

  const [selection, setSelection] = useState<ManagedAsset | null>(null)

  const handleAddAsset = () => {
    onChange?.(selection)
    setSelection(null)
  }

  return (
    <HStack mt={3}>
      <Box {...boxProps} flex={1}>
        <Select<ManagedAsset, false, GroupBase<ManagedAsset>>
          id="combiner-asset-select"
          instanceId="asset"
          options={filteredData}
          isLoading={isLoading}
          value={filteredData.find((e) => e.id === selection?.id) || null}
          noOptionsMessage={() => t('pulse.assets.selector.noAssetsFound')}
          aria-label={t('pulse.assets.selector.aria-label')}
          onChange={(newValue) => {
            if (newValue) {
              // onChange?.(newValue)
              setSelection(newValue)
            } else setSelection(null)
          }}
          isClearable
          placeholder={t('pulse.assets.selector.placeholder')}
          isOptionDisabled={(asset) => {
            // Assets currently in mappings cannot be added again
            return allAssetIds.includes(asset.id)
          }}
          filterOption={createFilter({
            stringify: (option) => {
              return `${option.data.name || ''}${option.data.description || ''}`
            },
          })}
          components={{
            SingleValue: SingleValue,
            Option: Option(selection),
          }}
        />
      </Box>
      <IconButton
        data-testid="combiner-mappings-add-asset"
        aria-label={t('pulse.assets.selector.add')}
        icon={<LuPlus />}
        onClick={handleAddAsset}
        isDisabled={!selection}
      />
    </HStack>
  )
}

export default ManagedAssetSelect
