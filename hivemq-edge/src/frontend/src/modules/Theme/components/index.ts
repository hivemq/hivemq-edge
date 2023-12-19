import { statTheme } from './Stat'
import { drawerTheme } from './Drawer.ts'
import { formControlTheme } from './FormControl.ts'
import { formErrorMessageTheme } from './FormErrorMessage.ts'
import { buttonTheme } from './Button.ts'

const components = {
  Button: buttonTheme,
  Stat: statTheme,
  Drawer: drawerTheme,
  Form: formControlTheme,
  FormError: formErrorMessageTheme,
}

export default components
