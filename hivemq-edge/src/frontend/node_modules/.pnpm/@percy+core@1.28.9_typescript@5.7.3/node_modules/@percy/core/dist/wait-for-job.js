import logger from '@percy/logger';
const MIN_POLLING_INTERVAL = 5_000;
// Poll atleast once in 2 min
const MAX_POLLING_INTERVAL_SECONDS = 120;
const THRESHOLD_OPTIMAL_POLL_TIME = 5;
const JOB_TIMEOUT = Number(process.env.SYNC_TIMEOUT) || 90_000;

// Job is either for snapshot or comparison only
export class WaitForJob {
  log = logger('core:wait-for-job');
  constructor(type, percy) {
    this.percy = percy;
    this.jobs = [];
    if (type !== 'comparison' && type !== 'snapshot') throw new Error('Type should be either comparison or snapshot');
    this.type = type;
    this.timer = null;
    this.exit = false;
    this.running = false;
  }
  push(job) {
    if (!(job instanceof JobData)) throw new Error('Invalid job passed, use JobData');
    if (this.type === 'snapshot') job.timeout += 420_000; // For snapshot timeout after 08:30 min

    this.jobs.push(job);
    if (!this.running) this.run();
  }
  run(interval = MIN_POLLING_INTERVAL) {
    if (this.exit) return;
    this.running = true;
    if (interval < MIN_POLLING_INTERVAL) {
      interval = MIN_POLLING_INTERVAL;
    }
    this.log.debug(`Polling for ${this.type} status in ${interval}ms`);
    this.timer = setTimeout(async () => {
      let nextPoll = MAX_POLLING_INTERVAL_SECONDS;
      const jobIds = this.jobs.map(job => job.id);
      const response = await this.percy.client.getStatus(this.type, jobIds);
      this.jobs = this.jobs.filter(job => {
        if (response[job.id]) {
          const jobStatus = response[job.id];
          if (jobStatus.status) {
            job.resolve(job.id);
            return false;
          } else if (jobStatus.error != null) {
            job.reject(jobStatus.error);
            return false;
          } else if (Date.now() - job.timeout >= 0) {
            job.reject(new Error(`Timeout waiting for ${this.type} with id ${job.id}`));
            return false;
          } else {
            job.nextPoll = jobStatus.next_poll;
          }
        }
        nextPoll = Math.min(nextPoll, job.nextPoll);
        return true;
      });
      if (this.jobs.length === 0) {
        this.running = false;
        return;
      }
      const optimalNextPollTime = this.getOptimalPollTime(nextPoll);
      this.run(optimalNextPollTime * 1000);
    }, interval);
  }

  // If there are other snapshots which can be completed in next
  // 5 seconds, calling after x seconds will reduce network call
  getOptimalPollTime(lowestPollTime) {
    let pollTime = lowestPollTime;
    this.jobs.forEach(job => {
      const jobPollTime = job.nextPoll;
      if (jobPollTime - lowestPollTime <= THRESHOLD_OPTIMAL_POLL_TIME) {
        pollTime = Math.max(pollTime, jobPollTime);
      }
    });
    return pollTime;
  }
  stop() {
    this.exit = true;
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    this.jobs.forEach(job => {
      job.reject(new Error('Unable to process synchronous results as the CLI was exited while awaiting completion of the snapshot.'));
    });
  }
}
export class JobData {
  constructor(id, nextPoll, resolve, reject) {
    this.id = id;
    this.nextPoll = nextPoll || 60;
    this.timeout = Date.now() + JOB_TIMEOUT;
    this.resolve = resolve;
    this.reject = reject;
  }
}