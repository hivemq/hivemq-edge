import { Position, ScrollBehaviorOptions } from "../getCypressElementCoordinates";
export interface RealMouseMoveOptions {
    /**
     * Initial position for movement
     * @default "topLeft"
     * @example cy.realMouseMove({ position: "topRight" })
     */
    position?: Position;
    /**
     * Controls how the page is scrolled to bring the subject into view, if needed.
     * @example cy.realMouseMove({ scrollBehavior: "top" });
     */
    scrollBehavior?: ScrollBehaviorOptions;
    /**
     * Indicates whether any modifier (shiftKey | altKey | ctrlKey | metaKey) was pressed or not when an event occurred
     * @example cy.realMouseDown({ shiftKey: true });
     */
    shiftKey?: boolean;
    altKey?: boolean;
    ctrlKey?: boolean;
    metaKey?: boolean;
}
/** @ignore this, update documentation for this function at index.d.ts */
export declare function realMouseMove(subject: JQuery, x: number, y: number, options?: RealMouseMoveOptions): Promise<JQuery<HTMLElement>>;
