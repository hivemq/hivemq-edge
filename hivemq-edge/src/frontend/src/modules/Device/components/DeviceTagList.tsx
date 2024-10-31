import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardBody, CardHeader, Code, Flex, Heading, HStack, List, ListItem } from '@chakra-ui/react'

import { Adapter, DomainTagList } from '@/api/__generated__'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { PLCTag } from '@/components/MQTT/EntityTag.tsx'
import ArrayItemDrawer from '@/components/rjsf/SplitArrayEditor/components/ArrayItemDrawer.tsx'
import { formatTagDataPoint } from '@/modules/Device/utils/tags.utils.ts'
import { useTagManager } from '@/modules/Mappings/hooks/useTagManager.tsx'

interface DeviceTagListProps {
  adapter?: Adapter
}

const DeviceTagList: FC<DeviceTagListProps> = ({ adapter }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, context, onupdateCollection } = useTagManager(adapter?.id)

  const onHandleSubmit = (data: DomainTagList | undefined) => {
    if (data) onupdateCollection(data)
  }

  return (
    <Card size="sm">
      <CardHeader>
        <Flex>
          <Flex flex="1" alignItems="center" flexWrap="wrap">
            <Heading size="sm">{t('device.drawer.tagList.title')}</Heading>
          </Flex>
          <ArrayItemDrawer isDisabled={isLoading || isError} context={context} onSubmit={onHandleSubmit} />
        </Flex>
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}
        {isError && <ErrorMessage message={t('device.errors.noTagLoaded')} />}
        {!isError && !isLoading && !data?.items?.length && (
          <ErrorMessage message={t('device.errors.noTagCreated')} status="info" />
        )}
        {!isError && !isLoading && data && (
          // TODO[NVL] Too simple. Use a paginated table
          <List data-testid="device-tags-list">
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
