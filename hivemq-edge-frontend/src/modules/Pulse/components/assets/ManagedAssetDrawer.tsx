import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { useDisclosure } from '@chakra-ui/react'

import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import { managedAssetJsonSchema } from '@/api/schemas/managed-asset.json-schema.ts'
import { managedAssetUISchema } from '@/api/schemas/managed-asset.ui-schema.ts'
import { useSelectCombinerFromMapping } from '@/api/hooks/useCombiners/useSelectCombinerFromMapping.ts'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'
import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'
import { customSchemaValidator } from '@/modules/Pulse/utils/validation-utils.ts'

const ManagedAssetDrawer: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { assetId } = useParams()
  const { data: listAssets, isLoading, error } = useListManagedAssets()
  const { errorToast } = useEdgeToast()

  const selectedAsset = useMemo(() => {
    return listAssets?.items.find((asset) => asset.id === assetId)
  }, [assetId, listAssets?.items])

  const { data: sourceCombiner } = useSelectCombinerFromMapping(selectedAsset?.mapping.mappingId)

  useEffect(() => {
    if (assetId && selectedAsset) return
    if (error || (!error && listAssets && !selectedAsset)) {
      errorToast(
        {
          duration: 60000,
          id: assetId,
          title: t('pulse.error.asset.title'),
          description: t('pulse.error.asset.loading'),
        },
        error || new Error(t('pulse.error.asset.notFound', { assetId }))
      )
      navigate('/pulse-assets')
    }
  }, [isLoading, error, listAssets, selectedAsset, assetId, errorToast, t, navigate])

  useEffect(() => {
    if (!assetId || !selectedAsset) return
    onOpen()
  }, [assetId, onOpen, selectedAsset])

  const handleClose = () => {
    onClose()
    navigate('/pulse-assets')
  }

  return (
    <ExpandableDrawer
      header={t('pulse.assets.viewer.aria-label')}
      // subHeader={<NodeNameCard name={selectedNode.data.label} type={selectedNode.type as NodeTypes} />}
      isOpen={isOpen}
      onClose={handleClose}
      closeOnOverlayClick={true}
    >
      <ChakraRJSForm
        id="asset-editor"
        schema={managedAssetJsonSchema}
        uiSchema={managedAssetUISchema}
        formData={selectedAsset}
        onSubmit={() => undefined}
        // @ts-ignore Need to fix the type
        customValidate={customSchemaValidator(sourceCombiner)}
      />
    </ExpandableDrawer>
  )
}

export default ManagedAssetDrawer
