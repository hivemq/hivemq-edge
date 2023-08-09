import React from 'react'

export interface OnboardingAction {
  title: string
  label: string
  to: string
  leftIcon: React.ReactElement
}

export interface OnboardingTask {
  header: string
  sections: OnboardingAction[]
}
