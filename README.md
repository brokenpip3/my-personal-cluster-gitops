# Gitops repo for my personal cluster

TDB

## Components installed

### Infra

* [metrics-server](infra/helm-metric-server.yaml)
* [nginxinc ingress-controller](infra/helm-nginxinc-ingress-controller.yaml)
* [cert-manager](infra/helm-certmanager.yaml)
* [ovh-cert-manager-webhook](infra/helm-certmanager.yaml)
* [prometheus stack/operator](infra/prometheus)

#### Datastores

* [zalando-postgres-operator](infra/datastores/postgres-zalando-operator.yaml)

### Promethues CRDs

*

### Apps

* [gitea](apps/gitea)
* [minio](apps/minio)
