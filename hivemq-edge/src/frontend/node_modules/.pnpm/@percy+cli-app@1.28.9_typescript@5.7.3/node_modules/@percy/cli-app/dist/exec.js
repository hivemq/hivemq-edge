import command from '@percy/cli-command';
import * as ExecPlugin from '@percy/cli-exec';
export const ping = ExecPlugin.ping;
export const stop = ExecPlugin.stop;
export const start = command('start', {
  description: 'Starts a locally running Percy process for native apps',
  examples: ['$0 &> percy.log'],
  percy: {
    server: true,
    projectType: 'app',
    skipDiscovery: true
  }
}, ExecPlugin.start.callback);
export const exec = command('exec', {
  description: 'Start and stop Percy around a supplied command for native apps',
  usage: '[options] -- <command>',
  commands: [start, stop, ping],
  flags: ExecPlugin.default.definition
  // grouped flags are built-in flags
  .flags.filter(f => !f.group),
  percy: {
    server: true,
    projectType: 'app',
    skipDiscovery: true
  }
}, ExecPlugin.default.callback);
export default exec;