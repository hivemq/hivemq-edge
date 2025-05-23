import command from '@percy/cli-command';
export const start = command('start', {
  description: 'Starts a locally running Percy process',
  examples: ['$0 &> percy.log'],
  percy: {
    server: true,
    projectType: 'web'
  }
}, async function* ({
  percy,
  log,
  exit
}) {
  if (!percy) exit(0, 'Percy is disabled');
  let {
    yieldFor
  } = await import('@percy/cli-command/utils');
  // Skip this for app because they are triggered as app:exec
  // Remove this once they move to exec command as well
  if (percy.projectType !== 'app') {
    percy.projectType = percy.client.tokenType();
    percy.skipDiscovery = percy.shouldSkipAssetDiscovery(percy.projectType);
  } else {
    log.debug('Skipping percy project attribute calculation');
  }
  try {
    // start percy
    yield* percy.yield.start();

    // run until stopped or terminated
    yield* yieldFor(() => percy.readyState >= 3);
  } catch (error) {
    log.error(error);
    await percy.stop(true);
    throw error;
  }
});
export default start;