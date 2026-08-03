# MongoDB Volume Labs

This Java 21 fixture uses the official MongoDB synchronous driver without starting a server. It validates BSON type preservation and the exact command documents built for filters, conditional updates, indexes, aggregations, keyset pagination, and transaction options.

It cannot validate query plans, storage behavior, replica elections, transaction retries, sharding, or change streams. Those require a target-version replica set or sharded integration environment.

```bash
bash content/volumes/frameworks/FW-07-mongodb/labs/validate_mongodb_labs.sh
```
