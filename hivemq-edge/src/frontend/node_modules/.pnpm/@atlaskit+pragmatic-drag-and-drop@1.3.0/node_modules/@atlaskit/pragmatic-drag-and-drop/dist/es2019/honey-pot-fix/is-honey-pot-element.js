import { honeyPotDataAttribute } from './honey-pot-data-attribute';
export function isHoneyPotElement(target) {
  return target instanceof Element && target.hasAttribute(honeyPotDataAttribute);
}