#!/bin/sh
set -e -x

# check permissions on the credentials dir and its parent
[ $(stat -c %a "$DOCKER_CERT_PATH")    = 700 ]
[ $(stat -c %a "$DOCKER_CERT_PATH"/..) = 700 ]

# check permissions and content of the certificate files
[ $(stat -c %a "$DOCKER_CERT_PATH/key.pem")  = 600 ]
[ $(stat -c %a "$DOCKER_CERT_PATH/cert.pem") = 600 ]
[ $(stat -c %a "$DOCKER_CERT_PATH/ca.pem")   = 600 ]
[ $(stat -c %s "$DOCKER_CERT_PATH/key.pem")  = 9 ]
[ $(stat -c %s "$DOCKER_CERT_PATH/cert.pem") = 17 ]
[ $(stat -c %s "$DOCKER_CERT_PATH/ca.pem")   = 19 ]

# keep location of the certificate dir for the next step
echo "$DOCKER_CERT_PATH" > cert-path
