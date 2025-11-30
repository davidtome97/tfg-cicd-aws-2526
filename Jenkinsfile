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
                sh "mvn -B clean package"
            }
        }

        stage('Test') {
            steps {
                echo "Ejecutando tests..."
                sh "mvn test"
            }
        }

        // Si quiero activar SonarCloud:
        // stage('SonarQube') {
        //     steps {
        //         withSonarQubeEnv('SonarCloud') {
        //             sh "mvn sonar:sonar"
        //         }
        //     }
        // }

        stage('Deploy') {
            steps {
                echo "Despliegue: pendiente de implementar"
            }
        }
    }
}