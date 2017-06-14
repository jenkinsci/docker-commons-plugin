REM get path of the certificate directory
if not exist cert-path exit /B 1
set /p cert_path=<cert-path

REM check it has been deleted
if exist %cert_path% exit /B 1
