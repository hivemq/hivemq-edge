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

import type { Combiner, DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { useListAssetMappers } from '@/api/hooks/useAssetMapper'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { HqAssets } from '@/components/Icons'
import MoreInfo from '@/components/MoreInfo.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import EntityReferencesWizard from '@/modules/Pulse/components/assets/EntityReferencesWizard.tsx'
import { DEFAULT_ASSET_MAPPER_SOURCES } from '@/modules/Pulse/utils/assets.utils.ts'

interface AssetMapperWizardProps {
  assetId: string
  onClose: () => void
  onSubmit?: (assetMapper: Combiner, isNew: boolean) => void
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

  if (!asset) return <ErrorMessage type={t('pulse.error.asset.notFound', { assetId })} />

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
                noOptionsMessage={() => t('pulse.assets.operation.edit.select.noOptions')}
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
                    sources: { items: DEFAULT_ASSET_MAPPER_SOURCES },
                    mappings: { items: [] },
                  })
                }
              />
              <FormHelperText>
                {!selectedValue && t('pulse.assets.operation.edit.select.helper')}
                {selectedValue && (
                  <Text data-testid="wizard-mapper-selector-instruction">
                    {t('pulse.assets.operation.edit.workspace', {
                      context: selectedValue?.__isNew__ ? MAPPER_OPERATION.CREATE : MAPPER_OPERATION.EDIT,
                    })}
                  </Text>
                )}
              </FormHelperText>
            </FormControl>
            {selectedValue && selectedValue.__isNew__ && (
              <EntityReferencesWizard
                values={selectedValue.sources.items}
                onChange={(newValues) => {
                  setSelectedValue({ ...selectedValue, sources: { items: newValues } })
                }}
              />
            )}
          </VStack>
        </ModalBody>

        <ModalFooter>
          <VStack>
            <Button
              variant="primary"
              onClick={() => {
                if (selectedValue && onSubmit) {
                  const { __isNew__, ...rest } = selectedValue
                  const newMapping: DataCombining = {
                    id: uuidv4(),
                    sources: {
                      // This is annoying, we should have the APi to accept nullable
                      primary: { id: '', type: DataIdentifierReference.type.TAG },
                      tags: [],
                      topicFilters: [],
                    },
                    destination: { topic: asset.topic, assetId: asset.id, schema: asset.schema },
                    instructions: [],
                  }
                  rest.mappings.items.unshift(newMapping)
                  onSubmit(rest, Boolean(__isNew__))
                }
              }}
              isDisabled={!selectedValue}
            >
              {t('pulse.assets.operation.edit.submit', {
                context: selectedValue?.__isNew__ ? MAPPER_OPERATION.CREATE : MAPPER_OPERATION.EDIT,
              })}
            </Button>
          </VStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}

export default AssetMapperWizard
