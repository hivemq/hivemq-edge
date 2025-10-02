import { type FC, useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import type { IChangeEvent } from '@rjsf/core'
import type { Node, NodeRemoveChange } from '@xyflow/react'
import debug from 'debug'

import {
  Button,
  ButtonGroup,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  FormControl,
  FormLabel,
  Switch,
  Text,
  useBoolean,
  useDisclosure,
  useToast,
} from '@chakra-ui/react'

import type { Combiner } from '@/api/__generated__'
import { AssetMapping, EntityType } from '@/api/__generated__'
import { useDeleteCombiner, useUpdateCombiner } from '@/api/hooks/useCombiners/'
import { useDeleteAssetMapper, useUpdateAssetMapper } from '@/api/hooks/useAssetMapper'
import { useGetCombinedEntities } from '@/api/hooks/useDomainModel/useGetCombinedEntities'
import { combinerMappingJsonSchema } from '@/api/schemas/combiner-mapping.json-schema'
import { combinerMappingUiSchema } from '@/api/schemas/combiner-mapping.ui-schema'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import { useUpdateManagedAsset } from '@/api/hooks/usePulse/useUpdateManagedAsset.ts'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { BASE_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'
import DangerZone from '@/modules/Mappings/components/DangerZone.tsx'
import type { CombinerContext } from '@/modules/Mappings/types.ts'
import { useValidateCombiner } from '@/modules/Mappings/hooks/useValidateCombiner.ts'
import { MappingType } from '@/modules/Mappings/types.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { IdStubs, NodeTypes } from '@/modules/Workspace/types.ts'

import config from '@/config'

const combinerLog = debug(`Combiner:CombinerMappingManager`)

const CombinerMappingManager: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { combinerId, tabId } = useParams()
  const { nodes, onUpdateNode, onNodesChange } = useWorkspaceStore()
  const toast = useToast(BASE_TOAST_OPTION)

  const selectedNode = useMemo(() => {
    return nodes.find((node) => node.id === combinerId) as Node<Combiner> | undefined
  }, [combinerId, nodes])

  if (!selectedNode) throw new Error('No combiner node found')

  const entities = useMemo(() => {
    const entities = selectedNode.data.sources.items || []
    const isBridgeIn = Boolean(
      entities.find((entity) => entity.id === IdStubs.EDGE_NODE && entity.type === EntityType.EDGE_BROKER)
    )
    if (!isBridgeIn) entities.push({ id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER })
    return entities
  }, [selectedNode.data.sources.items])

  const isAssetManager = useMemo(() => {
    return entities?.some((e) => e.type === EntityType.PULSE_AGENT)
  }, [entities])

  const sources = useGetCombinedEntities(entities)
  // @ts-ignore wrong type; need a fix
  const validator = useValidateCombiner(sources, entities)
  // TODO[NVL] Need to split the manager between Combiner and AssetMapper; no need to have so many hooks not in use
  const updateCombiner = useUpdateCombiner()
  const deleteCombiner = useDeleteCombiner()
  const updateAssetMapper = useUpdateAssetMapper()
  const updateManagedAsset = useUpdateManagedAsset()
  const deleteAssetMapper = useDeleteAssetMapper()
  const { data: allAssets, error: errorAssets, isLoading: isAssetsLoading } = useListManagedAssets()

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  const handleSubmitAssetMapper = (combinerId: string, combiner: Combiner) => {
    let promises: Promise<unknown>[] = []

    if (errorAssets) {
      promises.push(Promise.reject(errorAssets))
    } else {
      // TODO[NVL] This is very inefficient. Some of the mappings have not been modified and should not be updated
      promises = combiner.mappings.items.reduce<Promise<unknown>[]>((acc, newMapping) => {
        const assetId = newMapping.destination.assetId
        if (!assetId) {
          acc.push(Promise.reject(t('combiner.error.noAssetId')))
          return acc
        }

        // TODO[36190] Having to find the asset in order to only modify the mapping is highly inefficient (it requires a fetch)
        //            We should be able to update the mapping directly without fetching the whole asset (e.g. PATCH)
        const source = allAssets?.items.find((f) => f.id === assetId)
        if (!source) {
          acc.push(Promise.reject(t('combiner.error.noAssetFound')))
          return acc
        }

        const assetPromise = updateManagedAsset.mutateAsync({
          assetId,
          requestBody: {
            ...source,
            mapping: {
              status: AssetMapping.status.STREAMING,
              mappingId: newMapping.id,
            },
          },
        })
        acc.push(assetPromise)

        return acc
      }, promises)

      const mapperPromise = updateAssetMapper.mutateAsync({ combinerId, requestBody: combiner })
      promises.push(mapperPromise)
    }

    return Promise.all(promises)
  }

  const handleOnSubmit = (data: IChangeEvent) => {
    if (!data.formData || !combinerId) return

    const promise = isAssetManager
      ? handleSubmitAssetMapper(combinerId, data.formData)
      : updateCombiner.mutateAsync({ combinerId, requestBody: data.formData })

    toast.promise(
      promise.then(() => {
        onUpdateNode<Combiner>(selectedNode.id, data.formData)
        handleClose()
      }),
      {
        success: { title: t('combiner.toast.update.title'), description: t('combiner.toast.update.success') },
        error: (e) => {
          combinerLog(`Error publishing the ${isAssetManager ? t('pulse.mapper.title') : t('combiner.type')}`, e)
          return {
            title: t('combiner.toast.update.title'),
            description: (
              <>
                <Text>{t('combiner.toast.update.error')}</Text>
                {e.message && <Text>{e.message}</Text>}
              </>
            ),
          }
        },
        loading: { title: t('combiner.toast.update.title'), description: t('combiner.toast.loading') },
      }
    )
  }

  const handleOnDelete = () => {
    if (!combinerId) return
    const promise = isAssetManager
      ? deleteAssetMapper.mutateAsync({ combinerId })
      : deleteCombiner.mutateAsync({ combinerId })
    toast.promise(
      promise.then(() => {
        onNodesChange([{ id: selectedNode.id, type: 'remove' } as NodeRemoveChange])
        handleClose()
      }),
      {
        success: { title: t('combiner.toast.delete.title'), description: t('combiner.toast.delete.success') },
        error: { title: t('combiner.toast.delete.title'), description: t('combiner.toast.delete.error') },
        loading: { title: t('combiner.toast.delete.title'), description: t('combiner.toast.loading') },
      }
    )
  }

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const [showNativeWidgets, setShowNativeWidgets] = useBoolean()

  const header = isAssetManager
    ? t('pulse.mapper.manager.header')
    : t('protocolAdapter.mapping.manager.header', { context: MappingType.COMBINING })

  return (
    <Drawer
      isOpen={isOpen}
      placement="right"
      size="lg"
      onClose={handleClose}
      variant="hivemq"
      closeOnOverlayClick={false}
    >
      <DrawerOverlay />
      <DrawerContent aria-label={header}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text>{header}</Text>
          <NodeNameCard
            name={selectedNode.data.name}
            type={isAssetManager ? NodeTypes.ASSETS_NODE : NodeTypes.COMBINER_NODE}
            description={isAssetManager ? t('pulse.mapper.title') : t('combiner.type')}
          />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          <ChakraRJSForm
            showNativeWidgets={showNativeWidgets}
            id="combiner-main-form"
            schema={combinerMappingJsonSchema}
            uiSchema={combinerMappingUiSchema(isAssetManager, tabId)}
            formData={selectedNode.data}
            onSubmit={handleOnSubmit}
            formContext={{ queries: sources, entities } as CombinerContext}
            customValidate={validator?.validateCombiner}
          />
        </DrawerBody>
        <DrawerFooter justifyContent="space-between">
          <ButtonGroup>
            {config.isDevMode && (
              <FormControl display="flex" alignItems="center">
                <FormLabel htmlFor="modal-native-switch" mb="0">
                  {t('modals.native')}
                </FormLabel>
                <Switch id="modal-native-switch" isChecked={showNativeWidgets} onChange={setShowNativeWidgets.toggle} />
              </FormControl>
            )}
            <DangerZone onSubmit={handleOnDelete} />
          </ButtonGroup>
          <Button
            variant="primary"
            type="submit"
            form="combiner-main-form"
            // TODO[NVL] Asset loading unsatisfactory; if there is an error, form should not even be available
            isLoading={isAssetsLoading || updateCombiner.isPending}
          >
            {t('combiner.actions.submit')}
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default CombinerMappingManager
