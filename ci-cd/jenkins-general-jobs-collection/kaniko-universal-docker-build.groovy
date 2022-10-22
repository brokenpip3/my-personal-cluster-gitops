// Universal docker image build ci with Kanikp
//
// Condition for image build:
// - If GIT_BRANCH is specified build image from that branch with IMAGE_TAG equals to latest
// - If GIT_BRANCH is specified and GIT_FORCE_LAST_TAG is enabled build image from last tag on that branch if does not alredy exist on quay.io
// - if GIT_TAG is specified build image from that tag if it does not already exist on quay.io
//
// Other assumptions:
// - If IMAGE_NAME is empty repo name is used
// - If IMAGE_TAG is empyty and GIT_TAG is provided set image tag equals to git tag
//
// Please be aware that due to:
// - https://github.com/jenkinsci/kubernetes-plugin#notedue-to-implementation-constraints-there-can-be-issues-when-executing-commands-in-different-containers-if-they-run-using-different-uidsit-is-recommended-to-use-the-same-uid-across-the-different-containers-part-of-the-same-pod-to-avoid-any-issue
// - https://github.com/GoogleContainerTools/kaniko/issues/105
// the containers must run with the same user id
// and since kaniko need id 0 the containers will run as root user

pipeline {
		agent {
				kubernetes {
                    defaultContainer 'kaniko'
                    yaml '''
kind: Pod
apiVersion: v1
metadata:
  name: kaniko-build
spec:
  containers:
  - name: kaniko
    workingDir: /tmp/jenkins
    image: gcr.io/kaniko-project/executor:debug
    imagePullPolicy: Always
    command:
    - sleep
    args:
    - infinity
    tty: true
    volumeMounts:
    - name: jenkins-docker-cfg
      mountPath: /kaniko/.docker
    resources:
      limits:
        memory: 2Gi
        cpu: 1500m
        ephemeral-storage: 5Gi
      requests:
        memory: 1Gi
        cpu: 500m
        ephemeral-storage: 5Gi
  - name: utils
    workingDir: /tmp/jenkins
    image: brokenpip3/ubuntu-toolbox-jenkins
    tty: true
    command:
    - sleep
    args:
    - infinity
    resources:
      limits:
        memory: 500Mi
        cpu: 500m
      requests:
        memory: 500Mi
        cpu: 500m
  volumes:
  - name: jenkins-docker-cfg
    secret:
      secretName: brokenpip3-jenkins-pull-secret
      items:
      - key: .dockerconfigjson
        path: config.json
'''
				}
		}
		// https://github.com/GoogleContainerTools/kaniko/issues/1542
		environment {
				container = 'docker'
		}

		options {
            skipDefaultCheckout true
            timeout(time: 15, unit: 'MINUTES')
            // https://plugins.jenkins.io/throttle-concurrents/
            throttleJobProperty(
                categories: ['docker-images'],
                throttleEnabled: true,
                throttleOption: 'category',
                )
		}

		parameters {
				string(name: 'GIT_URL', defaultValue: '', description: 'Git url, must end with canonical .git')
				string(name: 'GIT_BRANCH', defaultValue: '', description: 'Git branch')
				string(name: 'GIT_TAG', defaultValue: '', description: 'Git tag')
				string(name: 'IMAGE_NAME', defaultValue: '', description: 'Image name')
				string(name: 'IMAGE_REGISTRY', defaultValue: 'quay.io', description: 'Image name')
				string(name: 'IMAGE_ORG', defaultValue: 'brokenpip3', description: 'Image organization or username')
				string(name: 'IMAGE_TAG', defaultValue: '', description: 'Image tag')
				string(name: 'IMAGE_DOCKERFILE_PATH', defaultValue: 'Dockerfile', description: 'Image dockerfile name')
				string(name: 'IMAGE_DOCKERFILE_DIR', defaultValue: '.', description: 'Image dockerfile path')
                booleanParam(name: 'GIT_FORCE_LAST_TAG', defaultValue: false, description: 'Force build last tag from GIT_BRANCH')
		}

		stages {
				stage('Checkout') {
                    steps {
                        // If git tag is specified checkout tag and set docker img tag otherwise use branch
                        script {
                            if (params.GIT_TAG.isEmpty()) {
                                checkout([
                                    $class: 'GitSCM',
                                    branches: [[name: "refs/heads/${params.GIT_BRANCH}"]],
                                    userRemoteConfigs: [[ url: "${params.GIT_URL}"]]
                                    ])
                            } else {
								checkout([
									$class: 'GitSCM',
									branches: [[name: "refs/tags/${params.GIT_TAG}"]],
									userRemoteConfigs: [[ url: "${params.GIT_URL}"]]
                                    ])
                                }
                            }
						}
                }
                stage('Validation') {
                    steps {
                        // Check if some optional params are empty and fill them with git info
                        script {
                            container(name: 'utils') {
                                // If image name is not provided set it from repo name
                                // Example https://github.com/foo/bar will set image name = bar
                                if (params.IMAGE_NAME.isEmpty()) {
                                    env.IMAGE_NAME = params.GIT_URL.replaceFirst(/^.*\/([^\/]+?).git$/, '$1')
                                }
                                // If image tag is empty but git tag is provided
                                // set image tag equals to git tag
                                if (params.IMAGE_TAG.isEmpty() && env.GIT_BRANCH.isEmpty()) {
                                    env.IMAGE_TAG = params.GIT_TAG
                                }
                                // if GIT_FORCE_LAST_TAG is enabled set last git tag as image tag
                                // otherwise use latest
                                if (params.GIT_TAG.isEmpty()) {
                                    if (params.GIT_FORCE_LAST_TAG) {
                                        final def lasttag = sh(script: "git describe --tags --abbrev=0", returnStdout: true).trim()
                                        env.IMAGE_TAG = lasttag
                                    } else {
                                        env.IMAGE_TAG = "latest"
                                    }
                                }
                            }
                        }
                    }
                }
				stage('Check if tag exist') {
                    // Check if the tag exist in quay.io
                    when { expression { env.IMAGE_TAG != "latest" } }
                    steps {
                            script {
                                container(name: 'utils') {
                                    final String url = "https://quay.io/api/v1/repository/${params.IMAGE_ORG}/${env.IMAGE_NAME}"
                                    final def imagereturncode = sh(script: """
                                    #!/usr/bin/env bash
                                    curl -s $url | jq -r '.tags[] | select(.name==\"$IMAGE_TAG\") | any'
                                    """, returnStdout: true).trim()

                                    if (imagereturncode == "true") {
                                        env.IMAGE_EXIST = "true"
                                    } else {
                                        env.IMAGE_EXIST = "false"
                                    }
                                }
                            }
                        }
                    }
				stage('Make Image') {
                    // Build image with Kaniko only if does not exist on destination or is latest
                    when {
                        anyOf {
                            expression { return env.IMAGE_EXIST == 'false'}
                            expression { return env.IMAGE_TAG == "latest" }
                        }
                    }
                    steps {
                        container(name: 'kaniko', shell: '/busybox/sh') {
                            sh """#!/busybox/sh
                                  /kaniko/executor -f ${IMAGE_DOCKERFILE_PATH} -c ${IMAGE_DOCKERFILE_DIR} --cache=false \
                                    --destination=${IMAGE_REGISTRY}/${IMAGE_ORG}/${IMAGE_NAME}:${IMAGE_TAG} --log-format=text
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
