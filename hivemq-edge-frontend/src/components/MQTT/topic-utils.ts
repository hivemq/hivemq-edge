const TOPIC_SEPARATOR = ' / '

export const formatTopicString = (topic: string) => topic.split('/').join(TOPIC_SEPARATOR)
