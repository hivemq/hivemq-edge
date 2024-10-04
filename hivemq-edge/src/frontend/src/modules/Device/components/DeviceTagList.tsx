import { FC } from 'react'
import { Card, CardBody, CardHeader, Code, Heading, HStack, List, ListItem } from '@chakra-ui/react'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.tsx'
import { Adapter } from '@/api/__generated__'
import { PLCTag } from '@/components/MQTT/EntityTag.tsx'
import { formatTagDataPoint } from '@/modules/Device/utils/tags.utils.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { useTranslation } from 'react-i18next'

interface DeviceTagListProps {
  adapter?: Adapter
}

const DeviceTagList: FC<DeviceTagListProps> = ({ adapter }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError } = useGetDomainTags(adapter?.id, adapter?.type)

  return (
    <Card size="sm">
      <CardHeader>
        <Heading size="sm">{t('XXXXX List of Device Tags')}</Heading>
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}
        {isError && <ErrorMessage message="XXXX Cannot load the tags" />}
        {!isError && !data?.items?.length && <ErrorMessage message="XXXX No tag defined" status="info" />}
        {!isError && data && (
          <List>
            {data.items?.map((e) => (
              <ListItem key={e.tag} m={1} display="flex" justifyContent="space-between">
                <HStack w="100%" justifyContent="space-between">
                  <PLCTag tagTitle={e.tag} />{' '}
                  <Code size="xs" textAlign="end">
                    {formatTagDataPoint(e.dataPoint)}
                  </Code>
                </HStack>
              </ListItem>
            ))}
          </List>
        )}
      </CardBody>
    </Card>
  )
}

export default DeviceTagList
