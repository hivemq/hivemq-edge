import type { ComponentType, FC } from 'react'
import { useMemo, useState } from 'react'
import {
  FormControl,
  FormLabel,
  FormHelperText,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalFooter,
  ModalBody,
  ModalCloseButton,
  Button,
  Text,
  VStack,
  Box,
  Icon,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import {
  chakraComponents,
  CreatableSelect,
  createFilter,
  type GroupBase,
  type OptionProps,
  type SingleValueProps,
  type ControlProps,
} from 'chakra-react-select'
import { v4 as uuidv4 } from 'uuid'

import type { Combiner } from '@/api/__generated__'
import { useListAssetMappers } from '@/api/hooks/useAssetMapper'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { HqAssets } from '@/components/Icons'
import MoreInfo from '@/components/MoreInfo.tsx'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'

interface AssetMapperWizardProps {
  assetId: string
  onClose: () => void
  onSubmit?: (assetMapper: Combiner) => void
  isOpen: boolean
}

const enum MAPPER_OPERATION {
  CREATE = 'CREATE',
  EDIT = 'EDIT',
}
type CreatableSelectProps = Combiner & { __isNew__?: boolean }

const AssetMapperWizard: FC<AssetMapperWizardProps> = ({ assetId, isOpen, onClose, onSubmit }) => {
  const { t } = useTranslation()
  const { data: listAssets } = useListManagedAssets()
  const { data: listMappers } = useListAssetMappers()
  const [selectedValue, setSelectedValue] = useState<CreatableSelectProps | undefined>(undefined)

  const asset = useMemo(() => {
    if (!listAssets) return undefined
    return listAssets.items.find((asset) => asset.id === assetId)
  }, [assetId, listAssets])

  // if (isAssetsLoading || isMapperLoading) return <LoaderSpinner />
  // if (assetsError) {
  //   errorToast({ id: 'xx', title: 'This is an error', description: 'And the error is this one' }, assetsError)
  //   return null
  // }
  // if (mappersError) {
  //   errorToast({ id: 'xx', title: 'This is an error', description: 'And the error is this one' }, mappersError)
  //   return null
  // }
  // if (!asset) {
  //   errorToast(
  //     { id: 'xx', title: 'This is an error', description: 'And the error is this one' },
  //     new Error('Asset not found')
  //   )
  //   return null
  // }

  const Option: ComponentType<OptionProps<Combiner, false, GroupBase<Combiner>>> = (props) => {
    const { data: assetMapper } = props
    const isNewOption = Boolean(props.children)

    return (
      <chakraComponents.Option {...props} isSelected={props.data.id === selectedValue?.id}>
        {isNewOption && props.children}
        {!isNewOption && (
          <NodeNameCard
            name={assetMapper.name}
            type={NodeTypes.ASSETS_NODE}
            description={assetMapper.description || t('pulse.mapper.title')}
            rightElement={
              <VStack gap={0} alignItems="flex-end" data-testid="node-sources">
                {assetMapper.sources.items.map((source) => (
                  <Box key={source.id} data-testid="node-source-id">
                    <Text>{source.id}</Text>
                  </Box>
                ))}
              </VStack>
            }
          />
        )}
      </chakraComponents.Option>
    )
  }

  const SingleValue = (props: SingleValueProps<Combiner>) => {
    const { data: assetMapper } = props as unknown as SingleValueProps<CreatableSelectProps>
    return (
      <chakraComponents.SingleValue {...props}>
        <Text id="react-select-mapper-value">{assetMapper.name}</Text>
      </chakraComponents.SingleValue>
    )
  }

  const Control = (props: ControlProps<Combiner>) => {
    return (
      <chakraComponents.Control {...props}>
        <Box
          data-testid="wizard-mapper-selector-leftAdd"
          height="-webkit-fill-available"
          alignContent="end"
          backgroundColor="gray.100"
          paddingInlineStart="var(--input-padding)"
          paddingInlineEnd="var(--input-padding)"
        >
          <Icon as={HqAssets} boxSize={6} />
        </Box>
        {props.children}
      </chakraComponents.Control>
    )
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} closeOnOverlayClick={false}>
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>
          {asset ? t('pulse.assets.operation.edit.header', { name: asset.name }) : <LoaderSpinner />}
        </ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <VStack spacing={4} alignItems="flex-start">
            <Text data-testid="wizard-mapper-instruction">{t('pulse.assets.operation.edit.overview')}</Text>
            <FormControl data-testid="wizard-mapper-selector-container">
              <FormLabel htmlFor="asset-mapper">
                {t('pulse.assets.operation.edit.select.title')}
                <MoreInfo
                  description={t('pulse.assets.operation.edit.select.moreInfo.title')}
                  link={t('pulse.assets.operation.edit.select.moreInfo.link')}
                />
              </FormLabel>

              <CreatableSelect<Combiner, false>
                id="wizard-mapper-selector"
                // menuIsOpen
                instanceId="mapper"
                inputId="asset-mapper"
                options={listMappers?.items || []}
                value={selectedValue}
                isClearable
                placeholder={t('pulse.assets.operation.edit.select.placeholder')}
                components={{
                  Option,
                  SingleValue,
                  Control,
                }}
                filterOption={createFilter({
                  stringify: (option) => {
                    const { name, description, sources } = option.data
                    return `${name}${description}${sources.items.map((source) => source.id).join('')}`
                  },
                })}
                formatCreateLabel={(name) => t('pulse.assets.operation.edit.select.createLabel', { name })}
                onChange={(e) => setSelectedValue(e || undefined)}
                onCreateOption={(name) =>
                  setSelectedValue({
                    name: t('pulse.assets.operation.edit.select.createValue', { name }),
                    id: uuidv4(),
                    __isNew__: true,
                    sources: { items: [] },
                    mappings: { items: [] },
                  })
                }
              />
              <FormHelperText>The asset to use for the new mapping</FormHelperText>
            </FormControl>
          </VStack>
        </ModalBody>

        <ModalFooter>
          <Button
            variant="primary"
            onClick={() => {
              if (selectedValue && onSubmit) onSubmit(selectedValue)
            }}
            isDisabled={!selectedValue}
          >
            {t('pulse.assets.operation.edit.submit', {
              context: selectedValue?.__isNew__ ? MAPPER_OPERATION.CREATE : MAPPER_OPERATION.EDIT,
            })}
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}

export default AssetMapperWizard
