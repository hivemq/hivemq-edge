import { Box, Button } from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { GrFormNext, GrFormPrevious, GrValidate } from 'react-icons/gr'

interface StepperNavigationProps {
  activeStep: number
  goToPrevious: () => void
  goToNext: () => void
  onCancel: () => void
}

const StepperNavigation: FC<StepperNavigationProps> = ({ activeStep, goToPrevious, goToNext, onCancel }) => {
  const { t } = useTranslation()
  return (
    <Box>
      <Button
        isDisabled={activeStep === 0}
        onClick={goToPrevious}
        size={'sm'}
        variant="outline"
        leftIcon={<GrFormPrevious />}
      >
        {t('action.previous')}
      </Button>

      <Button isDisabled={activeStep === 4} onClick={goToNext} size={'sm'} variant="outline" leftIcon={<GrFormNext />}>
        {t('action.next')}
      </Button>

      <Button size={'sm'} variant="outline" leftIcon={<GrValidate />} ml={5} onClick={onCancel}>
        {t('action.cancel')}
      </Button>
    </Box>
  )
}

export default StepperNavigation
