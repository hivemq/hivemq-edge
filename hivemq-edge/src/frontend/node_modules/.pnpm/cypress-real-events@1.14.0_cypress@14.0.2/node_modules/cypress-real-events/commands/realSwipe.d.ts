import { Position } from "../getCypressElementCoordinates";
export type SwipeDirection = "toLeft" | "toTop" | "toRight" | "toBottom";
export interface RealSwipeOptions {
    /**
     * The point of the element where touch event will be executed
     * @example cy.realSwipe({ position: "topLeft" })
     */
    touchPosition?: Position;
    /** X coordinate to click, relative to the Element. Overrides `position`.
     * @example
     * cy.get("canvas").realSwipe({ x: 100, y: 115 })
     * cy.get("body").realSwipe({ x: 11, y: 12 }) // global touch by coordinates
     */
    x?: number;
    /**  X coordinate to click, relative to the Element. Overrides `position`.
     * @example
     * cy.get("canvas").realSwipe({ x: 100, y: 115 })
     * cy.get("body").realSwipe({ x: 11, y: 12 }) // global touch by coordinates
     */
    y?: number;
    /** Length of swipe (in pixels)
     * @default 10
     * @example
     * cy.get(".drawer").realSwipe("toLeft", { length: 50 })
     */
    length?: number;
    /**
     * Swipe step (how often new touch move will be generated).
     * Must be less than or equal options.length
     * @default 10
     * cy.get(".drawer").realSwipe("toLeft", { step: 5 })
     */
    step?: number;
    /**
     * Delay between touchStart and touchMove events (ms)
     * @default 0
     * cy.get(".drawer").realSwipe("toLeft", { touchMoveDelay: 300 })
     */
    touchMoveDelay?: number;
}
export declare function realSwipe(subject: JQuery, direction: SwipeDirection, options?: RealSwipeOptions): Promise<JQuery<HTMLElement>>;
