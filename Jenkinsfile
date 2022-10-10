pipeline {
    agent {
        node {
            label 'metersphere'
        }
    }
    triggers {
        pollSCM('0 * * * *')
    }
    environment {
        JAVA_HOME = '/opt/jdk-11'
    }
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
        stage('SDK XPack Interface') {
            when {
                anyOf {
                    branch "main"
                    branch pattern: "^v\\d+\\.\\d+\$", comparator: "REGEXP";
                }
            }
            steps {
                script {
                    build job:"../metersphere/${BRANCH_NAME}", quietPeriod:10, parameters: [string(name: 'buildParent', value: String.valueOf("true"))]
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
