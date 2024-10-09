import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Alert,
  AlertDescription,
  AlertIcon,
  Avatar,
  Box,
  Card,
  CardBody,
  CardHeader,
  Flex,
  Heading,
  Text,
} from '@chakra-ui/react'
import { LuUpload } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { DeviceMetadata } from '@/modules/Workspace/types.ts'

interface DeviceMetadataProps {
  protocolAdapter?: DeviceMetadata
}

const DeviceMetadataViewer: FC<DeviceMetadataProps> = ({ protocolAdapter }) => {
  const { t } = useTranslation()

  return (
    <Card size="sm">
      <CardHeader>
        <Flex>
          <Flex flex="1" gap="4" alignItems="center" flexWrap="wrap">
            <Avatar src={protocolAdapter?.logoUrl} />
            <Box>
              <Heading size="sm">{protocolAdapter?.id}</Heading>
              <Text>{protocolAdapter?.category?.displayName}</Text>
            </Box>
          </Flex>
          <IconButton aria-label={t('device.drawer.metadataPanel.cta.load')} icon={<LuUpload />} isDisabled />
        </Flex>
      </CardHeader>
      <CardBody>
        <Alert status="info">
          <AlertIcon />
          <AlertDescription>{t('device.errors.noMetadataLoaded')}</AlertDescription>
        </Alert>
      </CardBody>
    </Card>
  )
}

export default DeviceMetadataViewer
