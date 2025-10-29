/**
 * Apply Layout Button Component
 *
 * Simple button to apply the current layout algorithm to the workspace.
 */

import { type FC, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Icon, Tooltip, useToast } from '@chakra-ui/react'
import { LuNetwork } from 'react-icons/lu'
import config from '@/config'
import { useLayoutEngine } from '../../hooks/useLayoutEngine.ts'

const ApplyLayoutButton: FC = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { applyLayout, currentAlgorithmInstance } = useLayoutEngine()
  const [isApplying, setIsApplying] = useState(false)

  if (!config.features.WORKSPACE_AUTO_LAYOUT) {
    return null
  }

  const handleApplyLayout = async () => {
    if (!currentAlgorithmInstance) {
      toast({
        title: t('workspace.autoLayout.error.no-algorithm'),
        description: t('workspace.autoLayout.error.select-algorithm'),
        status: 'warning',
        duration: 3000,
        isClosable: true,
      })
      return
    }

    setIsApplying(true)

    try {
      const result = await applyLayout()

      if (result?.success) {
        toast({
          title: t('workspace.autoLayout.success.title'),
          description: t('workspace.autoLayout.success.description', {
            algorithm: currentAlgorithmInstance.name,
            duration: result.duration.toFixed(0),
            nodes: result.metadata?.nodeCount || 0,
          }),
          status: 'success',
          duration: 3000,
          isClosable: true,
        })
      } else {
        toast({
          title: t('workspace.autoLayout.error.failed'),
          description: result?.error || t('workspace.autoLayout.error.unknown'),
          status: 'error',
          duration: 5000,
          isClosable: true,
        })
      }
    } catch (error) {
      toast({
        title: t('workspace.autoLayout.error.failed'),
        description: error instanceof Error ? error.message : String(error),
        status: 'error',
        duration: 5000,
        isClosable: true,
      })
    } finally {
      setIsApplying(false)
    }
  }

  const isMac = typeof navigator !== 'undefined' && navigator.platform.toUpperCase().indexOf('MAC') >= 0
  const shortcutKey = isMac ? '⌘L' : 'Ctrl+L'

  return (
    <Tooltip label={`${t('workspace.autoLayout.apply.tooltip')} (${shortcutKey})`} placement="bottom">
      <Button
        data-testid="workspace-apply-layout"
        leftIcon={<Icon as={LuNetwork} />}
        size="sm"
        variant="outline"
        onClick={handleApplyLayout}
        isLoading={isApplying}
        loadingText={t('workspace.autoLayout.apply.loading')}
      >
        {t('workspace.autoLayout.apply.label')}
      </Button>
    </Tooltip>
  )
}

export default ApplyLayoutButton
