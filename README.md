### UNDER CONSTRUCTION

This project contains:
- jenkins plugin
- standalone app

The goal is to provide a broker between jenkins and cloud providers.
The original issue came from the mesos world where mesos constantly polls jenkins masters (sends offers) which may cause performance issues.
This project has an application which jenkins masters can use to request builder nodes in mesos dynamically.