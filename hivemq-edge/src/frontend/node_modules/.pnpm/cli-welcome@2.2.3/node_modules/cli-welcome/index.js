/* CLI WELCOME */
const chalk = require('chalk');
const dim = chalk.dim;
const clearConsole = require('clear-any-console');

/**
 * Welcome.
 *
 * @param String heading Heading text.
 * @param String subHeading Sub heading text.
 * @param Object options Configurable options.
 */
module.exports = (options = {}) => {
	// Options.
	const defaultOptions = {
		title: 'ADD A HEADING',
		tagLine: '',
		description: '',
		bgColor: '#ffffff',
		color: '#000000',
		bold: true,
		clear: true,
		version: ''
	};
	const opts = {...defaultOptions, ...options};
	const {
		title,
		tagLine,
		description,
		bgColor,
		color,
		bold,
		clear,
		version
	} = opts;

	// Configure.
	const bg = bold
		? chalk.hex(bgColor).inverse.bold
		: chalk.hex(bgColor).inverse;
	const clr = bold ? chalk.hex(color).bold : chalk.hex(color);
	clear && clearConsole();

	// Do it.
	console.log();
	console.log(
		`${clr(`${bg(` ${title} `)}`)} v${version} ${dim(tagLine)}\n${dim(
			description
		)}`
	);
	console.log();
};
