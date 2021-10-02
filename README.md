# Gitops repo for my personal cluster

This is my "fleet" repo that describe my entire k8s cluster.

## Components installed

### Infra

* [metrics-server](infra/helm-metric-server.yaml)
* [nginxinc ingress-controller](infra/helm-nginxinc-ingress-controller.yaml)
* [cert-manager](infra/helm-certmanager.yaml)
* [ovh-cert-manager-webhook](infra/helm-certmanager.yaml)
* [prometheus stack/operator](infra/prometheus)

#### Datastores Operator

* [zalando-postgres-operator](infra/datastores/postgres-zalando-operator.yaml)

### Promethues CRDs

*

### Apps

* [gitea](apps/gitea)
* [minio](apps/minio)
* [jenkins](apps/jenkins)
* [zulip](apps/zulio)
  * [Zulop exporter](apps/zulip/zulip-exporter.yaml)
