import { FC } from 'react'
import { LuHardDriveUpload } from 'react-icons/lu'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { Button, useDisclosure } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

export interface BatchSubscriptionProps {
  schema: RJSFSchema
}

const BatchUploadButton: FC<BatchSubscriptionProps> = ({ schema }) => {
  const { onOpen } = useDisclosure()
  const { t } = useTranslation('components')

  console.log('XXXXXX schema', schema)

  return (
    <Button colorScheme="red" onClick={onOpen} leftIcon={<LuHardDriveUpload />}>
      {t('rjsf.BatchUpload.Button')}
    </Button>
  )
}

export default BatchUploadButton
