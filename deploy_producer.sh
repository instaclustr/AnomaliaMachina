docker build -f Dockerfile.producer -t producer .
docker tag producer user/producer:latest
docker push user/producer
kubectl delete deployment producer-deployment
kubectl create -f k8_producer.yaml
kubectl get deployments
kubectl get services
