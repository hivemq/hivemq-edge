import { Undefined } from './validations.js';
export default class TimeIt {
  static data = {};
  static enabled = process.env.PERCY_METRICS === 'true';
  static async run(store, func) {
    if (!this.enabled) return await func();
    const t1 = Date.now();
    try {
      return await func();
    } finally {
      if (Undefined(this.data[store])) this.data[store] = [];
      this.data[store].push(Date.now() - t1);
    }
  }
  static min(store) {
    return Math.min(...this.data[store]);
  }
  static max(store) {
    return Math.max(...this.data[store]);
  }
  static avg(store) {
    const vals = this.data[store];
    return vals.reduce((a, b) => a + b, 0) / vals.length;
  }
  static summary({
    includeVals
  } = {}) {
    const agg = {};
    for (const key of Object.keys(this.data)) {
      agg[key] = {
        min: this.min(key),
        max: this.max(key),
        avg: this.avg(key),
        count: this.data[key].length
      };
      if (includeVals) agg[key].vals = this.data[key];
    }
    return agg;
  }
  static reset() {
    this.data = {};
  }
}
;