import command from '@percy/cli-command';
import exec from './exec.js';
export const app = command('app', {
  description: 'Create Percy builds for native app snapshots',
  hidden: true,
  commands: [exec]
});
export default app;