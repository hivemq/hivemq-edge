/**
 * Cross platform clear console.
 *
 * Platform: macOS, Windows, and Linux.
 *
 * @author Ahmad Awais <https://twitter.com/MrAhmadAwais>
 */
module.exports = () => process.stdout.write(process.platform === 'win32' ? '\x1B[2J\x1B[0f' : '\x1B[2J\x1B[3J\x1B[H');
