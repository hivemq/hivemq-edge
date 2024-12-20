import { FC } from 'react'
import { Avatar, Box, Card, CardHeader, Flex, Heading, Text } from '@chakra-ui/react'
import { DeviceMetadata } from '@/modules/Workspace/types.ts'

interface DeviceMetadataProps {
  device: DeviceMetadata
}

const DeviceMetadataViewer: FC<DeviceMetadataProps> = ({ device }) => {
  return (
    <Card size="sm">
      <CardHeader>
        <Flex data-testid="device-metadata-header">
          <Flex flex="1" gap="4" alignItems="center" flexWrap="wrap">
            <Avatar src={device.logoUrl} />
            <Box>
              <Heading size="sm">{device.id}</Heading>
              <Text>{device.category?.displayName}</Text>
            </Box>
          </Flex>
        </Flex>
      </CardHeader>
    </Card>
  )
}

export default DeviceMetadataViewer
