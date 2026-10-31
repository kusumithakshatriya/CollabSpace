pipeline {
    agent any

    environment {
        IMAGE_NAME = 'collabspace'
        CONTAINER_NAME = 'collabspace'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Application') {
            steps {
                bat 'mvn -B clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat 'docker build -t %IMAGE_NAME%:latest .'
            }
        }

        stage('Stop Existing Container') {
            steps {
                bat 'docker rm -f %CONTAINER_NAME% || exit /b 0'
            }
        }

        stage('Deploy Container') {
            steps {
                bat 'docker run -d --restart unless-stopped --name %CONTAINER_NAME% -p 8080:8080 %IMAGE_NAME%:latest'
            }
        }
    }

    post {
        success {
            echo 'CollabSpace deployed successfully!'
        }
        failure {
            echo 'Deployment failed. Check Console Output.'
        }
    }
}