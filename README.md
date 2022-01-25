# Sharded Transaction Manager

### General Architecture
* System is composed of 6 docker containers devided to 2 shards (3 containers per shard)
* Atomic broadcast grpc service using paxos for leader - follower replication
* Transaction grpc service for submitting transactions, transfers, utxos per account ...
* Rest API using spring boot framework exposed to clients
* Shards manager which keeps track of available nodes in the system and elects available leader per shard
* Zookeeper service for syncronization puposes (membership, locks) and transactions log handeling leader crash

### Main Features
* System recognizes participating nodes with Zookeeper membership algorithem and adjusts when a node fails during run-time
* Client address space devided to 2 shards, each shard has 3 replicated nodes to support leader failure
* State replication maintained in shard with leader atomic broadcasts list of operations made to the replicas
* All nodes in system accept client http requests and route the request to the appropiate leader responsible for client's account shard
* Supports transaction list atomic submission
* Each transaction is added to log initially and removed from log on completion (that way if leader fails mid-way the newley elected leader can continue him according to log)
* Each node has a dedicated thread which polls log and re-transmits operation that has timedout (then we assume operation didn't happen because of node failure)
* Utxos double spending is prevented using locking Zookeeper mechanism
* Total ordering of transaction ledger is maintained with global clock (implemented with zookeeper sequntial Znode)
* Atomic broadcast is preformed on batches of 5 operations (or timeout, whatever comes first)

### General flow
1. Node recieves client request
2. Node gets leader ip responsible for client's account using Shards service
3. Node routes request to that shard leader using Transaction grpc service
4. Upon leader recieving the request it adds operation to be preformed to log 
5. leader atomic broadcasts operation to all shard replicas
6. Each node replica adjusts local data stractures to maintain sync with leader
7. If leader fails mid-way the dedicated log thread in the newley elected leader will continue polling log instead of previous leader
8. For transactions, each transfer in output list will be routed to the appropriate leader to manage the transfer (and repeate steps 3-5 in this shard)
9. Upon compleation remove operation from log to let the source shard leader the operation was completed successfully 
