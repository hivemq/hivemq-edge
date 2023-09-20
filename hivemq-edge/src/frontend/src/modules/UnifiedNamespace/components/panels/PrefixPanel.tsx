import { FC } from 'react'
import {
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  FormControl,
  FormHelperText,
  FormLabel,
  Heading,
  Skeleton,
  Switch,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { ApiError, ISA95ApiBean } from '@/api/__generated__'
import { useSetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useSetUnifiedNamespace.tsx'
import NamespaceDisplay from '@/modules/UnifiedNamespace/components/NamespaceDisplay.tsx'
import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

interface PrefixPanelProps {
  data: ISA95ApiBean
  isLoading?: boolean
}

const PrefixPanel: FC<PrefixPanelProps> = ({ data, isLoading }) => {
  const { t } = useTranslation()
  const { mutateAsync } = useSetUnifiedNamespace()
  const { successToast, errorToast } = useEdgeToast()

  const handleOnChange = () => {
    const isDisabling = data.enabled
    mutateAsync({ requestBody: { ...data, enabled: !data.enabled } })
      .then(() => {
        successToast({
          status: isDisabling ? 'info' : 'success',
          title: t('unifiedNamespace.toast.update.title'),
          description: !isDisabling
            ? t('unifiedNamespace.toast.update.description')
            : t('unifiedNamespace.toast.disabled.description'),
        })
      })
      .catch((err: ApiError) =>
        errorToast(
          { title: t('unifiedNamespace.toast.update.title'), description: t('unifiedNamespace.toast.update.error') },
          err
        )
      )
  }
  return (
    <Card>
      <CardHeader>
        <Skeleton isLoaded={!isLoading}>
          <Heading as="h2" size="md">
            My namespace prefix
          </Heading>
        </Skeleton>
      </CardHeader>
      <CardBody>
        <Skeleton isLoaded={!isLoading}>
          <NamespaceDisplay namespace={data} />
        </Skeleton>
      </CardBody>
      <CardFooter>
        <Skeleton isLoaded={!isLoading}>
          <FormControl>
            <FormLabel htmlFor={'unifiedNamespace-enabled'}>{t('unifiedNamespace.enabled.label')}</FormLabel>
            <Switch
              id={'unifiedNamespace-enabled'}
              colorScheme={'brand'}
              isChecked={data.enabled}
              onChange={handleOnChange}
            />
            <FormHelperText>{t('unifiedNamespace.enabled.helper')}</FormHelperText>
          </FormControl>
        </Skeleton>
      </CardFooter>
    </Card>
  )
}

export default PrefixPanel
