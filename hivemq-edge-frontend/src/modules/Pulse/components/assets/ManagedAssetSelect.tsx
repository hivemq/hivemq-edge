import type { FC } from 'react'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { GroupBase, SingleValue } from 'chakra-react-select'
import { createFilter, chakraComponents, Select } from 'chakra-react-select'
import type { BoxProps } from '@chakra-ui/react'
import { Box, HStack, Text, VStack } from '@chakra-ui/react'
import { LuPlus } from 'react-icons/lu'

import type { ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'

interface ManagedAssetSelectProps extends Omit<BoxProps, 'onChange'> {
  onChange?: (value: SingleValue<ManagedAsset>) => void
}

const ManagedAssetSelect: FC<ManagedAssetSelectProps> = ({ onChange, ...boxProps }) => {
  const { t } = useTranslation()
  const { isLoading, data } = useListManagedAssets()

  const filteredData = useMemo(() => {
    return data?.items || []
  }, [data?.items])

  const [selection, setSelection] = useState<ManagedAsset | null>(null)

  const handleAddAsset = () => {
    onChange?.(selection)
  }

  return (
    <HStack mt={3}>
      <Box {...boxProps} flex={1}>
        <Select<ManagedAsset, false, GroupBase<ManagedAsset>>
          // inputId="react-select-asset-input"
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
          isOptionDisabled={(asset) => asset.mapping.status === AssetMapping.status.STREAMING}
          filterOption={createFilter({
            stringify: (option) => {
              return `${option.data.name || ''}${option.data.description || ''}`
            },
          })}
          components={{
            SingleValue: (props) => {
              return (
                <chakraComponents.SingleValue {...props}>
                  <Text>{props.data.name}</Text>
                </chakraComponents.SingleValue>
              )
            },
            Option: ({ children, ...props }) => {
              return (
                <chakraComponents.Option {...props} isSelected={Boolean(selection && props.data.id === selection?.id)}>
                  <VStack gap={0} alignItems="stretch" w="100%">
                    <HStack>
                      <Box flex={1}>
                        <Text flex={1}>{props.data.name}</Text>
                      </Box>
                      <Box flex={1}>
                        <Text flex={1}>{props.data.mapping?.status}</Text>
                      </Box>
                    </HStack>
                    <Text fontSize="sm" noOfLines={3} ml={4} lineHeight="normal" textAlign="justify">
                      {props.data.description}
                    </Text>
                  </VStack>
                </chakraComponents.Option>
              )
            },
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
