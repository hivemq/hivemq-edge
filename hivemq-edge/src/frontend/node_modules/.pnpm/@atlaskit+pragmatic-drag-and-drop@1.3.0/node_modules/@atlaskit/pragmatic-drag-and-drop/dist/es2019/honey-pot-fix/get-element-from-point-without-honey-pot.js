import { isHoneyPotElement } from './is-honey-pot-element';
export function getElementFromPointWithoutHoneypot(client) {
  // eslint-disable-next-line no-restricted-syntax
  const [top, second] = document.elementsFromPoint(client.x, client.y);
  if (!top) {
    return null;
  }
  if (isHoneyPotElement(top)) {
    return second !== null && second !== void 0 ? second : null;
  }
  return top;
}