// Archlinux falco probe builder
pipeline {
    agent {
        kubernetes {
            yaml """
kind: Pod
metadata:
  name: falco-builder
spec:
  serviceAccount: falco-ci
  containers:
  - name: falco-builder
    workingDir: /tmp/jenkins
    image: quay.io/brokenpip3/basearch:latest
    imagePullPolicy: Always
    envFrom:
    - secretRef:
        name: jenkins-aur-repo
    args:
      - "sh"
      - "-c"
      - 'touch /tmp/build.log && tail -f /tmp/build.log'
    tty: true
    resources:
      limits:
        memory: 2Gi
        cpu: 1500m
        ephemeral-storage: 10Gi
      requests:
        memory: 1Gi
        cpu: 500m
        ephemeral-storage: 10Gi
    volumeMounts:
      - name: repo-pvc
        mountPath: /srv/repo
  volumes:
  - name: repo-pvc
    persistentVolumeClaim:
      claimName: jenkins-aur-packages
"""
        }
    }
    parameters {
        string(name: 'arch', defaultValue: 'amd64', description: 'The architecture to build for')
        string(name: 'driverversion', defaultValue: '2.0.0+driver', description: 'The version of the driver to build')
        string(name: 'kernelrelease', defaultValue: '5.15.74-1-lts', description: 'The kernel release to build for')
        string(name: 'kernelversion', defaultValue: '1', description: 'The kernel version to build for')
        string(name: 'outputmodule', defaultValue: '/tmp/falco-arch.ko', description: 'The output module path')
        string(name: 'outputprobe', defaultValue: '/tmp/falco-arch.o', description: 'The output probe path')
    }
    stages {
        stage("setname") {
            steps {
                buildName "falco-builder #${BUILD_NUMBER}"
                buildDescription "${BUILD_NUMBER}"
            }
        }
        stage('pacman prereq') {
            steps {
                container('falco-builder') {
                    sh 'sudo pacman -Syu git --noconfirm --needed'
                }
            }
        }
        stage("install driverkit") {
            steps {
                container('falco-builder') {
                    sh "sudo pacman -S driverkit --noconfirm"
                    sh "driverkit --version"
                }
            }
        }
        stage('build probe') {
            steps {
                container('falco-builder') {
                    sh """
                    #!/usr/bin/env bash
                    driverkit kubernetes-in-cluster --target=arch  \
                      --kernelrelease=${kernelrelease} --kernelversion=${kernelversion} \
                      --driverversion=${driverversion} --output-module=${outputmodule} \
                      --output-probe=${outputprobe} --namespace=jenkins -l debug --builderimage=quay.io/brokenpip3/arch-driverkit
                    """
                }
            }
        }
        stage("move probe to the repo") {
            steps {
                container('falco-builder') {
                    sh "cp ${outputprobe} /srv/repo/${driverversion}/x86_64/falco_arch_${kernelrelease}_${kernelversion}.o"
                    sh "cp ${outputmodule} /srv/repo/${driverversion}/x86_64/falco_arch_${kernelrelease}_${kernelversion}.ko"
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
