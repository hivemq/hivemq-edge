/*
  @license
	Rollup.js v4.34.4
	Wed, 05 Feb 2025 21:30:40 GMT - commit 19312a762c3cda56a0f6dc80a0887a4499db2257

	https://github.com/rollup/rollup

	Released under the MIT License.
*/
export { version as VERSION, defineConfig, rollup, watch } from './shared/node-entry.js';
import './shared/parseAst.js';
import '../native.js';
import 'node:path';
import 'path';
import 'node:process';
import 'node:perf_hooks';
import 'node:fs/promises';
