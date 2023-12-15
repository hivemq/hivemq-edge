import { BridgeSubscription } from '@/api/__generated__'
import Topic from '@/components/MQTT/Topic.tsx'
import SubscriptionEditor from '@/modules/Bridges/components/setup/SubscriptionEditor.tsx'
import { AddIcon, DeleteIcon, EditIcon } from '@chakra-ui/icons'
import { ButtonGroup, IconButton, Table, Tbody, Td, Th, Thead, Tr, useDisclosure } from '@chakra-ui/react'
import { FC, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useBridgeSetup } from '../../hooks/useBridgeConfig.tsx'

interface SubscriptionSetupProps {
  type: 'local' | 'remote'
}

const SubscriptionStep: FC<SubscriptionSetupProps> = ({ type }) => {
  const { bridge, setBridge } = useBridgeSetup()
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const finalFocusRef = useRef(null)

  const onHandleSubmit = (sub: BridgeSubscription) => {
    setBridge((old) => {
      const { localSubscriptions, remoteSubscriptions } = old
      const append = (arr: BridgeSubscription[] | undefined, sub: BridgeSubscription) => {
        return [...(arr || []), sub]
      }

      if (type === 'local') {
        return { ...old, localSubscriptions: append(localSubscriptions, sub) }
      }
      return { ...old, remoteSubscriptions: append(remoteSubscriptions, sub) }
    })
    onClose()
  }

  // TODO[NVL] Not a good idea to delete on index
  const onHandleDelete = (index: number) => {
    setBridge((old) => {
      const { localSubscriptions, remoteSubscriptions } = old
      const del = (arr: BridgeSubscription[] | undefined) => {
        const subs = [...(arr || [])]
        subs.splice(index, 1)
        return subs
      }

      if (type === 'local') {
        return { ...old, localSubscriptions: del(localSubscriptions) }
      }
      return { ...old, remoteSubscriptions: del(remoteSubscriptions) }
    })
  }

  const source = type === 'local' ? bridge.localSubscriptions : bridge.remoteSubscriptions

  return (
    <>
      <Table variant="simple" size="sm">
        {/*<TableCaption placement={'top'}>{t('bridge.subscription.type', { context: type })}</TableCaption>*/}
        <Thead>
          <Tr>
            <Th w={'80%'}>{t('bridge.subscription.filters.label')}</Th>
            <Th>{t('bridge.subscription.destination.label')}</Th>
            <Th>{t('bridge.subscription.maxQoS.label')}</Th>
            <Th>{t('bridge.subscription.actions')}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {source?.map((sub: BridgeSubscription, index) => (
            <Tr key={`${sub.destination}-${index}`}>
              <Td gap={1} display={'flex'} flexWrap={'wrap'}>
                {sub.filters?.map((topic, index) => (
                  <Topic key={`${topic}-${index}`} topic={topic} />
                ))}
              </Td>
              <Td>{sub.destination && <Topic topic={sub.destination} />}</Td>
              <Td>{sub.maxQoS}</Td>
              <Td>
                <ButtonGroup size="sm" isAttached variant="outline">
                  <IconButton isDisabled aria-label={t('bridge.subscription.edit')} icon={<EditIcon />} />
                  <IconButton
                    onClick={() => onHandleDelete(index)}
                    aria-label={t('bridge.subscription.delete')}
                    icon={<DeleteIcon />}
                  />
                </ButtonGroup>
              </Td>
            </Tr>
          ))}
          <Tr>
            <Td colSpan={3} />
            <Td>
              <ButtonGroup size="sm" isAttached variant="outline">
                <IconButton
                  aria-label={t('bridge.subscription.add')}
                  icon={<AddIcon />}
                  onClick={onOpen}
                  ref={finalFocusRef}
                />
              </ButtonGroup>
            </Td>
          </Tr>
        </Tbody>
      </Table>
      <SubscriptionEditor isOpen={isOpen} onClose={onClose} onSubmit={onHandleSubmit} finalFocusRef={finalFocusRef} />
    </>
  )
}

export default SubscriptionStep
