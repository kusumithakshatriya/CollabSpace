pipeline {
  agent any
  tools { jdk 'jdk17'; maven 'maven3' }
  environment { IMAGE_NAME = 'collabspace' }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Build') { steps { sh 'mvn -B clean package' } }
    stage('Test') { steps { sh 'mvn -B test' } }
    stage('Build Docker image') {
      steps { sh 'docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} .' }
    }
    stage('Deploy to Kubernetes') {
      when { branch 'main' }
      steps {
        sh 'kubectl apply -f k8s/'
        sh 'kubectl set image deployment/collabspace collabspace=${IMAGE_NAME}:${BUILD_NUMBER} --namespace=default'
        sh 'kubectl rollout status deployment/collabspace --timeout=120s'
      }
    }
  }
  post { success { echo 'CollabSpace CI/CD pipeline completed.' } failure { echo 'Pipeline failed. Review the stage logs.' } }
}
