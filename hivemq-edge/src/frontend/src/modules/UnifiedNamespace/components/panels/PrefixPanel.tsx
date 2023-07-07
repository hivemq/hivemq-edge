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
  Switch,
} from '@chakra-ui/react'
import NamespaceDisplay from '@/modules/UnifiedNamespace/components/NamespaceDisplay.tsx'
import { ISA95Namespace } from '@/modules/UnifiedNamespace/types.ts'
import { useTranslation } from 'react-i18next'

interface PrefixPanelProps {
  data: ISA95Namespace
}

const PrefixPanel: FC<PrefixPanelProps> = ({ data }) => {
  const { t } = useTranslation()

  return (
    <Card>
      <CardHeader>
        <Heading as="h2" size="md">
          My namespace prefix
        </Heading>
      </CardHeader>
      <CardBody>
        <NamespaceDisplay namespace={data} />
      </CardBody>
      <CardFooter>
        <FormControl>
          <FormLabel htmlFor={'unifiedNamespace-enabled'}>{t('unifiedNamespace.enabled.label')}</FormLabel>
          <Switch id={'unifiedNamespace-enabled'} isChecked colorScheme={'brand'} />
          <FormHelperText>{t('unifiedNamespace.enabled.helper')}</FormHelperText>
        </FormControl>
      </CardFooter>
    </Card>
  )
}

export default PrefixPanel
