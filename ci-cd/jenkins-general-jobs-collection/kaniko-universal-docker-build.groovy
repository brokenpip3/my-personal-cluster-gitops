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
  - name: utils
    workingDir: /tmp/jenkins
    image: brokenpip3/ubuntu-curl-jq
    command:
    - sleep
    args:
    - infinity
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

		options { skipDefaultCheckout true
		timeout(time: 15, unit: 'MINUTES')
		}

		parameters {
				string(name: 'GIT_URL', defaultValue: '', description: 'Git url')
				string(name: 'GIT_BRANCH', defaultValue: '', description: 'Git branch')
				string(name: 'GIT_TAG', defaultValue: '', description: 'Git tag')
				string(name: 'IMAGE_NAME', defaultValue: '', description: 'Image name')
				string(name: 'IMAGE_REGISTRY', defaultValue: 'quay.io', description: 'Image name')
				string(name: 'IMAGE_ORG', defaultValue: 'brokenpip3', description: 'Image organization/username')
				string(name: 'IMAGE_TAG', defaultValue: '', description: 'Image tag')
				string(name: 'IMAGE_DOCKERFILE_PATH', defaultValue: 'Dockerfile', description: 'Image dockerfile name')
				string(name: 'IMAGE_DOCKERFILE_DIR', defaultValue: '.', description: 'Image dockerfile path')
		}

        //curl -s https://quay.io/api/v1/repository/brokenpip3/tg2| jq -e -r '.tags[] | select(.name=="0.0.2")| any'  > /dev/null
		stages {
				stage('Check if tag exist') {
                    when {  expression { params.GIT_BRANCH.isEmpty() }}
                    steps {
                            script {
                                container(name: 'utils') {
                                    final String url = "https://quay.io/api/v1/repository/${params.IMAGE_ORG}/${params.IMAGE_NAME}"
                                    final def imagereturncode = sh(script: """
                                    #!/usr/bin/env bash
                                    curl -s $url | jq -r '.tags[] | select(.name==\"$GIT_TAG\") | any'
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
				stage('Checkout') {
                    when { expression { return env.IMAGE_EXIST == 'false'} }
					steps {
						    // If git tag is specified checkout tag and the same docker img tag
							// otherwise use branch and "latest" as docker img tag
							script {
                                if (params.GIT_TAG.isEmpty()) {
                                    checkout([
                                    $class: 'GitSCM',
                                    branches: [[name: "refs/heads/${GIT_BRANCH}"]],
                                    userRemoteConfigs: [[ url: "${GIT_URL}"]]
                                    ])
                                    env.IMAGE_TAG = "latest"
                                } else {
									checkout([
									$class: 'GitSCM',
									branches: [[name: "refs/heads/${GIT_TAG}"]],
									userRemoteConfigs: [[ url: "${GIT_URL}"]]
                                    ])
                                }
                            }
						}
				}
				stage('Make Image') {
                    when { expression { return env.IMAGE_EXIST == 'false'} }
                    steps {
                            container(name: 'kaniko', shell: '/busybox/sh') {
										sh """#!/busybox/sh
										   /kaniko/executor -f ${IMAGE_DOCKERFILE_PATH} -c ${IMAGE_DOCKERFILE_DIR} --cache=false \
										     --destination=${IMAGE_REGISTRY}/${IMAGE_ORG}/${IMAGE_NAME}:${IMAGE_TAG}
										"""
								}
						}
				}
		}
}
