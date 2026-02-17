# HiveMQ Technical Documentation Writing Guidelines

\*Version 1.0 | Last Updated: January 2026\*

# Purpose

Ensure HiveMQ documentation is clear, accurate, and consistent, and enables users to deploy, operate, and integrate the HiveMQ Platform effectively.

## Goals

\- Increase consistency & clarity  
\- Decrease ambiguity  
\- Facilitate translation  
\- Streamline content

## Writing Principles

1\. Be Clear  
\- Use simple, direct language  
\- One main idea per paragraph  
\- Explain concepts before details  
\- Avoid ambiguity

2\. Be Helpful  
\- Focus on what the user wants to accomplish  
\- Answer "why", "how," as well as "what".  
\- Assume users have limited time

3\. Be Accurate  
\- Verify technical behavior  
\- Match product terminology  
\- Keep examples current  
\- Avoid speculation

4\. Be Consistent  
\- Use the same terms throughout  
\- Follow established patterns  
\- Maintain a uniform structure  
\- Keep formatting consistent

## Voice & Tone

### Active Voice

Use active voice (“A” does “B”). Avoid passive sentence constructions (“B” is done by “A”). Passive voice sentences require more words and make the reader work harder to understand the intended meaning. 

| **Recommended** | **Avoid** |  
|---------------|----------|  
| The broker disconnects the client. | The client is disconnected by the broker. |  
| HiveMQ fully supports all MQTT versions and features. | All versions of MQTT are supported by HiveMQ fully. |  
| Adjust the value in your configuration file. | The value is changed by adjusting the config. |

### Tone Guidelines

Be helpful, friendly, and supportive. Achieve a natural, knowledgeable, and approachable voice. Remain non-judgmental and use positive statements. Avoid overly formal constructions. Use a familiar tone, but do not try to be humorous.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| If you experience an error, check your settings and try again. | If you get an error, it means your settings are wrong. |  
| You need the correct permission to complete this action. | You don't have the right permission. |  
| Enter a valid user ID. | Ooops\! We've never seen that number before\! |  
| This API lets you collect data about what your users like. | The API documentation presented by this page may enable the acquisition of information pertaining to user preferences. |

### Sentence Length

Write short, clear sentences. Concise sentences are easier to understand and translate.

\- 25 words or fewer for descriptions.  
\- 20 words or fewer for procedures.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| When you run HiveMQ on your local machine with the default control center configuration enabled, your control center appears on http://localhost:8080/. The default login for the HiveMQ control center is username: admin, password: hivemq. (Two sentences, 24 \+ 12 words)   
| If you are running a HiveMQ instance on your local machine and the default configuration of the HiveMQ control center is enabled, you can navigate to the http://localhost:8080/ URL and enter the default HiveMQ control center login (username admin and password hivemq) to access your control center. Too Long (47 words) |  
| At the top of the dashboard, an overview bar shows you key metrics such as the current usage and throughput of the cluster. To view detailed information for a specific metric, click the metric in the overview bar.  (Two sentences, 18 \+ 15 words) |  
|The overview bar (located at the top of the dashboard) shows an overview of key metrics with the ability to drill down deep each one (just click on a specific metric to see the details). Too Long (35 words) |

## Verbs & Tenses

### Use Simple Verb Tenses

Documentation helps users perform tasks and gather information. Readers typically want to complete a task or apply the information immediately. Since these activities take place in the present for the reader, documentation that is written in the present tense is appropriate in most cases. Additionally, the present tense is easier to read than the past or future tense.

**Approved verb forms**:  
\- Simple present: Opens, runs, is  
\- Imperative: Open xyz, Run xyz  
\- Infinitive: To run, to retain  
\- Simple past: Finished, monitored

### Avoid Modal Verbs

**Do not use vague modal verbs**:

- Should, could, would  
- May, might   
- Will

### Avoid "-ing" Forms

Do not use the "-ing" form of verbs unless the verb is part of a technical name.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| To navigate through the table, open and close the associated elements. | You may navigate through the table by opening and closing the associated elements. |  
| The server sends an acknowledgment. | The server will then send an acknowledgment. |  
| The broker disconnects the client. | The client would then be disconnected by the broker. |  
| Because action menus are context-sensitive, the actions that are available in each action menu can vary. | The available menu items may vary because actions are context-sensitive and will affect it. |  
| If you want HiveMQ to log these entries, set your log level to DEBUG. | Should you want these entries to be logged, consider switching to the debugging log level. |  
| If the list in this output is empty, the extension drops the message and does not write a Kafka record. | Setting an empty list in this output will result in the MQTT message being dropped and no Kafka record being written. |  
| To view further actions, click the menu icon. | To view further actions, click on the menu icon. |

## Click vs. Select

Use clear and consistent verbs to describe user interaction with the HiveMQ user interface.

### Click

Use “**click**” when the user activates an element that performs an action.  These elements include:

- Buttons  
- Links  
- Icons

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Click Save. | Click on the Save button |  
| To continue, click Next. | To continue, click on Next. |  
| For more information, click View Details. | For more information, click on View Details. |

**Note**: Use “‘click” as a transitive verb that acts as an action verb with a direct object. No preposition is required after the verb. Do not use “click on” or “click at”.

### Select

Use “**select**” when the user chooses a value or state from a set of options. These options include:

- Dropdown menus  
- Radio buttons  
- Check boxes  
- List item

| **Recommended** | **Avoid** |  
|---------------|----------|

| Select a region from the dropdown menu. | Choose a region. |  
| Select MQTT 5 from the Protocol list. | Click on MQTT 5 as the Protocol. |

## Grammar & Punctuation

### Indefinite Articles (a, an)

The key is the **pronunciation**, not the spelling:

- Use '**a**' before a word that begins with a consonant sound   
- Use  '**an**' before a word that starts with a vowel sound. 

The choice is based on the sound of the first letter that is pronounced in the word the article modifies. 

| **Recommended** | **Avoid** |  
|---------------|----------|  
| an MQTT broker (pronounced "em-queue") | a MQTT broker |  
| a user (pronounced "yoo-zer") | an user |  
| a HiveMQ instance (pronounced "hive") | an HiveMQ instance |  
| an hour (silent 'h') | a hour |  
| an SQL database (pronounced "ess-queue-ell") | a SQL database |  
| an HTML file (pronounced "aitch-tee-em-ell") | a HTML file |

**Tip**: Check pronunciation at \[Merriam-Webster Dictionary\](https://www.merriam-webster.com/) if unsure.

### Commas

Use a comma after an introductory phrase. 

In a series of three items, place a comma immediately before the conjunction. 

For a series of more than three items, use an unordered list.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| From the dashboard, you can access important information about your system. | From the dashboard you can access important information about your system. |  
| For example, if your cluster runs with over 60% CPU usage. | For example if your cluster runs with over 60% CPU usage. |  
| For more information, see... | For more information read the... |  
| A minimal HiveMQ extension consists of a Java class, a text file, and an XML file. | A minimal HiveMQ extension consists of a Java class, a text file and an XML file. |

### Hyphens

1\. Hyphenate two or more words that precede and modify a noun in the manner of an adjective. Do not hyphenate words that end in \-ly or acronyms that modify a noun.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Drop-down list | Drop down list |  
| Pop-up window | Popup window |  
| High-performance MQTT broker | High performance MQTT broker |  
| Immediately responsive interface | Immediately-responsive interface |  
| HTTPS listener | HTTPS-listener |  
| Mission-critical deployment | Mission critical deployment |

2\. Hyphenate two or more words that precede and modify a noun as a unit if the meaning would otherwise be ambiguous.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Read-only memory | Read only memory |  
| Built-in drive | Built in drive |

3\. Hyphenate when one of the words is a verb form that ends in \-ed or \-ing the combination is used as an adjective or noun.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Well-defined schema | Well defined schema |  
| Left-aligned text | Left aligned text |  
| Free-flowing conversation | Free flowing conversation |

4\. Hyphenate when the modifier is a number, single letter, or abbreviation.

**Exception**: Email does not require a hyphen.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| E-commerce | Ecommerce |  
| Two-way communication | Two way communication |  
| Three-node cluster | Three node cluster |  
| 5-pointed star | 5 pointed star |

5\. Hyphenate when the stem word begins with a capital letter.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Un-American | UnAmerican |  
| Non-XML | non XML |

**Tip**: Check the \[Merriam-Webster Dictionary\](https://www.merriam-webster.com/) if unsure.

## Lists

### Unordered Lists

An **unordered list** is a collection of items with no required sequence, where each item is typically marked with bullet points such as circles, squares, or discs.

**Unordered list usage**:  
\- Series of more than three items (required)  
\- Two or three items (optional)

**Guidelines**:

- Capitalize the first word of each item in the list.    
- If the items in the list are sentences, end each line with a period.  
- If the items in the list are objects, you do not need a period at the end of each line.  
- Use a colon (:) to introduce an unordered list.  
- Put a colon at the end of a sentence that introduces a list.  
- Do not use sentence fragments to introduce a list.

**Note**: To ensure coherence, always use parallel construction in your list. For example, use the same part of speech and check that all phrases have the same logical structure.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| The Logback library defines five log levels:  
\- TRACE  
\- DEBUG  
\- INFO  
\- WARN  
\- ERROR | Logback defines trace, debug, info, warn and error log levels. |  
| Message expiry can occur for several reasons:  
\- A client takes too long to consume the message.  
\- The client remains offline and the message expires before the client can consume it.  
\- A retained message that is stored on the broker expires. |  
| You can deploy your customization in three steps: | Deploying the customization is as simple and easy as: |  
| The following fields are available: | The fields are: |  
| The reasons for message expiry are:  
\- It takes too long to consume the message  
\- The client remains offline and the message expires before the client can consume it  
\- A retained message that is stored on the broker expires |

### Ordered Lists

An **ordered list** presents items in a specific sequence, where each item is numbered or lettered to show progression or priority.

**Ordered list usage**:  
\- Step-by-step procedures  
\- Sequential processes  
\- Prioritized items

## Terminology & Consistency

### Acronyms

Define acronyms and abbreviations on first usage. Define again if used infrequently in long sections.

**Note**: MQTT is considered an initialism and does not require definition on first usage.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| The HiveMQ Enterprise Security Extension (ESE) expands the role, user, and permission-management capabilities of HiveMQ Enterprise and Professional editions. | ESE expands the role, user, and permission-management capabilities... (if first use) |  
| Kubernetes (K8s) is a widely used open-source platform for automating the deployment, scaling, and management of containerized applications. | K8s is a widely-used open-source platform... (if first use) |  
| Transport Layer Security (TLS) is a cryptographic protocol that allows secure and encrypted communication between a client application and a server at the transport layer. | TLS is a cryptographic protocol... (if first use) |

### Noun Clusters

**Do not place more than three nouns in a row.**

| **Recommended** | **Avoid** |  
|---------------|----------|  
| When you add a WebSocket listener, you must configure the properties of the websocket-listener appropriately. | Configuring the bi-directional WebSocket network communication protocol listener configuration properties file section entries is then necessary. |

### Controlled Language

**Use identical sentences for identical content.**

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Enter a name in the Name field. Enter a date in the Date field. | Enter a name in the "Name" input field and use the "Date" option for setting a date. |  
| Select a file from the Downloads menu. | Browse for the file of your choice in the drop-down menu called downloads located at the left side of the screen. |  
| In the File menu, select New Keystore. In the popup window that opens, select the JKS key store type. | Choose File → New Keystore from the menu and select JKS as key store type in the popup window. |

### Consistent Terminology

**Do not use the same word to mean different things**. Whenever possible, use words for a specific part of speech. For example, avoid using the same word as both a noun and a verb in the same text. Decide on a specific meaning for each word.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Many HiveMQ customers currently use the HiveMQ MQTT Client. | Many HiveMQ clients currently use the HiveMQ MQTT Client. |  
| In the terminal window of the second MQTT client, enter “sub \-t testTopic \-s”. In the terminal window of the first MQTT client, enter pub \-t testTopic \-m Hello. | In the terminal window of the second MQTT client, enter “sub \-t testTopic \-s”. For the first MQTT client, type pub \-t testTopic \-m Hello on your command line. |  
| Expand the side navigation. | Expand the main navigation. Expand the left navigation. |  
| View client details. View connection status. View license information. | Access client details. See client details. Open license information. |  
| For more information, see Configuration Options. | A more thorough explanation of features and configuration instructions can be found here. |

## Pronouns

### Personal Pronouns

**Do not use gender-specific pronouns.**

**Strategies to avoid gender-specific pronouns**:  
\- Use imperatives (Do this)  
\- Use plural nouns and pronouns  
\- Use second person (you) instead of third person  
\- Replace pronoun with noun or indefinite article

| **Recommended** | **Avoid** |  
|---------------|----------|  
| To log in, enter your user name and password. | To log in, users must enter their usernames and passwords. To log in, the user should enter his or her user name and password. |  
| Technical documentation exists for the reader. You are writing for the reader, not for yourself. | Technical documentation exists for the reader. You are writing for him, not for yourself. |

### Pronouns (this, these, it)

**Limit the use of "this", "these", and "it".** Overuse creates ambiguity and increases reader confusion. If a pronoun can refer to more than one noun in a text, replace the pronoun with a noun.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| WebSocket is a network protocol that provides bi-directional communication between a browser and a web server. | This is providing bi-directional communication between browsers and web servers. |  
| Although HiveMQ can also run on embedded devices, server hardware unleashes the full potential of the broker. | You can also run on embedded devices, although server hardware unleashes its full potential. |  
| Incorrect character encoding can create problems in the import file. | It can create problems in the import file. |

### Optional Pronouns

Use the optional pronouns "that" and "who"  to avoid ambiguity.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Use the configuration that you created. | Use your created configuration. |  
| Right-click the link that you want to open. | Right-click on the link you want to open. |

### Relative Pronouns

\- "**Which**" introduces a non-restrictive clause and is preceded by a comma.  
\- "**That**" introduces a restrictive clause and is not preceded by a comma.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Workflows are a universal tool that is available for all asset types. | They are a universal tool available for all asset types. |  
| The version number of the HiveMQ version that you are currently running appears below the side navigation. | The version number of the HiveMQ version, which you are currently running, appears below the side navigation. |  
| Groups consist of users who share similar duties. | Groups consist of users that share similar duties. |

### Conditional Clauses

**Place the condition first.** Separate the "if clause" from the main clause with a comma. Use the simple present in both clauses.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| If you run HiveMQ in a cluster, install the extension on each node. | Install the extension on each node if running HiveMQ in a cluster. |  
| To remove the cluster, click Delete. | Click on the Delete button if you want to remove the cluster. |

## Courtesy & Style

### Courtesy

Be polite, but do not use "please" in instructions. 

| **Recommended** | **Avoid** |  
|---------------|----------|  
| For more information, see... | For more information on XYZ, please see... |  
| To view the document, click View. | To view the document, please click on "View". |

### Plurals

**Do not put optional plurals in parentheses.** Use either plural or singular consistently.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| Enter the names of the topics. | Enter the name(s) of the topic(s). |  
| View the subscriptions that match your search criteria in the table. | View the subscription(s) that match your search criteria in the table. |  
| Add one or more nodes to the cluster. | Add nodes(s) to the cluster. |

### Versus & e.g.

Spell out "versus" in longer content. The abbreviation "vs." is permitted in short expressions. The best practice is to use “for example” instead of “e.g”. Plain language improves clarity, readability, and translation quality.

| **Recommended** | **Avoid** |  
|---------------|----------|  
| MQTT vs. HTTP for IoT | MQTT Vs. HTTP for IoT |  
| This article explores the benefits of MQTT versus AMQP for your IoT applications. | This article explores the benefits of MQTT vs AMQP for your IoT applications. |  
| For example, configure the listener with TLS. |   
|​​ Configure the listener with TLS, e.g., port 8883\. |

**Note**: Since August 2024, the HiveMQ website follows the AP Style guide.

## Code Examples

### Code Block Guidelines

Always include the following information:  
\- Syntax highlighting language  
\- A brief description before code  
\- Comments for complex sections

Example:

Configure MQTT listener on port 1883:  
\`\`\`xml  
\<hivemq\>  
    \<listeners\>  
        \<tcp-listener\>  
            \<port\>1883\</port\>  
            \<bind-address\>0.0.0.0\</bind-address\>  
        \</tcp-listener\>  
    \</listeners\>  
\</hivemq\>  
\`\`\`

### Placeholder Conventions

Use clear, descriptive placeholders:

Example:  

\`\`\`bash  
\<hivemq-install-directory\>/bin/run.sh  
http://\<your-server-ip\>:8080  
\<username\>@\<hostname\>  
\`\`\`

### Command Examples

Show the command and the expected output:

Example command: 

\`\`\`bash  
\# Check HiveMQ status  
systemctl status hivemq

Example expected output:  
● hivemq.service \- HiveMQ MQTT Broker  
   Loaded: loaded  
   Active: active (running)  
\`\`\`

##  Visual Elements

When to Use Tables  
\- Comparing options  
\- Reference information  
\- Configuration parameters

Example:

| Parameter | Default | Description |  
|-----------|---------|-------------|  
| max-connections | \-1 | Maximum client connections (-1 \= unlimited) |  
| port | 1883 | MQTT listener port |

When to Use Lists

Unordered lists (bullets):  
\- Related items without sequence  
\- Features or capabilities  
\- Multiple examples

Ordered lists (numbered):  
\- Step-by-step procedures  
\- Sequential processes  
\- Prioritized items

Diagrams & Screenshots

Guidelines:  
\- Add descriptive captions  
\- Use callouts sparingly  
\- Keep images up-to-date  
\- Provide alt text for accessibility

## Accessibility & Localization

### Write for Global Audiences

 Avoid:   
\- Idioms: "piece of cake"  
\- Cultural references: "home run"  
\- Humor that doesn't translate  
\- Regional date formats: 1/5/2026

 Use:   
\- Simple, direct language  
\- Universal examples  
\- ISO date formats: 2026-01-05  
\- Clear, literal meanings

### Inclusive Language

Use gender-neutral terms:  
\- "they" instead of "he/she"  
\- "user" instead of "guys"  
\- "developer" instead of gendered pronouns

### Link Text

Use descriptive link text:

| **Recommended** | **Avoid** |  
|---------------|----------|

| See the \[Installation Guide\](link) |  
 | \[This page\](link) has details |  
|For more information, see \[MQTT protocol\](link) |  
|Click \[here\](link) for more information |

## Quality Checklist

### Review Before Publishing

\- \[ \]  Accuracy:  All information verified and tested  
\- \[ \]  Sentence length:  25 words or fewer (descriptions), 20 or fewer (procedures)  
\- \[ \]  Active voice:  No passive constructions  
\- \[ \]  Verb tenses:  Simple present, imperative, infinitive only  
\- \[ \]  Modal verbs:  No should, could, would, may, might, will  
\- \[ \]  Pronouns:  No gender-specific, minimal use of "this/it"  
\- \[ \]  Articles:  Correct use of "a" or "an" based on pronunciation  
\- \[ \]  Commas:  Serial commas, and after introductory phrases  
\- \[ \]  Hyphens:  Correct compound adjectives  
\- \[ \]  Lists:  Parallel construction, proper capitalization  
\- \[ \]  Terminology:  Consistent throughout  
\- \[ \]  Acronyms:  Defined on first use  
\- \[ \]  Code:  All examples tested and current  
\- \[ \]  Links:  All links valid and relevant  
\- \[ \]  Formatting:  Proper markdown/formatting applied  
\- \[ \]  No "please":  Removed from instructions  
\- \[ \]  Click:  Used correctly without "on" or "at"

### Self-Review Questions

1\. Are all sentences 25 words or fewer?  
2\. Is everything in the active voice?  
3\. Have I avoided modal verbs (should, could, would)?  
4\. Are pronouns gender-neutral and unambiguous?  
5\. Is terminology consistent?  
6\. Would this translate well to other languages?  
7\. Can users scan and find what they need quickly?

## Quick Reference

### Words to Avoid

| Avoid | Use Instead |  
|-------|-------------|  
| should, could, would | Use imperative or simple present |  
| may, might, will | Use simple present |  
| please | (Remove from instructions) |  
| click on | click |  
| \-ing verbs | Simple present or imperative |  
| this, it (ambiguous) | Specific noun |  
| he/she, his/her | you, they, or specific noun |

### Common Patterns

| Pattern | Example |  
|---------|---------|  
| Conditional | If you run HiveMQ in a cluster, install the extension on each node. |  
| Imperative | Click Save to apply changes. |  
| List introduction | The broker supports three authentication methods: |  
| Active voice | The broker disconnects the client. |

## Resources

### Style Guides

\- AP Style Guide (HiveMQ website standard since August 2024\)  
\- Microsoft Writing Style Guide  
\- Google Developer Documentation Style Guide

\*These guidelines are a work in progress. Suggest improvements anytime.\*  
