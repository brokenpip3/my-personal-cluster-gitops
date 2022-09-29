// Archlinux falco probe builder
pipeline {
    agent {
        kubernetes {
            yaml """
kind: Pod
metadata:
  name: falco-builder
spec:
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
    options {
        timeout(time: 15, unit: 'MINUTES')
    }
    parameters {
        string(name: 'arch', defaultValue: 'amd64', description: 'The architecture to build for')
        string(name: 'driverversion', defaultValue: '2.0.0+driver', description: 'The version of the driver to build')
        string(name: 'kernelrelease', defaultValue: '5.15.64-1-lts', description: 'The kernel release to build for')
        string(name: 'kernelversion', defaultValue: '1', description: 'The kernel version to build for')
        string(name: 'output-module', defaultValue: '/tmp/falco-arch.ko', description: 'The output module path')
        string(name: 'output-probe', defaultValue: '/tmp/falco-arch.o', description: 'The output probe path')
    }
    stages {
        stage("setname") {
            steps {
                buildName "falco-builder #${BUILD_NUMBER}"
                buildDescription "${BUILD_NUMBER}"
            }
        }
        stage('pacman') {
            steps {
                container('falco-builder') {
                    sh 'sudo pacman -Sy go git --noconfirm'
                }
            }
        }
        stage("install driverkit") {
            steps {
                container('falco-builder') {
                    sh "git clone https://github.com/falcosecurity/driverkit.git"
                    sh "cd driverkit; make build"
                }
            }
        }
        stage('install dep') {
            steps {
                container('falco-builder') {
                    sh """
                    #!/bin/bash
                    cd driverkit
                    _output/bin/driverkit kubernetes-in-cluster --target=arch  \
                      --kernelrelease=${params.kernelrelease} --kernelversion=${params.kernelversion} \
                      --driverversion=${params.driverversion} --output-module=${params.output-module} \
                      --output-probe=${params.output-probe}
                    """
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
