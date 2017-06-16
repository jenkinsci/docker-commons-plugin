REM check existence of the credentials dir
if not exist %DOCKER_CERT_PATH% exit /B 1

REM check existence of the certificate files
if not exist %DOCKER_CERT_PATH%\key.pem exit /B 1
if not exist %DOCKER_CERT_PATH%\cert.pem exit /B 1
if not exist %DOCKER_CERT_PATH%\ca.pem exit /B 1

REM keep location of the certificate dir for the next step
echo %DOCKER_CERT_PATH% > cert-path
