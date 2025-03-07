# Instant Payment API

## **Overview**
The **Instant Payment API** is a high-availability payment processing service that enables users to send money instantly using a RESTful API. The system ensures **transactional integrity, concurrency handling, and fault tolerance**, leveraging **Spring Boot, PostgreSQL, Kafka, and Eureka Service Discovery**.

## **Key Features**
- **Transactional Processing**: Ensures account balance checks before transactions.
- **Concurrency Handling**: Prevents double spending and duplicate notifications.
- **Database Persistence**: Saves transactions in a **PostgreSQL** database.
- **Asynchronous Notifications**: Uses **Kafka** to notify recipients of payments.
- **Service Discovery & Load Balancing**: Manages microservices with **Eureka Naming Server** and **API Gateway**.
- **Containerization**: Uses **Docker Compose** for easy deployment.

## **Architecture**
The system consists of the following components:

1. **API Gateway (`api-gateway/`)** - Routes requests to the correct service and enables load balancing.
2. **Naming Server (`naming-server/`)** - Manages service discovery using Eureka.
3. **Payment API (`instant-payment-api/`)** - The core microservice responsible for processing payments.
4. **PostgreSQL (`postgres`)** - Stores account and transaction data.
5. **Kafka (`kafka`)** - Handles asynchronous transaction notifications.
6. **Kafka Consumer (`kafka-consumer`)** - A simple consumer script that logs incoming transaction notifications.

## **Setup & Deployment**

### **Prerequisites**
*For Development*
- Java (recommended **Java 17+**)
- Maven or Gradle

*For Deployment*
- Docker & Docker Compose

### **Run the Application**
To start all services, run:
```sh
docker-compose up -d --build
```
This will start the API Gateway, Naming Server, Payment API instances, Kafka, PostgreSQL, and the Kafka Consumer.

### **Eureka Service Discovery**
Visit **[http://localhost:8761/](http://localhost:8761/)** to see the registered services.

## **API Usage**

### **1. Create an Account**
#### **Create a Savings Account:**
```sh
curl --location 'localhost:8080/api/accounts' \
--header 'Content-Type: application/json' \
--data '{
    "accountName": "Savings account",
    "ownerName": "Tamas Pinter"
}'
```
#### **Create a Spending Account:**
```sh
curl --location 'localhost:8080/api/accounts' \
--header 'Content-Type: application/json' \
--data '{
    "accountName": "Spending account",
    "ownerName": "Tamas Pinter"
}'
```

### **2. Check Account Details**
```sh
curl --location 'localhost:8080/api/accounts/1'
```

### **3. Test Kafka Messaging**
```sh
curl --location 'localhost:8080/api/test-kafka/send?msg=hello%20world'
```

### **4. Make a Payment**
Manually add a balance to an account in the database before testing payments.
```sh
curl --location 'localhost:8080/api/payments' \
--header 'Content-Type: application/json' \
--data '{
    "fromAccountId":1,
    "toAccountId":2,
    "amount":10.00
}'
```

This will fail if the account balance is insufficient.
To add balance to an account, use the database or the following API:
```sh
curl --location 'localhost:8080/api/accounts/deposit' \
--header 'Content-Type: application/json' \
--data '{
    "accountId": 1,
    "amount": 100
}'
```

## **Database Schema**
The PostgreSQL database consists of two primary tables:
- **`accounts`** - Stores user accounts with balances.
- **`transactions`** - Logs all payment transactions.

## Improvement Ideas:

Answering the question: *"Bonus for Senior Candidates:
How would you architect an instant payment system to ensure high availability and fault tolerance?
(covering database partitioning, microservices, and resilience patterns)."*

To make the API fault-tolerant, I already started to implement a microservice architecture with the Instant Payment API project.
There is an API Gateway, a Naming Server, and a Payment API microservice that handles the payment processing logic.
To make it even more fault-tolerant, I would introduce a few more microservices:
- **API Gateway**: Routes requests to the correct service and enables load balancing. - I already have this in the project.
- **Naming Server**: Manages service discovery using Eureka. - I already have this in the project.
- **Payment API**: The core microservice responsible for processing payments. - I already have this in the project, but I would split it into smaller services.
- **Account Service**: Responsible for managing user accounts and balances.
- **Transaction/Payment Service**: Handles transaction processing and logging.
- **Notification Service**: Sends notifications to users about incoming payments.
- **Authentication Service**: Manages user authentication and authorization while also securing inter-service communication using tools like **OAuth** or **certificate-based authentication**.
- **Monitoring Service**: Monitors the health of the system and alerts on failures.
- **Fraud Detection Service**: Detects fraudulent transactions and blocks them.

### Fault-tolerant communication between these services:

- **synchronous operations**:
Creating a **Feign client** and using **REST** calls between services
- **asyncronous communication**:
Introducing a message broker like **Kafka** for event driven stuff, like notifications.

### Database partitioning:
I separated the accounts and transactions into two tables,
but to make it more fault-tolerant, I would create multiple instances of the database across regions.

I used optimistic locking (by adding @Version fields to the entities) to ensure two concurrent calls don't update the same account balance.

### Resilience patterns:
- **Circuit Breaker**: To prevent cascading failures (Previously I used **Resilience4j**).
- **Retry**: To exponentially back off and retry failed operations.
- **Dead Letter Queue**: To handle failed messages in Kafka.

### High Availability:
- **Load Balancing using API Gateway**: To distribute incoming requests across multiple instances of the same service. - I am already doing this, but it can be done with K8s routing as well, or using a third party cloud's gateway service.
- **Topic replication/Kafka Cluster**: Kafka can also be made more fault-tolerant by setting up topic replication, or a cluster with multiple brokers.
- **Monitoring**: Using tools like **Logstash**, **DataDog**, **Prometheus** and **Grafana** to monitor the health of the system and alert on failures.
- **Automated Failover**: Using tools like **Kubernetes** to automatically restart failed services.
