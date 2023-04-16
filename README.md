#  Vitcoin

Bitcoin killer

Simple blockchain implementation on the spring-framework

## Implementation

Nodes communicate with each other via http.
### Model
1. Block model
```kotlin
data class Block(
    val index: Long, // block index
    val previousHash: String, // hash of previous block
    val hash: String, // current block hash
    val data: String, // current block data
    val nonce: Long // nonce
)
```
_hash_ = _sha256_(concatenation of _index_, _previousHash_, _data_ and _nonce_ fields)

A block is valid provided the hash ends with 4 zeros.
2. HttpMessages models

Incoming message about generating newBlock by another node
```kotlin
sealed class HttpIncomingMessage {
    data class NewBlockMessage(val block: Block): HttpIncomingMessage()
}
```
Outgoing messages: BlockValidationError, BlockAcceptedMessage, LastBlockMessage, BlockChainMessage
```kotlin
sealed class HttpOutgoingMessage {
    /**
     * block is invalid
     */
    data class BlockValidationError(val message: String, val block: Block): HttpOutgoingMessage()

    /**
     * block was accepted by node
     */
    data class BlockAcceptedMessage(val block: Block): HttpOutgoingMessage()

    /**
     * get last block in chain
     */
    data class LastBlockMessage(val block: Block) : HttpIncomingMessage()

    /**
     * get all blockchain
     */
    data class BlockChainMessage(val blocks: List<Block>) : HttpIncomingMessage()
}
```

### API
1. To send a new block, use the `/newBlock` POST request:
```json
{
  "block": {
    "index": 40,
    "previousHash": "016d74c33fd2204f6fc18b170ab7d1009560e6204432e335385c43b57e890000",
    "hash": "95a12cfce7e3599fc33e910ce3694c534802b4b1add7e1232b4d847905630000",
    "data": "OYHcMGVHtElBN8c6ieEkuH63kMlL2oRj2tLG5NCh86y3IGmIctK04z4AhkzOUZLoPJC04mIpFEsUBt4",
    "nonce": 1012199802390308277
  }
}
```
2. To get a new block, use the `/lastBlock` GET request. Response:
```json
{
  "block": {
    "index": 1,
    "previousHash": "",
    "hash": "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
    "data": "p9PmGmjLj5N0qxaP4MW6yHBsGQxODwhcQdaDFV4CIkOSi3UU0fn6YK4R",
    "nonce": 8344120720967856628
  }
}
```
3. To get a blockchain, use the `/blockChain` GET request. Response:
```json
{
  "blocks": [
    {
      "index": 1,
      "previousHash": "",
      "hash": "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
      "data": "p9PmGmjLj5N0qxaP4MW6yHBsGQxODwhcQdaDFV4CIkOSi3UU0fn6YK4R",
      "nonce": 8344120720967856628
    },
    {
      "index": 2,
      "previousHash": "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
      "hash": "6e176cdeabbcaf2ef2a5c9013e5242180bb790b3b556c9b2b0d13daafe0f0000",
      "data": "p4nLxoVZPrj3ZFQv2W8G3LaSHk3uUFOZWi0RmeN9RnnhB3vmy0cD25CqL8CSThagAX",
      "nonce": -8988250733424672812
    }
  ]
}
```
4. To start or stop use `/start` and `/stop` GET requests respectively
5.  To generate genesis block use `/generateGenesys` GET request
### Generation strategy

Each node can use its own approach to calculate the nonce -
sequentially increase by 1, increase by Fibonacci, use a random value.

To configure generation strategy pass `blockchain.generationStrategyName` into application.yml file:
```yaml
blockchain:
  generationStrategyName: RANDOM # or INCREMENT or FIBONACCI
```
## Installation

### Build necessary docker images via install.sh script:

```shell
./install.sh
```
It builds 2 docker images:
1. vitekkor/vitcoin:{latestVersion} - blockchain node
2. vitekkor/vitcoin-start-script:{latestVersion} - script to start mining blocks

### Up docker-compose
```shell
docker-compose up
```
### Stop nodes
To stop nodes run stopNodes.sh script:
```shell
./stopNodes.sh localhost:8080 localhost:8081 localhost:8082
```
**NB:** 8080, 8081 and 8082 are external docker containers ports

### Nodes scaling

## Configure docker-compose
1. Add one more node to services:
```yaml
# file: docker-compose.yml
  node4:
    image: vitekkor/vitcoin:1.0.0
    volumes:
      - ./application-node_4.yml:/etc/vitekkor/vitcoin/application.yml
    restart: always
    ports:
      - "8083:8080"
```
2. Provide application-node_4.yml configuration
```yaml
# file: application-node_4.yml
blockchain:
  generationStrategyName: # RANDOM or INCREMENT or FIBONACCI
  nodes: # list of blockchain nodes
    - uri: http://node1:8080
    - uri: http://node2:8080
    - uri: http://node3:8080
```
3. Add new node to the another nodes configuration

E.g. application-node_1.yml:
```yaml
# file: application-node_1.yml
blockchain:
  generationStrategyName: RANDOM
  nodes:
    - uri: http://node2:8080
    - uri: http://node3:8080
    - uri: http://node4:8080
```
4. Add new node arg to startScript
```yaml
# file: docker-compose.yml
  start-script:
    image: vitekkor/vitcoin-start-script:1.0.0
    restart: no
    environment:
      NODES: "node1:8080 node2:8080 node3:8080 node4:8080"
    depends_on:
      - node1
      - node2
      - node3
      - node4
```

