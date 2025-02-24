import { type FC, useEffect, useMemo } from 'react'
import type { Node } from 'reactflow'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import {
  Button,
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

import { MappingType } from './types'
import type { Combiner } from '@/api/__generated__'
import { combinerMappingJsonSchema } from '@/api/schemas/combiner-mapping.json-schema'
import { combinerMappingUiSchema } from '@/api/schemas/combiner-mapping.ui-schema'
import { useUpdateCombiner } from '@/api/hooks/useCombiners/useUpdateCombiner'
import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import ErrorMessage from '@/components/ErrorMessage'
import type { NodeTypes } from '@/modules/Workspace/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import NodeNameCard from '@/modules/Workspace/components/parts/NodeNameCard.tsx'
import { useGetCombinedEntities } from '../../api/hooks/useDomainModel/useGetCombinedEntities'

const CombinerMappingManager: FC = () => {
  const { t } = useTranslation()
  const [isExpanded, setExpanded] = useBoolean(true)
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { combinerId } = useParams()
  const { nodes, onUpdateNode } = useWorkspaceStore()
  const toast = useToast()
  const updateCombiner = useUpdateCombiner()

  const selectedNode = useMemo(() => {
    return nodes.find((node) => node.id === combinerId) as Node<Combiner> | undefined
  }, [combinerId, nodes])

  const entities = useMemo(() => {
    return selectedNode?.data?.sources?.items || []
  }, [selectedNode?.data?.sources?.items])

  const sources = useGetCombinedEntities(entities)

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  const handleUpdateCombiner = (data: Combiner) => {
    if (selectedNode) onUpdateNode<Combiner>(selectedNode.id, data)
    handleClose()
  }

  const handleOnSubmit = (data: IChangeEvent) => {
    if (!data.formData || !combinerId) return

    const promise = updateCombiner.mutateAsync({ combinerId: combinerId, requestBody: data.formData })

    toast.promise(
      promise.then(() => handleUpdateCombiner(data.formData)),
      {
        success: { title: t('combiner.toast.update.title'), description: t('combiner.toast.update.success') },
        error: { title: t('combiner.toast.update.title'), description: t('combiner.toast.update.error') },
        loading: { title: t('combiner.toast.update.title'), description: t('combiner.toast.loading') },
      }
    )
  }

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const [showNativeWidgets, setShowNativeWidgets] = useBoolean()

  return (
    <Drawer isOpen={isOpen} placement="right" size={isExpanded ? 'full' : 'lg'} onClose={handleClose} variant="hivemq">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerExpandButton isExpanded={isExpanded} toggle={setExpanded.toggle} />
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
              formContext={{ sources: sources }}
            />
          )}
        </DrawerBody>
        <DrawerFooter>
          {config.isDevMode && (
            <FormControl display="flex" alignItems="center">
              <FormLabel htmlFor="email-alerts" mb="0">
                {t('modals.native')}
              </FormLabel>
              <Switch id="email-alerts" isChecked={showNativeWidgets} onChange={setShowNativeWidgets.toggle} />
            </FormControl>
          )}
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
