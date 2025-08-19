import { ConditionalWrapper } from '@/components/ConditonalWrapper.tsx'
import { Tag, TagLeftIcon } from '@chakra-ui/react'
import type { FC, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { FaKey } from 'react-icons/fa'

interface PrimaryWrapperProps {
  isPrimary: boolean
  children: ReactElement
}

export const PrimaryWrapper: FC<PrimaryWrapperProps> = ({ children, isPrimary }) => {
  const { t } = useTranslation()

  return (
    <ConditionalWrapper
      condition={isPrimary}
      wrapper={(children) => (
        <Tag data-testid="primary-wrapper" role="group" p={1} variant="outline">
          <TagLeftIcon boxSize="12px" as={FaKey} ml={1} aria-label={t('combiner.schema.mapping.primary.aria-label')} />
          {children}
        </Tag>
      )}
    >
      {children}
    </ConditionalWrapper>
  )
}
