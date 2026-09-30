# 5. Chat and Presence: Ordering, Delivery, and Connections

## Learning objectives

By the end of this chapter, you should be able to:

- choose a transport for real-time delivery and size the connection tier;
- define what "ordered" means in chat, and implement it with per-conversation sequence numbers;
- deliver messages exactly once to the user even though the network delivers at least once;
- route a message to a recipient connected to a different server;
- design presence and state its detection latency and load; and
- handle offline users, multiple devices, and group chats.

## Why this matters at SDE-2

"Design WhatsApp" or "design Slack" looks like a CRUD system with a socket. It is actually a distributed ordering and delivery problem attached to a very large number of long-lived connections. Interviewers probe three places: what ordering guarantee you give and how, how a message finds a user connected to another machine, and what the presence feature costs. Each has a concrete answer.

## First-principles model

A chat system is **a durable, per-conversation ordered log, plus a routing layer that pushes new log entries to whichever connections currently belong to the conversation's members.**

- The **log** is the source of truth. A message is "sent" when it is durably appended to its conversation.
- **Delivery** is best-effort pushing over live connections, backed by the log: a client that missed something catches up by reading from its last known position.
- **Ordering** is defined per conversation. There is no meaningful global order across all conversations, and trying to provide one is expensive and pointless.

## Core terminology

- **WebSocket:** a persistent, bidirectional connection over HTTP; the usual transport for chat.
- **Gateway (connection server):** a server that holds client connections and forwards messages.
- **Sequence number:** a per-conversation, monotonically increasing id assigned when a message is appended.
- **At-least-once delivery:** every message arrives, possibly more than once.
- **Idempotent receive:** processing the same message twice has the same effect as once.
- **Presence:** whether a user is currently online, and when they were last seen.
- **Heartbeat:** a periodic message proving a connection is alive.

## Detailed mechanics

### Requirements and connection sizing

In scope: one-to-one and group messages, delivery and read receipts, online presence, history on a new device. Out of scope: voice and video, end-to-end encryption details (mention where keys live), and media processing.

Non-functional: messages must not be lost once acknowledged; within a conversation everyone sees the same order; delivery to online users within a second.

The connection tier is sized by concurrent connections, not requests. The companion's numbers for 10 million concurrent users:

```text
at 500,000 connections per gateway: 20 gateways before headroom
```

State the per-server figure as an assumption - it depends on memory per connection and kernel tuning - and add headroom so that losing a gateway does not overload the rest.

### Ordering with per-conversation sequence numbers

When a message arrives, the service appends it to its conversation's log and assigns the next sequence number for that conversation. Everyone in the conversation then orders by that number. One writer per conversation - route each conversation to one partition, for example by hashing the conversation id onto a partitioned log such as Kafka - makes assignment trivial and removes clock skew from the question. Ordering by client timestamps is the classic mistake: device clocks disagree, so two users can see different orders.

### At-least-once in, exactly-once to the user

Networks deliver at least once: a retried send or a reconnect can repeat a message, and pushes can arrive out of order. The client makes delivery exactly-once *as observed by the user* by delivering in sequence order and ignoring anything already delivered. The companion's receiver buffers early arrivals, drops duplicates, and notices gaps:

```text
arrivals [1, 2, 4, 5, 2, 3, 6] -> delivered [m1, m2, m3, m4, m5, m6], gap requested for [3]
```

Message 4 arrives before 3, so it is buffered and 3 is requested from the log. The duplicate 2 is ignored. When 3 arrives, 3, 4 and 5 are released in order.

Sends need the same protection in the other direction. The client attaches a client-generated message id; if it retries because the acknowledgement was lost, the server recognises the id and returns the original sequence number instead of appending a duplicate.

### Routing across gateways

User A is connected to gateway 3; user B is connected to gateway 17. The message service needs to know where B is:

- A **connection registry** maps user id to the gateways holding that user's connections - one entry per device. Gateways write to it on connect and remove entries on disconnect, with a TTL so that a crashed gateway's entries expire.
- On a new message, the service looks up each recipient's gateways and forwards the message to them, typically through a per-gateway queue or a pub/sub channel.
- If a recipient has no live connection, nothing is pushed. The message is already in the log, and a push notification is sent through the mobile platform's notification service.

### Offline users, multiple devices, history

Each device stores the last sequence number it has seen for each conversation. On reconnect it asks for everything after that position. Multiple devices are just multiple positions. A new device reads recent history from the log. This design makes "offline" the same code path as "missed a push".

### Receipts

Delivery and read receipts are themselves small messages flowing back to the sender, recording the highest sequence number delivered or read per recipient. Store the high-water mark, not one receipt per message: "read up to 482" replaces 482 separate records.

### Group chats

Small groups fan out on write like one-to-one chats. Very large groups or channels - thousands of members - behave like the feed problem in chapter 4: push only to members who are currently connected, and let everyone else pull from the log on open.

### Presence

Presence looks simple and is often the most expensive feature by request count. The companion's numbers for 10 million online users:

```text
10,000,000 online users, heartbeat every 30s: 333,333 heartbeats/s
offline after 3 missed beats: detected 60-90 s after disconnect
```

A third of a million heartbeats per second is more traffic than all the messages in many chat systems. Ways to cut it:

- Use the gateway's knowledge of the socket: a clean disconnect is detected immediately, and heartbeats only need to catch silent drops.
- Keep presence state in the gateway tier and publish only *changes*, not heartbeats.
- Deliver presence only to users who are looking at a relevant conversation or contact list, not to every contact of every user.

State the detection latency explicitly - "offline" appears 60 to 90 seconds after a silent drop with these settings - and let the product decide whether that is acceptable.

## Failure modes and common mistakes

- **Ordering by client timestamp.** Clocks disagree; use a per-conversation sequence number.
- **Global ordering.** Unnecessary and expensive; order per conversation.
- **Treating the push as the source of truth.** A missed push must be recoverable from the log.
- **No client message id.** Retried sends create duplicates.
- **Registry entries without TTL.** A crashed gateway leaves users looking online and unreachable.
- **Presence broadcast to everyone.** The heartbeat rate alone can dominate the system.

## Interview questions and model answers

**Q: How do you guarantee message order?**
A: Per conversation. Each conversation is routed to one partition, which appends messages and assigns increasing sequence numbers; clients order by sequence number. Client timestamps are unreliable, and a global order across conversations is expensive and unnecessary.

**Q: The network can duplicate and reorder messages. What does the user see?**
A: Each message exactly once, in order. The client buffers early arrivals, ignores sequence numbers it has already delivered, and asks the log for any gap. On the send side, a client-generated message id makes retries idempotent.

**Q: How does a message reach a user connected to a different server?**
A: A connection registry maps each user to the gateways holding their connections, with TTL-based entries. The message service looks recipients up and forwards to those gateways. If there is no connection, the message waits in the log and a push notification goes out.

**Q: What does presence cost?**
A: With 10 million online users and a 30-second heartbeat, 333,333 heartbeats per second. I'd rely on socket disconnects for clean exits, publish only presence changes, and send them only to users viewing a relevant screen. Detection of a silent drop takes 60 to 90 seconds with three missed beats; the product should agree to that number.

## Exercises

1. Size the gateway tier for 50 million concurrent users at 200,000 connections per gateway, with enough headroom to lose one availability zone out of three.
2. Design read receipts for a 500-member group. What do you store, and what do you send to the sender?
3. Extend the companion's receiver so that it re-requests a gap only if the gap is still open after a timeout.

## Chapter summary

Chat is a per-conversation ordered log plus a routing layer to live connections. Assign sequence numbers per conversation on a single writer, deliver at least once, and make delivery exactly-once to the user with in-order buffering, duplicate suppression, and gap requests. Route through a TTL-backed connection registry, treat offline as "catch up from the log", and size presence explicitly - it can be the heaviest traffic in the system.

## Revision checklist

- [ ] I can size the gateway tier from concurrent users.
- [ ] I can explain per-conversation sequence numbers and why client timestamps fail.
- [ ] I can walk through the receiver handling [1, 2, 4, 5, 2, 3, 6].
- [ ] I can describe cross-gateway routing and what happens when a gateway crashes.
- [ ] I can state the presence heartbeat load and detection latency.
