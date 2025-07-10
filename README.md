# ActiveMQ MCP Server

A [Model Context Protocol](https://www.anthropic.com/news/model-context-protocol) server implementation for ActiveMQ operation.

## Features

* You can manage your ActiveMQ message broker using AI agent. This MCP server wraps JMS Messaging APIs of an ActiveMQ broker as MCP tools. It also uses Jolokia to interact with ActiveMQ on admin level.

* The MCP server communicates with the agent using `stdIO`, and it communicates with ActiveMQ broker using `OpenWire`.


## Tools
* Send message to a topic
* Send message to a queue
* Inspect the status of a topic (user needs to enable Cross-Origin Resource Sharing in Jolokia config)
* Inspect the status of a queue (user needs to enable Cross-Origin Resource Sharing in Jolokia config)

## Installation
Currently the user needs to download the MCP server, compile it and add the jar file into the MCP client configuration of the agent. (e.g., for Amazon Q Developer CLI, edit `~/.aws/amazonq/mcp.json`):


To compile the MCP Server, you need to go to the folder containing `pom.xml` and run `mvn clean install`. And the generated jar file will be located in `target` folder.


You can add the following config in the MCP client configuration.
```angular2html
"mcp-activemq":{
      "command": "java",
      "args":[
          "-jar",
          "PATH-TO-JAR-FILE"
      ]
}
```

## Roadmap
1. More tools for both messaging and admin parts.
2. Upload the MCP server, so that the user don't have to fork and build the MCP server locally.

## Development
### Set up Development Environment
```angular2html
mvn clean install
```
### Running Tests
```angular2html
mvn test
```

## References
https://activemq.apache.org/components/classic/download/classic-05-18-07

https://activemq.apache.org/components/classic/documentation/how-can-i-monitor-activemq-classic

https://github.com/spring-projects/spring-ai-examples

https://docs.aws.amazon.com/amazon-mq/latest/developer-guide/amazon-mq-working-java-example.html



## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.

