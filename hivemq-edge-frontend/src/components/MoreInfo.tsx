import { ExternalLinkIcon } from '@chakra-ui/icons'
import type { FC } from 'react'
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
  PopoverBody,
  PopoverArrow,
  Icon,
  Text,
  IconButton,
  Link,
  VStack,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { LuInfo } from 'react-icons/lu'

interface MoreInfoProps {
  description: string
  link?: string
}

const MoreInfo: FC<MoreInfoProps> = ({ description, link }) => {
  const { t } = useTranslation('components')

  return (
    <Popover placement="right">
      <PopoverTrigger>
        <IconButton
          size={'sm'}
          icon={<Icon as={LuInfo} />}
          aria-label={t('MoreInfo.title')}
          variant={'ghost'}
          data-testid={'more-info-trigger'}
        />
      </PopoverTrigger>
      <PopoverContent
        fontSize={'sm'}
        aria-label={t('MoreInfo.title')}
        boxShadow={'var(--chakra-shadows-dark-lg)'}
        data-testid={'more-info-popover'}
      >
        <PopoverArrow />
        <PopoverBody as={VStack} alignItems="flex-start">
          <Text data-testid={'more-info-message'}>{description}</Text>
          {link && (
            <Link href={link} isExternal data-testid={'more-info-link'}>
              {t('MoreInfo.link')} <ExternalLinkIcon mx="2px" />
            </Link>
          )}
        </PopoverBody>
      </PopoverContent>
    </Popover>
  )
}

export default MoreInfo
