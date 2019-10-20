### project is under development in deep, deep pre-alpha stage

This project contains:
- jenkins plugin
- standalone http server/mesos framework
- hazelcast data nodes

The goal is to provide a broker between jenkins and cloud providers.
The original issue came from the mesos world where mesos constantly polls jenkins masters (sends offers) which may cause performance issues.
This project has an application which jenkins masters can use to request builder nodes in mesos dynamically.