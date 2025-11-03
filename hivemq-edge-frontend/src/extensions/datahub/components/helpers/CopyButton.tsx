import type { FC } from 'react'
import { useEffect, useState } from 'react'
import { Button, Icon, useToast } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuCopy, LuCheck } from 'react-icons/lu'

export interface CopyButtonProps {
  /**
   * The text content to copy to clipboard
   */
  content: string
  /**
   * Button size (default: 'xs')
   */
  size?: 'xs' | 'sm' | 'md' | 'lg'
  /**
   * Button label text (optional, defaults to "Copy")
   */
  label?: string
  /**
   * data-testid for testing
   */
  'data-testid'?: string
}

const COPY_LATENCY = 2000

/**
 * Reusable copy-to-clipboard button with visual feedback.
 * Shows a checkmark icon and toast notification when content is copied.
 * Uses clipboard API directly (not useClipboard hook) to avoid permission issues in tests.
 */
export const CopyButton: FC<CopyButtonProps> = ({ content, size = 'xs', label, 'data-testid': dataTestId }) => {
  const { t } = useTranslation('datahub')
  const toast = useToast()
  const [hasCopied, setHasCopied] = useState(false)

  useEffect(() => {
    if (!hasCopied) return

    const timeout = setTimeout(() => {
      setHasCopied(false)
    }, COPY_LATENCY)

    return () => clearTimeout(timeout)
  }, [hasCopied])

  const handleCopy = () => {
    navigator.clipboard.writeText(content).then(
      () => {
        setHasCopied(true)
        toast({
          title: t('workspace.dryRun.report.success.details.json.copied'),
          status: 'success',
          duration: 2000,
          isClosable: true,
        })
      },
      () => {
        // Handle error silently or show error toast if needed
        toast({
          title: 'Failed to copy',
          status: 'error',
          duration: 2000,
          isClosable: true,
        })
      }
    )
  }

  const buttonLabel = label || (hasCopied ? t('workspace.dryRun.report.success.details.json.copied') : 'Copy')

  return (
    <Button
      size={size}
      onClick={handleCopy}
      leftIcon={<Icon as={hasCopied ? LuCheck : LuCopy} />}
      data-testid={dataTestId}
    >
      {buttonLabel}
    </Button>
  )
}

export default CopyButton
