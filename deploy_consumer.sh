docker build -f Dockerfile.consumer -t consumer .
docker tag consumer user/consumer:latest
docker push user/consumer
kubectl delete deployment consumer-deployment
kubectl create -f k8_consumer.yaml
kubectl get deployments
kubectl get services
