# Redis Volume Labs

The Java 21 lab implements a deliberately small RESP2 command encoder plus deterministic models for key TTL, `NX` lease acquisition, token-safe release, sliding-window limits, and fencing tokens. These are executable interview mechanics without requiring a native Redis process or Docker.

The lab does not claim to reproduce Redis server scheduling, memory encodings, eviction, persistence, replication, Sentinel, Cluster, Lua, or client behavior. Validate those against the exact Redis and Java-client versions.

```bash
bash content/volumes/frameworks/FW-08-redis/labs/validate_redis_labs.sh
```
