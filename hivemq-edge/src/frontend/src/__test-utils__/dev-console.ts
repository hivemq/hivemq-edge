console.groupCollapsed(
  '%c[HiveMQ Edge][%s %s] Dev Mode started',
  'color:#ffc000;font-weight:bold;',
  import.meta.env.VITE_APP_VERSION,
  import.meta.env.MODE
)
console.log('Documentation', import.meta.env.VITE_APP_DOCUMENTATION)
console.log('Found an issue?', import.meta.env.VITE_APP_ISSUES)
console.groupEnd()
