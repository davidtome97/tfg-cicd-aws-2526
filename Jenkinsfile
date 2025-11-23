pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                echo "Haciendo checkout del repositorio..."
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Compilando proyecto Java..."
                sh "chmod +x mvnw"
                sh "./mvnw -B clean package"
            }
        }

        stage('Test') {
            steps {
                echo "Ejecutando tests..."
                sh "./mvnw test"
            }
        }

        // stage('SonarQube') {
        //     steps {
        //         echo "Analizando código con SonarQube..."
        //         sh "./mvnw sonar:sonar"
        //     }
        // }

        stage('Deploy') {
            steps {
                echo "Despliegue: pendiente de implementar"
            }
        }
    }
}