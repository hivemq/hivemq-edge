import type { ParseResult } from 'langium';
import type { Info, InfoServices, Pie, PieServices, GitGraph, GitGraphServices } from '../src/language/index.js';
/**
 * A helper test function that validate that the result doesn't have errors
 * or any ambiguous alternatives from chevrotain.
 *
 * @param result - the result `parse` function.
 */
export declare function expectNoErrorsOrAlternatives(result: ParseResult): void;
export declare function createInfoTestServices(): {
    services: InfoServices;
    parse: (input: string) => ParseResult<Info>;
};
export declare const infoParse: (input: string) => ParseResult<Info>;
export declare function createPieTestServices(): {
    services: PieServices;
    parse: (input: string) => ParseResult<Pie>;
};
export declare const pieParse: (input: string) => ParseResult<Pie>;
export declare function createGitGraphTestServices(): {
    services: GitGraphServices;
    parse: (input: string) => ParseResult<GitGraph>;
};
export declare const gitGraphParse: (input: string) => ParseResult<GitGraph>;
