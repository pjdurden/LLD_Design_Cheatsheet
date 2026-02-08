Implement Redis/Memcached

1. Functional Requirements
get - fetch by key with low latency
put - inset / update key-value pair
Delete - remove key explicitly
TTL support - keys expire automatically
Eviction policy - evict entries when memory full

out of scope - complex queries , joins or full text search

2. Non functional Requirements
Latency - sub ms to single digit ms read.
CAP - AP>CP
Scalibility - Horizontally scalable
Consistancy - eventual acceptable
memory efficient - optimal eviction and memory usage

3. Entities
Key 
Value
Metadata - TTL/Expiry time , Last accessed time (for eviction) , Size

4. API Design
GET /cache/{key}
Put /cache/{key}
Delete /cache{key}

headers - X-TTL (Time to live in seconds)
Auth handled at API gateway

5. HLD


