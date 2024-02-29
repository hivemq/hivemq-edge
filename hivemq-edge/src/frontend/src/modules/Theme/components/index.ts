import { statTheme } from './Stat'
import { drawerTheme } from './Drawer.ts'
import { formControlTheme } from './FormControl.ts'
import { formErrorMessageTheme } from './FormErrorMessage.ts'
import { buttonTheme } from './Button.ts'
import { spinnerTheme } from './Spinner.ts'

const components = {
  Spinner: spinnerTheme,
  Button: buttonTheme,
  Stat: statTheme,
  Drawer: drawerTheme,
  Form: formControlTheme,
  FormError: formErrorMessageTheme,
}

export default components
