import type { FC } from 'react'
import { useMemo, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { useDisclosure } from '@chakra-ui/react'

import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'
import { managedAssetJsonSchema } from '@/api/schemas/managed-asset.json-schema.ts'
import { managedAssetUISchema } from '@/api/schemas/managed-asset.ui-schema.ts'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'
import ExpandableDrawer from '@/components/ExpandableDrawer/ExpandableDrawer.tsx'
import { NodeTypes } from '@/modules/Workspace/types.ts'

const ManagedAssetDrawer: FC = () => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const navigate = useNavigate()
  const { assetId } = useParams()
  const { data: listAssets } = useListManagedAssets()

  const selectedAsset = useMemo(() => {
    return listAssets?.items.find((asset) => asset.id === assetId)
  }, [assetId, listAssets?.items])

  if (!selectedAsset) throw new Error('No asset found')

  useEffect(() => {
    onOpen()
  }, [onOpen])

  const handleClose = () => {
    onClose()
    navigate('/pulse-assets')
  }

  return (
    <ExpandableDrawer
      header={t('workspace.property.header', { context: NodeTypes.PULSE_NODE })}
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
        // customValidate={validator?.validateCombiner}
      />
    </ExpandableDrawer>
  )
}

export default ManagedAssetDrawer
