import command from '@percy/cli-command';
import finalize from './finalize.js';
import wait from './wait.js';
import id from './id.js';
export const build = command('build', {
  description: 'Finalize and wait on Percy builds',
  commands: [finalize, wait, id]
});
export default build;