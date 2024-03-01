import { FC, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Tab, TabList, TabPanel, TabPanels, Tabs, Text, useDisclosure, useToast } from '@chakra-ui/react'
import { UseMutateAsyncFunction } from '@tanstack/react-query'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'

import PolicyTable from '@datahub/components/pages/PolicyTable.tsx'
import SchemaTable from '@datahub/components/pages/SchemaTable.tsx'
import ScriptTable from '@datahub/components/pages/ScriptTable.tsx'
import { dataHubToastOption } from '@datahub/utils/toast.utils.ts'

interface DeleteMutationRequest {
  mutation: UseMutateAsyncFunction<void, unknown, string, unknown>
  type: string
  id: string
}

const DataHubListings: FC = () => {
  const { t } = useTranslation('datahub')
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()
  const [deleteItem, setDeleteItem] = useState<DeleteMutationRequest | undefined>(undefined)
  const toast = useToast()

  const handleConfirmOnClose = () => {
    onConfirmDeleteClose()
    setDeleteItem(undefined)
  }

  const handleConfirmOnSubmit = () => {
    console.log(deleteItem)
    if (!deleteItem?.mutation) return
    deleteItem
      ?.mutation(deleteItem?.id)
      .then(() =>
        toast({
          ...dataHubToastOption,
          title: t('error.delete.title', { source: deleteItem?.type }),
          status: 'success',
        })
      )
      .catch((e) =>
        toast({
          ...dataHubToastOption,
          title: t('error.delete.error', { source: deleteItem?.type }),
          description: e.toString(),
          status: 'error',
        })
      )
  }

  const handleOnDelete = (
    mutation: UseMutateAsyncFunction<void, unknown, string, unknown>,
    type: string,
    id: string
  ) => {
    onConfirmDeleteOpen()
    setDeleteItem({ mutation, type, id })
  }

  return (
    <Tabs isLazy colorScheme="brand" data-testid="list-tabs">
      <TabList>
        <Tab fontSize="lg" fontWeight="bold">
          {t('Listings.tabs.policy.title')}
        </Tab>
        <Tab fontSize="lg" fontWeight="bold">
          {t('Listings.tabs.schema.title')}
        </Tab>
        <Tab fontSize="lg" fontWeight="bold">
          {t('Listings.tabs.script.title')}
        </Tab>
      </TabList>

      <TabPanels>
        <TabPanel>
          <Text mb={3}>{t('Listings.tabs.policy.description')}</Text>
          <PolicyTable onDeleteItem={handleOnDelete} />
        </TabPanel>
        <TabPanel>
          <Text mb={3}>{t('Listings.tabs.schema.description')}</Text>
          <SchemaTable />
        </TabPanel>
        <TabPanel>
          <Text mb={3}>{t('Listings.tabs.script.description')}</Text>
          <ScriptTable />
        </TabPanel>
      </TabPanels>
      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={handleConfirmOnClose}
        onSubmit={handleConfirmOnSubmit}
        message={t('Listings.modal.delete.header')}
        header={t('Listings.modal.delete.message')}
      />
    </Tabs>
  )
}

export default DataHubListings
