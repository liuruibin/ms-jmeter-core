pipeline {
    agent {
        node {
            label 'metersphere'
        }
    }
    options { quietPeriod(600) }
    stages {
        stage('Preparation') {
            steps {
                script {
                    REVISION = ""
                    if (env.BRANCH_NAME.startsWith("v") ) {
                        REVISION = env.BRANCH_NAME.substring(1)
                    } else {
                        REVISION = env.BRANCH_NAME
                    }
                    env.REVISION = "${REVISION}"
                    echo "REVISION=${REVISION}"
                }
            }
        }
        stage('Build/Test') {
            steps {
                configFileProvider([configFile(fileId: 'metersphere-maven', targetLocation: 'settings.xml')]) {
                    sh """
                    mvn clean install -Dgpg.skip -Drevision=${REVISION} -Dmaven.javadoc.skip=true --settings ./settings.xml
                    """
                }
            }
        }
    }
}
