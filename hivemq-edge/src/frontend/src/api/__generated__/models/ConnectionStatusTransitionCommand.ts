/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type ConnectionStatusTransitionCommand = {
    /**
     * The command to perform on the target connection.
     */
    command?: ConnectionStatusTransitionCommand.command;
};

export namespace ConnectionStatusTransitionCommand {

    /**
     * The command to perform on the target connection.
     */
    export enum command {
        CONNECT = 'CONNECT',
        DISCONNECT = 'DISCONNECT',
        RESTART = 'RESTART',
    }


}

