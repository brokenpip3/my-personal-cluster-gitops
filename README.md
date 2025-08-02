# Gitops repo for my personal cluster

This is my "fleet" repo that describe my entire k8s cluster, made with :heart: using [Flux](https://github.com/fluxcd/flux2)

## Components installed

### Infra

* [metrics-server](infra/helm-metric-server.yaml)
* [nginxinc ingress-controller](infra/helm-nginxinc-ingress-controller.yaml)
* [cert-manager](infra/certmanager)
* [ovh-cert-manager-webhook](infra/certmanager)
* [prometheus stack/operator](infra/prometheus)
* [prometheus-alertmanager-telegram-bot](infra/prometheus/telegram)
* [etcd-backup](infra/utils/etcd-backup)
* [nfs-storage](infra/storage)
* [loki](infra/loki)

### Ci

* [Tekton pipelines](infra/tekton/tekton-pipelines)
* [Tekton triggers](infra/tekton/tekton-triggers)

### Datastores Operator

* [zalando-postgres-operator](infra/datastores/postgres-zalando-operator.yaml)
* [minio](apps/minio)

### Prometheus CRDs

* [Pacman and brtfs rules](infra/prometheus/prom-crds/rules-pacman.yaml)
* [Postgres rules](infra/prometheus/prom-crds/rules-postgres.yaml)
* [Jenkins rules](infra/prometheus/prom-crds/rules-jenkins.yaml)
* [Redis-memcached-rabbit rules](infra/prometheus/prom-crds/rules-other-datastores.yaml)
* [borg-backup](infra/prometheus/prom-crds/rules-borgbackup.yaml)
* [Others](infra/prometheus/prom-crds)

### Apps

* [Gitea](apps/gitea)
* [Falco](apps/falco)
* [Jenkins](apps/jenkins)
  * [Jenkins-operator](apps/jenkins/helm-jenkins-release.yaml)
  * [Jenkins Groovy and Casc configuration](apps/jenkins/jenkins-configuration.yaml)
* [Taskd](apps/taskd) * ARCHIVED
* [Memos](apps/memos)
* [bowling-mania](apps/bowling-mania)
* [lotb](apps/lotb) https://github.com/brokenpip3/lotb

### Utils

* [urlwatch](apps/utils/urlwatch)
* [rtt](apps/utils/rtt)
* [ublock-searchlist](apps/utils/ublock-searchlist)
