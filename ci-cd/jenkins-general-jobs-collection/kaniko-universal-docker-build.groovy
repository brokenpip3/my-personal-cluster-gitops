pipeline {
		agent {
				kubernetes {
						label 'kaniko-build'
						yaml '''
kind: Pod
apiVersion: v1
metadata:
  name: kaniko-build
spec:
  containers:
  - name: jnlp
    workingDir: /tmp/jenkins
  - name: kaniko
    workingDir: /tmp/jenkins
    image: gcr.io/kaniko-project/executor:debug
    imagePullPolicy: Always
    command:
    - /busybox/cat
    tty: true
  volumeMounts:
  - name: jenkins-docker-cfg
    mountPath: /kaniko/.docker
  volumes:
  - name: jenkins-docker-cfg
  projected:
    sources:
     - secret:
       name: brokenpip3-jenkins-pull-secret
       items:
       - key: .dockerconfigjson
         path: config.json
'''
				}
		}

		options { disableConcurrentBuilds()
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

		stages {
				stage('Checkout') {
					steps {
						checkout([
						$class: 'GitSCM',
						branches: [[name: "refs/heads/${GIT_BRANCH}"]],
						userRemoteConfigs: [[ url: "${GIT_URL}"]]
						])
						}
				}
				stage('Make Image') {
						steps {
								withFolderProperties {
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
}
