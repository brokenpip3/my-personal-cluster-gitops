// Universal docker image build ci with Kanikp
//
// TDB

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
    image: brokenpip3/ubuntu-toolbox
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
                stage('validation') {
                    steps {
                        // Check if some optional params are empty and fill them with git info
                        script {
                            // If image name is not procided set it from repo name
                            // Example https://github.com/foo/bar will set image name = bar
                            if (params.IMAGE_NAME.isEmpty()) {
                                env.IMAGE_NAME = params.GIT_URL.replaceFirst(/^.*\/([^\/]+?).git$/, '$1')
                            }
                            // If image tag is empty but git tag is provided
                            // set image tag equals to git tag
                            if (params.IMAGE_TAG.isEmpty() && env.GIT_BRANCH.isEmpty()) {
                                env.IMAGE_TAG = params.GIT_TAG
                            }
                        }
                    }
                }
				stage('Check if tag exist') {
                    when {
                        allOf {
                            expression { return params.GIT_BRANCH.isEmpty() }
                            not {
                                expression { return params.GIT_TAG.isEmpty() }
                            }
                        }
                    }
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
                    when {
                        anyOf {
                            expression { return env.IMAGE_EXIST == 'false'}
                            not {
                                expression { return params.GIT_BRANCH.isEmpty()}
                            }
                        }
                    }
                    steps {
						    // If git tag is specified checkout tag and set docker img tag
							// otherwise use branch and set "latest" as docker img tag
							script {
                                if (params.GIT_TAG.isEmpty()) {
                                    checkout([
                                    $class: 'GitSCM',
                                    branches: [[name: "refs/heads/${params.GIT_BRANCH}"]],
                                    userRemoteConfigs: [[ url: "${params.GIT_URL}"]]
                                    ])
                                    if (params.GIT_FORCE_LAST_TAG) {
                                        final def LASTTAG = sh(returnStdout: true, script: "git describe --tags --abbrev=0").trim()
                                        env.IMAGE_TAG = LASTTAG
                                    } else {
                                        env.IMAGE_TAG = "latest"
                                    }
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
				stage('Make Image') {
                    when {
                        anyOf {
                            expression { return env.IMAGE_EXIST == 'false'}
                            not {
                                expression { return params.GIT_BRANCH.isEmpty()}
                            }
                        }
                    }
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

        post {
            always {
                cleanWs()
            }
        }
}
