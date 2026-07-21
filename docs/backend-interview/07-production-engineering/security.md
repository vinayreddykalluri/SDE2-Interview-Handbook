# Backend Security and Abuse Resistance

## Threat-model sequence

1. Identify assets, actors, entry points, trust boundaries, and data classifications.
2. Enumerate spoofing, tampering, repudiation, disclosure, denial of service, and privilege escalation.
3. Prioritize by likelihood and impact.
4. Assign preventive, detective, and recovery controls.
5. Record residual risk and an owner.

## Backend checklist

- Authentication strength, token validation, expiry, rotation, and revocation.
- Object/action authorization; deny by default and prevent tenant crossing.
- Input validation, output encoding, parameterized queries, and safe deserialization.
- TLS, encryption at rest, managed keys, and secret rotation.
- Rate limits, quotas, abuse signals, payload limits, and isolation.
- Minimal sensitive logging, audit trails, retention, deletion, and privacy.
- Dependency/image scanning, patching, provenance, and least-privilege identity.

## Drills

Threat-model a file upload, password reset, webhook receiver, admin endpoint, and multi-tenant search API. Include replay, confused deputy, SSRF, injection, exfiltration, and denial of service.
