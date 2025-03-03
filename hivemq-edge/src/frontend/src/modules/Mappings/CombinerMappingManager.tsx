import { type FC, useEffect, useMemo } from 'react'
import type { Node, NodeRemoveChange } from 'reactflow'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
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

import config from '@/config'

import type { CombinerContext } from './types'
import { MappingType } from './types'
import type { Combiner } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { combinerMappingJsonSchema } from '@/api/schemas/combiner-mapping.json-schema'
import { combinerMappingUiSchema } from '@/api/schemas/combiner-mapping.ui-schema'
import { useUpdateCombiner, useDeleteCombiner } from '@/api/hooks/useCombiners/'
import { useGetCombinedEntities } from '@/api/hooks/useDomainModel/useGetCombinedEntities'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import ErrorMessage from '@/components/ErrorMessage'
import type { NodeTypes } from '@/modules/Workspace/types.ts'
import { IdStubs } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import DangerZone from './components/DangerZone'
import { useValidateCombiner } from './hooks/useValidateCombiner'

const CombinerMappingManager: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { combinerId } = useParams()
  const { nodes, onUpdateNode, onNodesChange } = useWorkspaceStore()
  const toast = useToast()

  const selectedNode = useMemo(() => {
    return nodes.find((node) => node.id === combinerId) as Node<Combiner> | undefined
  }, [combinerId, nodes])

  const entities = useMemo(() => {
    const entities = selectedNode?.data?.sources?.items || []
    const isBridgeIn = Boolean(
      entities.find((entity) => entity.id === IdStubs.EDGE_NODE && entity.type === EntityType.EDGE_BROKER)
    )
    if (!isBridgeIn) entities.push({ id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER })
    return entities
  }, [selectedNode?.data?.sources?.items])

  const sources = useGetCombinedEntities(entities)
  const validateCombiner = useValidateCombiner(sources)
  const updateCombiner = useUpdateCombiner()
  const deleteCombiner = useDeleteCombiner()

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  const handleOnSubmit = (data: IChangeEvent) => {
    if (!data.formData || !combinerId) return

    const promise = updateCombiner.mutateAsync({ combinerId, requestBody: data.formData })

    toast.promise(
      promise.then(() => {
        if (selectedNode) onUpdateNode<Combiner>(selectedNode.id, data.formData)
        handleClose()
      }),
      {
        success: { title: t('combiner.toast.update.title'), description: t('combiner.toast.update.success') },
        error: { title: t('combiner.toast.update.title'), description: t('combiner.toast.update.error') },
        loading: { title: t('combiner.toast.update.title'), description: t('combiner.toast.loading') },
      }
    )
  }

  const handleOnDelete = () => {
    if (!combinerId) return
    const promise = deleteCombiner.mutateAsync({ combinerId })
    toast.promise(
      promise.then(() => {
        if (selectedNode) onNodesChange([{ id: selectedNode.id, type: 'remove' } as NodeRemoveChange])
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

  return (
    <Drawer isOpen={isOpen} placement="right" size={'lg'} onClose={handleClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent aria-label={t('protocolAdapter.mapping.manager.header', { context: MappingType.COMBINING })}>
        <DrawerCloseButton />
        <DrawerHeader>
          <Text>{t('protocolAdapter.mapping.manager.header', { context: MappingType.COMBINING })}</Text>
          <NodeNameCard
            name={selectedNode?.data.name}
            type={selectedNode?.type as NodeTypes}
            description={t('combiner.type')}
          />
        </DrawerHeader>
        <DrawerBody display="flex" flexDirection="column" gap={6}>
          {!selectedNode && <ErrorMessage message={t('combiner.error.noDataUri')} status="error" />}
          {selectedNode && (
            <ChakraRJSForm
              showNativeWidgets={showNativeWidgets}
              id="combiner-main-form"
              schema={combinerMappingJsonSchema}
              uiSchema={combinerMappingUiSchema}
              formData={selectedNode.data}
              onSubmit={handleOnSubmit}
              formContext={{ queries: sources, entities } as CombinerContext}
              customValidate={validateCombiner}
            />
          )}
        </DrawerBody>
        <DrawerFooter justifyContent={'space-between'}>
          <ButtonGroup>
            {config.isDevMode && (
              <FormControl display="flex" alignItems="center">
                <FormLabel htmlFor="email-alerts" mb="0">
                  {t('modals.native')}
                </FormLabel>
                <Switch id="email-alerts" isChecked={showNativeWidgets} onChange={setShowNativeWidgets.toggle} />
              </FormControl>
            )}
            {selectedNode && <DangerZone onSubmit={handleOnDelete} />}
          </ButtonGroup>
          {selectedNode && (
            <Button variant="primary" type="submit" form="combiner-main-form" isLoading={updateCombiner.isPending}>
              {t('combiner.actions.submit')}
            </Button>
          )}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

export default CombinerMappingManager
