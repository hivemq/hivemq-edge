/**
 * Cli Handle Unhandled.
 *
 * Custom error handleError for the script crash on unhandled rejections.
 */
const handleError = require('cli-handle-error');

module.exports = () => {
	process.on('unhandledRejection', err => {
		handleError(`UNHANDLED ERROR`, err);
	});
};
