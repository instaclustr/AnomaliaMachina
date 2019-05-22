# Anomalia Machina
Massively Scalable Anomaly Detection with Apache Kafka, Cassandra and Kubernetes - final code for **Instaclustr's Anomalia Machina Blog series**:

* [Anomalia Machina 1 - Introduction](https://www.instaclustr.com/anomalia-machina-1-massively-scalable-anomaly-detection-with-apache-kafka-and-cassandra/)
* [Anomalia Machina 4 - Prototype](https://www.instaclustr.com/anomalia-machina-4-prototype-massively-scalable-anomaly-detection-apache-kafka-cassandra/)
* [Anomalia Machina 5 - Prometheus](https://www.instaclustr.com/anomalia-machina-5-1-application-monitoring-prometheus-massively-scalable-anomaly-detection-apache-kafka-cassandra/)
* [Anomalia Machina 6 - OpenTracing and Jaeger](https://www.instaclustr.com/anomalia-machina-6-application-tracing-opentracing-massively-scalable-anomaly-detection-apache-kafka-cassandra/)
* [Anomalia Machina 7 - Creating a Kubernetes Cluster on AWS](https://www.instaclustr.com/anomalia-machina-7-application-deployment-kubernetes/)
* [Anomalia Machina 8 - Production Kubernetes](https://www.instaclustr.com/anomalia-machina-8-production-application-deployment-kubernetes-massively-scalable-anomaly-detection-apache-kafka-cassandra/)
* [Anomalia Machina 9 - Scaling Out](https://www.instaclustr.com/anomalia-machina-9-anomaly-detection-at-scale/)
* [Anomalia Machina 10 - Final Results](https://www.instaclustr.com/anomalia-machina-10-final-results-massively-scalable-anomaly-detection-with-apache-kafka-and-cassandra/)

## Instructions

For the design and more detailed instructions see the blogs (above). Here are the basic steps.

To run the Anomaly Detection pipeline you need to have the following configured and running (all on AWS):
* Instaclustr [Kafka](https://www.instaclustr.com/solutions/managed-apache-kafka/) and [Cassandra](https://www.instaclustr.com/solutions/managed-apache-cassandra/) clusters (for Cassandra, no authentication)
* Kubernetes running in the same region as the Kafka and Cassandra clusters (E.g. On AWS use EKS)
* Edit KafkaProperties.java with the Instaclustr Kafka cluster credentials
* Edit AnomaliaProperties.jave with the Instaclustr Provisioning API credentials
* Either: Configure Kafka and Cassandra cluster firewalls to enable access from Kubernetes (and use public IPs, this assumes you know the IPs of the Kubernetes worker nodes), or set up VPC peering between the Kubernetes cluster and the Instaclustr clusters (and use private IPs)
* A local Docker and Kubernetes (On a Mac I was using the Docker community edition which comes with Kubernetes)
* A Docker hub account (edit the xxx.sh files with the account name)
* An IDE with the code loaded

To deploy and run the application:
* Generate executable two jar files, one called consumer.jar from AnomaliaMainConsumer.jar, and one called producer.jar from AnomaliaMainProducer.jar
* Start 1 or more Kubernetes worker nodes in AWS (using auto scaling groups)
* Deploy Prometheus using the deploy_prometheus.sh script
* Deploy the producer using the deploy_producer.sh script
* Deploy the consumer using the deploy_consumer.sh script
* Look at the prometheus metrics in a broswer (you'll need to copy an IP address of one of the Kubernetes worker nodes from the AWS console into your browser), e.g. http://1.2.3.4:30123
* The producer load and consumers can be scaled by increasing the number of Kubernetes worker nodes and increasing the number of pods for producers and consumers.  Some tuning of the parameters in AnomaliaProperties.java will be required to ensure optimal throughput. 

## Instaclustr Open Source Project Status: SAMPLE
- for further information see: https://www.instaclustr.com/support/documentation/announcements/instaclustr-open-source-project-status/
