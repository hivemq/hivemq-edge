import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Button, HStack, Icon, Stack } from '@chakra-ui/react'
import { MdPublishedWithChanges } from 'react-icons/md'

export const ToolboxPublish: FC = () => {
  const { t } = useTranslation('datahub')

  const handlePublish = () => {
    return undefined
  }

  return (
    <Stack maxW={500}>
      <HStack>
        <Box>
          <Button
            leftIcon={<Icon as={MdPublishedWithChanges} boxSize="24px" />}
            onClick={handlePublish}
            isDisabled={true}
          >
            {t('workspace.toolbar.policy.publish')}
          </Button>
        </Box>
      </HStack>
    </Stack>
  )
}
