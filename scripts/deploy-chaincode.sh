#!/bin/bash
# Deploy carbon-chaincode to Fabric network
# Prerequisites: Fabric network running (docker-compose.fabric.yml up)
# Crypto materials generated (./scripts/generate-fabric-crypto.sh)
#
# Usage: ./scripts/deploy-chaincode.sh [VERSION] [SEQUENCE]
# Default VERSION: 1.0, SEQUENCE: 1

set -e

CC_NAME="carbon-chaincode"
CC_VERSION="${1:-1.0}"
CC_SEQUENCE="${2:-1}"
CC_PATH="/opt/gopath/src/github.com/oaiss/chain/chaincode"
CHANNEL_NAME="mychannel"
ORDERER_ADDRESS="orderer.example.com:7050"
ORDERER_TLS_CA="/var/hyperledger/orderer/tls/ca.crt"
PEER_ADDRESS="peer0.org1.example.com:7051"
PEER_TLS_CA="/var/hyperledger/peer/tls/ca.crt"
CLI_CONTAINER="fabric-cli"

echo "=== Deploying ${CC_NAME} v${CC_VERSION} (sequence: ${CC_SEQUENCE}) ==="

# Step 1: Package chaincode
echo "1. Packaging chaincode..."
docker exec -e CORE_PEER_LOCALMSPID=Org1MSP \
  -e CORE_PEER_MSPCONFIGPATH=/var/hyperledger/peer/msp \
  -e CORE_PEER_ADDRESS=${PEER_ADDRESS} \
  -e CORE_PEER_TLS_ROOTCERT_FILE=${PEER_TLS_CA} \
  ${CLI_CONTAINER} \
  peer lifecycle chaincode package ${CC_NAME}.tar.gz \
  --path ${CC_PATH} \
  --lang golang \
  --label ${CC_NAME}_${CC_VERSION}

# Step 2: Install chaincode on peer
echo "2. Installing chaincode on peer0..."
docker exec -e CORE_PEER_LOCALMSPID=Org1MSP \
  -e CORE_PEER_MSPCONFIGPATH=/var/hyperledger/peer/msp \
  -e CORE_PEER_ADDRESS=${PEER_ADDRESS} \
  -e CORE_PEER_TLS_ROOTCERT_FILE=${PEER_TLS_CA} \
  ${CLI_CONTAINER} \
  peer lifecycle chaincode install ${CC_NAME}.tar.gz

# Step 3: Get package ID
echo "3. Querying installed chaincode..."
docker exec -e CORE_PEER_LOCALMSPID=Org1MSP \
  -e CORE_PEER_MSPCONFIGPATH=/var/hyperledger/peer/msp \
  -e CORE_PEER_ADDRESS=${PEER_ADDRESS} \
  -e CORE_PEER_TLS_ROOTCERT_FILE=${PEER_TLS_CA} \
  ${CLI_CONTAINER} \
  peer lifecycle chaincode queryinstalled >&log.txt

PACKAGE_ID=$(grep -o "${CC_NAME}_${CC_VERSION}:[a-f0-9]*" log.txt | head -1 | awk -F':' '{print $2}')
echo "Package ID: ${PACKAGE_ID}"

if [ -z "${PACKAGE_ID}" ]; then
  echo "ERROR: Could not find package ID. Check queryinstalled output."
  exit 1
fi

# Step 4: Approve chaincode for org
echo "4. Approving chaincode for Org1..."
docker exec -e CORE_PEER_LOCALMSPID=Org1MSP \
  -e CORE_PEER_MSPCONFIGPATH=/var/hyperledger/peer/msp \
  -e CORE_PEER_ADDRESS=${PEER_ADDRESS} \
  -e CORE_PEER_TLS_ROOTCERT_FILE=${PEER_TLS_CA} \
  ${CLI_CONTAINER} \
  peer lifecycle chaincode approveformyorg \
  --channelID ${CHANNEL_NAME} \
  --name ${CC_NAME} \
  --version ${CC_VERSION} \
  --package-id ${PACKAGE_ID} \
  --sequence ${CC_SEQUENCE} \
  --tls \
  --cafile ${ORDERER_TLS_CA} \
  --orderer ${ORDERER_ADDRESS}

# Step 5: Commit chaincode to channel
echo "5. Committing chaincode to channel..."
docker exec -e CORE_PEER_LOCALMSPID=Org1MSP \
  -e CORE_PEER_MSPCONFIGPATH=/var/hyperledger/peer/msp \
  -e CORE_PEER_ADDRESS=${PEER_ADDRESS} \
  -e CORE_PEER_TLS_ROOTCERT_FILE=${PEER_TLS_CA} \
  ${CLI_CONTAINER} \
  peer lifecycle chaincode commit \
  --channelID ${CHANNEL_NAME} \
  --name ${CC_NAME} \
  --version ${CC_VERSION} \
  --sequence ${CC_SEQUENCE} \
  --tls \
  --cafile ${ORDERER_TLS_CA} \
  --orderer ${ORDERER_ADDRESS} \
  --peerAddresses ${PEER_ADDRESS} \
  --tlsRootCertFiles ${PEER_TLS_CA}

# Step 6: Verify deployment
echo "6. Verifying deployment..."
docker exec -e CORE_PEER_LOCALMSPID=Org1MSP \
  -e CORE_PEER_MSPCONFIGPATH=/var/hyperledger/peer/msp \
  -e CORE_PEER_ADDRESS=${PEER_ADDRESS} \
  -e CORE_PEER_TLS_ROOTCERT_FILE=${PEER_TLS_CA} \
  ${CLI_CONTAINER} \
  peer lifecycle chaincode querycommitted \
  --channelID ${CHANNEL_NAME} \
  --name ${CC_NAME} \
  --tls \
  --cafile ${ORDERER_TLS_CA}

echo ""
echo "=== ${CC_NAME} deployed successfully ==="
echo "Chaincode: ${CC_NAME}_${CC_VERSION}"
echo "Channel:   ${CHANNEL_NAME}"
echo "Sequence:  ${CC_SEQUENCE}"
echo ""
echo "Test invocation:"
echo "docker exec ${CLI_CONTAINER} peer chaincode invoke -C ${CHANNEL_NAME} -n ${CC_NAME} -c '{\"function\":\"CreateCarbonReport\",\"Args\":[\"1\",\"test\"]}' --tls --cafile ${ORDERER_TLS_CA} --orderer ${ORDERER_ADDRESS}"