/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type StatusTransitionCommand = {
    /**
     * The command to perform on the target connection.
     */
    command?: StatusTransitionCommand.command;
};

export namespace StatusTransitionCommand {

    /**
     * The command to perform on the target connection.
     */
    export enum command {
        START = 'START',
        STOP = 'STOP',
        RESTART = 'RESTART',
    }


}

