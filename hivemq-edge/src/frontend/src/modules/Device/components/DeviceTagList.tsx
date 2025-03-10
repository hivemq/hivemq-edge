import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardBody, CardHeader, Code, Flex, Heading, HStack, List, ListItem } from '@chakra-ui/react'
import { LuFileCog } from 'react-icons/lu'

import type { Adapter, DomainTagList } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { PLCTag } from '@/components/MQTT/EntityTag.tsx'
import ArrayItemDrawer from '@/components/rjsf/SplitArrayEditor/components/ArrayItemDrawer.tsx'
import { formatTagDataPoint } from '@/modules/Device/utils/tags.utils.ts'
import { useTagManager } from '@/modules/Device/hooks/useTagManager.ts'

interface DeviceTagListProps {
  adapter: Adapter
}

const DeviceTagList: FC<DeviceTagListProps> = ({ adapter }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, context, onupdateCollection } = useTagManager(adapter.id)

  const onHandleSubmit = (data: unknown) => {
    if (data) onupdateCollection(data as DomainTagList)
  }

  return (
    <Card size="sm">
      <CardHeader>
        <Flex>
          <Flex flex="1" alignItems="center" flexWrap="wrap">
            <Heading size="sm">{t('device.drawer.tagList.title')}</Heading>
          </Flex>
          <ArrayItemDrawer
            header={t('device.drawer.tagEditor.title')}
            context={context}
            onSubmit={onHandleSubmit}
            trigger={({ onOpen: onOpenArrayDrawer }) => (
              <IconButton
                variant="primary"
                aria-label={t('device.drawer.tagList.cta.edit')}
                icon={<LuFileCog />}
                isDisabled={isLoading || isError}
                onClick={onOpenArrayDrawer}
              />
            )}
          />
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
            {data.items?.map((domainTag) => (
              <ListItem key={domainTag.name} m={1} display="flex" justifyContent="space-between">
                <HStack w="100%" justifyContent="space-between">
                  <PLCTag tagTitle={domainTag.name} />{' '}
                  <Code size="xs" textAlign="end">
                    {formatTagDataPoint(domainTag.definition)}
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
