/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $NotificationList = {
    properties: {
        items: {
            type: 'array',
            contains: {
                type: 'Notification',
            },
        },
    },
} as const;
