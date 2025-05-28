declare module "mqtt-match" {
  function match(filter: string, topic: string, handleSharedSubscription?: boolean): boolean;
  export = match;
}
