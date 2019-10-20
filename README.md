## project is under development in deep, deep pre-alpha stage
#### Build status: [![CircleCI](https://circleci.com/gh/angry-cellophane/menkins/tree/master.svg?style=svg)](https://circleci.com/gh/angry-cellophane/menkins/tree/master)

This project contains:
- jenkins plugin
- standalone http server/mesos framework
- hazelcast data nodes

The goal is to provide a broker between jenkins and cloud providers.
The original issue came from the mesos world where mesos constantly polls jenkins masters (sends offers) which may cause performance issues.
This project has an application which jenkins masters can use to request builder nodes in mesos dynamically.